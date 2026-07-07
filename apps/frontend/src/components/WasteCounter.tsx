import { moneyCents } from "../format";
import { useCountUp } from "../useCountUp";

interface Props {
  monthly: number;
  annual: number;
  currency: string;
  findingCount: number;
  totalCost: number;
}

/** The hero: a large counter that animates up to the wasted total. */
export function WasteCounter({ monthly, annual, currency, findingCount, totalCost }: Props) {
  const animatedMonthly = useCountUp(monthly);
  const animatedAnnual = useCountUp(annual);
  const pct = totalCost > 0 ? Math.round((monthly / totalCost) * 100) : 0;

  return (
    <section className="hero" aria-live="polite">
      <p className="eyebrow">Wasted spend detected</p>
      <div className="counter">
        {moneyCents(animatedMonthly, currency)}
        <span className="per"> /mo</span>
        <span className="sep"> · </span>
        {moneyCents(animatedAnnual, currency)}
        <span className="per"> /yr</span>
      </div>
      <p className="hero-sub">
        <strong>{findingCount}</strong> wasteful resources — about{" "}
        <strong>{pct}%</strong> of the {moneyCents(totalCost, currency)}/mo bill.
        Every figure computed by the deterministic rules engine.
      </p>
    </section>
  );
}
