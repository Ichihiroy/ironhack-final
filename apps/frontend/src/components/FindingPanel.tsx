import { useEffect, useState } from "react";
import { api } from "../api";
import { CATEGORY_LABEL, moneyCents } from "../format";
import type { ExplainResponse, Finding } from "../types";

interface Props {
  finding: Finding;
  currency: string;
  onClose: () => void;
}

/**
 * Slide-in detail panel. Fetches the explanation on open — AI-phrased when a
 * key is configured, deterministic template otherwise (shown by the source
 * tag). The saving shown always comes from the finding, never the AI.
 */
export function FindingPanel({ finding, currency, onClose }: Props) {
  const [explain, setExplain] = useState<ExplainResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    api
      .explain(finding.id)
      .then((res) => active && setExplain(res))
      .catch((e: Error) => active && setError(e.message))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [finding.id]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  return (
    <>
      <div className="scrim" onClick={onClose} />
      <aside className={`panel category-${finding.category}`} role="dialog" aria-modal="true">
        <button className="close" onClick={onClose} aria-label="Close">
          ✕
        </button>
        <span className="badge">{CATEGORY_LABEL[finding.category]}</span>
        <h3>{finding.resourceName}</h3>
        <p className="sub">
          {finding.resourceType} · {finding.resourceGroup || "no resource group"} · {finding.region}
        </p>

        <div className="save-big">
          {finding.monthlySaving > 0
            ? `${moneyCents(finding.monthlySaving, currency)}/mo`
            : "Governance flag"}
        </div>

        <div className="block">
          <h4>Why it's flagged</h4>
          {loading && (
            <p className="explain-text">
              <span className="spinner" /> Generating explanation…
            </p>
          )}
          {error && <p className="explain-text">Could not load explanation: {error}</p>}
          {explain && <p className="explain-text">{explain.explanation}</p>}
          {explain && (
            <span className={`source-tag ${explain.source === "ai" ? "ai" : ""}`}>
              {explain.source === "ai"
                ? "AI-generated"
                : explain.source === "cache"
                  ? "cached"
                  : "deterministic template (no AI key)"}
            </span>
          )}
        </div>

        <div className="block">
          <h4>Recommended fix</h4>
          <p className="explain-text">{explain?.remediation ?? finding.remediation}</p>
        </div>
      </aside>
    </>
  );
}
