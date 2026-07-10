import { moneyCents } from "../format";
import type { OptimizedBill } from "../types";
import { ArrowRightIcon, CheckIcon, DownloadIcon, TerminalIcon } from "../icons";

interface Props {
  bill: OptimizedBill;
  onExport: () => void;
}

/** Before/after totals + the remediation checklist (exportable). */
export function OptimizedView({ bill, onExport }: Props) {
  return (
    <section className="panel">
      <div className="panel-head">
        <span className="ph-icon">
          <TerminalIcon size={15} />
        </span>
        <h2>Optimized bill</h2>
        <div className="panel-tools">
          <button className="ghost-btn" onClick={onExport}>
            <DownloadIcon size={14} /> Export checklist
          </button>
        </div>
      </div>

      <div className="panel-body">
        <div className="beforeafter">
          <div className="ba-card">
            <div className="lbl">Current</div>
            <div className="amt">{moneyCents(bill.currentMonthly, bill.currency)}</div>
          </div>
          <div className="ba-arrow">
            <ArrowRightIcon size={22} />
          </div>
          <div className="ba-card after">
            <div className="lbl">Optimized</div>
            <div className="amt">{moneyCents(bill.optimizedMonthly, bill.currency)}</div>
          </div>
        </div>

        <p className="savings-line">
          Save <strong>{moneyCents(bill.monthlySavings, bill.currency)}/mo</strong> ·{" "}
          <strong>{moneyCents(bill.annualSavings, bill.currency)}/yr</strong> across{" "}
          {bill.checklist.length} actions.
        </p>

        <ul className="checklist">
          {bill.checklist.map((item, i) => (
            <li key={`${item.resourceId}-${i}`}>
              <span className="tick">
                <CheckIcon size={15} />
              </span>
              <span className="ck-text">
                <strong>{item.ruleName}</strong> — {item.action}
              </span>
              <span className="save">{moneyCents(item.monthlySaving, bill.currency)}/mo</span>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
