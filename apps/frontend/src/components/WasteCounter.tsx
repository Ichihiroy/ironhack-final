import { moneyCents } from "../format";
import { useCountUp } from "../useCountUp";
import { CheckIcon } from "../icons";

interface Props {
  monthly: number;
  annual: number;
  currency: string;
  findingCount: number;
  totalCost: number;
}

/** The hero: a large monospace counter that animates up to the wasted total. */
export function WasteCounter({ monthly, annual, currency, findingCount, totalCost }: Props) {
  const animatedMonthly = useCountUp(monthly);
  const animatedAnnual = useCountUp(annual);
  const pct = totalCost > 0 ? Math.round((monthly / totalCost) * 100) : 0;

  return (
    <section className="panel hero" aria-live="polite">
      <p className="eyebrow">
        <span className="pip" /> Wasted spend detected
      </p>
      <div className="counter">
        {moneyCents(animatedMonthly, currency)}
        <span className="per"> /mo</span>
        <span className="sep"> · </span>
        <span className="annual">{moneyCents(animatedAnnual, currency)}</span>
        <span className="per"> /yr</span>
      </div>
      <p className="hero-sub">
        <strong>{findingCount}</strong> wasteful resources — about <strong>{pct}%</strong> of the{" "}
        {moneyCents(totalCost, currency)}/mo bill.
      </p>
      <p className="hero-note">
        <CheckIcon size={13} /> Every figure computed by the deterministic rules engine
      </p>
    </section>
  );
}
