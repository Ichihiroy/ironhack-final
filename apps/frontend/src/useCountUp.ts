import { useEffect, useRef, useState } from "react";

/**
 * Animates a number from its previous value to `target` with an ease-out curve
 * using requestAnimationFrame — the hero counter. No animation library needed.
 * Respects prefers-reduced-motion by snapping straight to the target.
 */
export function useCountUp(target: number, durationMs = 1600): number {
  const [value, setValue] = useState(0);
  const fromRef = useRef(0);
  const rafRef = useRef<number | null>(null);

  useEffect(() => {
    const reduce = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
    const from = fromRef.current;
    if (reduce || durationMs <= 0) {
      fromRef.current = target;
      setValue(target);
      return;
    }

    let start: number | null = null;
    const step = (ts: number) => {
      if (start === null) start = ts;
      const t = Math.min((ts - start) / durationMs, 1);
      const eased = 1 - Math.pow(1 - t, 3); // easeOutCubic
      setValue(from + (target - from) * eased);
      if (t < 1) {
        rafRef.current = requestAnimationFrame(step);
      } else {
        fromRef.current = target;
      }
    };
    rafRef.current = requestAnimationFrame(step);
    return () => {
      if (rafRef.current !== null) cancelAnimationFrame(rafRef.current);
      fromRef.current = target; // next run animates from where we were headed
    };
  }, [target, durationMs]);

  return value;
}
