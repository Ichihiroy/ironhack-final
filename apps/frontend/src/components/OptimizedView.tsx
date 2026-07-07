import { moneyCents } from "../format";
import type { OptimizedBill } from "../types";

interface Props {
  bill: OptimizedBill;
  onExport: () => void;
}

/** Before/after totals + the remediation checklist (exportable). */
export function OptimizedView({ bill, onExport }: Props) {
  return (
    <section className="optimized">
      <div className="section-head">
        <h2>Optimized bill</h2>
        <button className="ghost-btn" onClick={onExport}>
          ⬇ Export checklist
        </button>
      </div>

      <div className="beforeafter">
        <div className="ba-card">
          <div className="lbl">Current</div>
          <div className="amt">{moneyCents(bill.currentMonthly, bill.currency)}</div>
        </div>
        <div className="arrow">→</div>
        <div className="ba-card after">
          <div className="lbl">Optimized</div>
          <div className="amt">{moneyCents(bill.optimizedMonthly, bill.currency)}</div>
        </div>
      </div>

      <p className="hero-sub" style={{ marginBottom: "1rem" }}>
        Save <strong>{moneyCents(bill.monthlySavings, bill.currency)}/mo</strong> ·{" "}
        <strong>{moneyCents(bill.annualSavings, bill.currency)}/yr</strong> across{" "}
        {bill.checklist.length} actions.
      </p>

      <ul className="checklist">
        {bill.checklist.map((item, i) => (
          <li key={`${item.resourceId}-${i}`}>
            <span className="tick">✓</span>
            <span>
              <strong>{item.ruleName}</strong> — {item.action}
            </span>
            <span className="save">{moneyCents(item.monthlySaving, bill.currency)}/mo</span>
          </li>
        ))}
      </ul>
    </section>
  );
}
