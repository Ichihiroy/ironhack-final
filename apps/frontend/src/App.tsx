import { useEffect, useMemo, useRef, useState } from "react";
import type { FormEvent } from "react";
import { api } from "./api";
import { CATEGORY_LABEL, moneyCents } from "./format";
import type { Category, Finding, OptimizedBill, ScanSummary } from "./types";
import { WasteCounter } from "./components/WasteCounter";
import { FindingPanel } from "./components/FindingPanel";
import { OptimizedView } from "./components/OptimizedView";
import "./styles.css";

const CATEGORIES: Category[] = ["idle", "oversized", "forgotten"];
const DEMO_SCAN_ID = "demo";

type Source = { kind: "sample"; scanId: string } | { kind: "upload"; file: File };

export default function App() {
  const [source, setSource] = useState<Source>({ kind: "sample", scanId: DEMO_SCAN_ID });
  const [scanning, setScanning] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [summary, setSummary] = useState<ScanSummary | null>(null);
  const [findings, setFindings] = useState<Finding[]>([]);
  const [visibleCount, setVisibleCount] = useState(0); // progressive "streaming" reveal
  const [selected, setSelected] = useState<Finding | null>(null);

  const [optimized, setOptimized] = useState<OptimizedBill | null>(null);
  const [optimizing, setOptimizing] = useState(false);

  const [question, setQuestion] = useState("");
  const [answer, setAnswer] = useState<string | null>(null);
  const [asking, setAsking] = useState(false);

  const revealTimer = useRef<number | null>(null);

  // Stream findings in one-by-one for the reveal effect (whole batch ~1.2s).
  useEffect(() => {
    if (findings.length === 0) return;
    setVisibleCount(0);
    const step = Math.max(1, Math.ceil(findings.length / 24));
    revealTimer.current = window.setInterval(() => {
      setVisibleCount((n) => {
        const next = n + step;
        if (next >= findings.length && revealTimer.current !== null) {
          clearInterval(revealTimer.current);
        }
        return Math.min(next, findings.length);
      });
    }, 55);
    return () => {
      if (revealTimer.current !== null) clearInterval(revealTimer.current);
    };
  }, [findings]);

  async function runScan() {
    setScanning(true);
    setError(null);
    setOptimized(null);
    setAnswer(null);
    setSelected(null);
    try {
      let scanId: string;
      let sum: ScanSummary;
      if (source.kind === "upload") {
        const created = await api.uploadCsv(source.file);
        scanId = created.scanId;
        sum = created.summary;
      } else {
        scanId = source.scanId;
        sum = await api.summary(scanId);
      }
      const page = await api.findings(scanId, 0, 200);
      setSummary(sum);
      setFindings(page.items);
    } catch (e) {
      setError((e as Error).message);
      setSummary(null);
      setFindings([]);
    } finally {
      setScanning(false);
    }
  }

  async function generateOptimized() {
    if (!summary) return;
    setOptimizing(true);
    try {
      setOptimized(await api.optimized(summary.scanId));
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setOptimizing(false);
    }
  }

  async function submitQuestion(e: FormEvent) {
    e.preventDefault();
    if (!summary || !question.trim()) return;
    setAsking(true);
    setAnswer(null);
    try {
      const res = await api.ask(summary.scanId, question.trim());
      setAnswer(res.answer);
    } catch (err) {
      setAnswer(`Could not get an answer: ${(err as Error).message}`);
    } finally {
      setAsking(false);
    }
  }

  function exportChecklist() {
    if (!optimized) return;
    const lines = [
      `# Overcast remediation checklist`,
      ``,
      `Scan: ${optimized.scanId}`,
      `Current: ${moneyCents(optimized.currentMonthly, optimized.currency)}/mo`,
      `Optimized: ${moneyCents(optimized.optimizedMonthly, optimized.currency)}/mo`,
      `Savings: ${moneyCents(optimized.monthlySavings, optimized.currency)}/mo · ${moneyCents(
        optimized.annualSavings,
        optimized.currency,
      )}/yr`,
      ``,
      ...optimized.checklist.map(
        (c) =>
          `- [ ] ${c.ruleName}: ${c.action} (saves ${moneyCents(c.monthlySaving, optimized.currency)}/mo)`,
      ),
    ];
    const blob = new Blob([lines.join("\n")], { type: "text/markdown" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `overcast-checklist-${optimized.scanId}.md`;
    a.click();
    URL.revokeObjectURL(url);
  }

  const shown = useMemo(() => findings.slice(0, visibleCount), [findings, visibleCount]);

  return (
    <div className="app">
      <div className="masthead">
        <div className="logo">☁️</div>
        <h1>Overcast</h1>
      </div>
      <p className="tagline">Find the waste hiding in your Azure bill — in one scan.</p>

      <div className="controls">
        <div className="control-group">
          <label htmlFor="src">Bill to scan</label>
          <select
            id="src"
            value={source.kind === "sample" ? "sample" : "upload"}
            onChange={(e) =>
              setSource(
                e.target.value === "sample"
                  ? { kind: "sample", scanId: DEMO_SCAN_ID }
                  : { kind: "upload", file: new File([], "") },
              )
            }
          >
            <option value="sample">Sample: messy startup bill (seeded)</option>
            <option value="upload">Upload a CSV…</option>
          </select>
        </div>

        {source.kind === "upload" && (
          <div className="control-group">
            <label htmlFor="file">Azure usage CSV</label>
            <input
              id="file"
              className="filebtn"
              type="file"
              accept=".csv,text/csv"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) setSource({ kind: "upload", file });
              }}
            />
            {source.file.name && <div className="filename">{source.file.name}</div>}
          </div>
        )}

        <button
          className="scan-btn"
          onClick={runScan}
          disabled={scanning || (source.kind === "upload" && !source.file.name)}
        >
          {scanning ? "Scanning…" : "⚡ Scan bill"}
        </button>
      </div>

      {error && <div className="error-banner">⚠ {error}</div>}

      {summary && (
        <>
          <WasteCounter
            monthly={summary.totalMonthlyWaste}
            annual={summary.totalAnnualWaste}
            currency={summary.currency}
            findingCount={summary.findingCount}
            totalCost={summary.totalMonthlyCost}
          />

          <div className="cat-row">
            {CATEGORIES.map((c) => (
              <div key={c} className={`cat-tile category-${c}`}>
                <div className="k">
                  <span className="dot" /> {CATEGORY_LABEL[c]}
                </div>
                <div className="v">{moneyCents(summary.byCategory[c].monthlySaving, summary.currency)}</div>
                <div className="n">{summary.byCategory[c].count} resources /mo</div>
              </div>
            ))}
          </div>

          <div className="section-head">
            <h2>
              Findings{" "}
              <span style={{ color: "var(--text-dim)", fontWeight: 400 }}>
                ({shown.length}/{findings.length})
              </span>
            </h2>
            <button className="ghost-btn" onClick={generateOptimized} disabled={optimizing}>
              {optimizing ? "Generating…" : "Generate optimized bill"}
            </button>
          </div>

          {optimized && <OptimizedView bill={optimized} onExport={exportChecklist} />}

          <form className="ask" onSubmit={submitQuestion}>
            <input
              placeholder="Ask about this bill — e.g. where is my money going?"
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
            />
            <button className="ghost-btn" type="submit" disabled={asking || !question.trim()}>
              {asking ? "Asking…" : "Ask"}
            </button>
          </form>
          {answer && <div className="ask-answer">{answer}</div>}

          <div className="findings">
            {shown.map((f) => (
              <button
                key={f.id}
                className={`finding category-${f.category}`}
                onClick={() => setSelected(f)}
              >
                <div className="top">
                  <span className="name">{f.resourceName}</span>
                  <span className="save">
                    {f.monthlySaving > 0 ? (
                      `${moneyCents(f.monthlySaving, summary.currency)}/mo`
                    ) : (
                      <span className="flag-zero">flag</span>
                    )}
                  </span>
                </div>
                <div className="meta">
                  <span className="badge">{CATEGORY_LABEL[f.category]}</span>
                  <span>{f.resourceGroup}</span>
                </div>
              </button>
            ))}
          </div>
        </>
      )}

      {!summary && !scanning && (
        <div className="empty">
          Pick the seeded sample bill and hit <strong>Scan</strong> to see the waste.
        </div>
      )}

      {selected && summary && (
        <FindingPanel finding={selected} currency={summary.currency} onClose={() => setSelected(null)} />
      )}
    </div>
  );
}
