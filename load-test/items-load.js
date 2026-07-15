import http from "k6/http";
import { check } from "k6";

// Two scenarios, one run (see README.md):
//
//  1. `findings` — HPA demo: ramp GET /api/scans/demo/findings past 300 req/s
//     and hold, so backend CPU crosses the 70% HPA target and replicas visibly
//     scale 3 → max on the Grafana "Platform" dashboard. Served from the Redis
//     cache when warm — the hot read path.
//  2. `db_reads` — honest p95: GET /api/scans/demo/optimized is deliberately
//     uncached (scan lookup + findings query per request), so its latency
//     threshold measures the real database round-trip under load, not Redis.
//
// The "demo" scan is seeded at startup (DemoDataSeeder) — zero setup.
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
// Seeded at startup with zero setup; overridable if pointing at another scan.
const SCAN_ID = __ENV.SCAN_ID || "demo";

export const options = {
  scenarios: {
    findings: {
      executor: "ramping-arrival-rate",
      exec: "findings",
      startRate: 20,
      timeUnit: "1s",
      preAllocatedVUs: 100,
      maxVUs: 600,
      stages: [
        { target: 100, duration: "1m" }, // warm-up
        { target: 350, duration: "2m" }, // ramp past the 300 req/s goal
        { target: 350, duration: "5m" }, // sustain — HPA reacts in this window
        { target: 0, duration: "1m" }, // cool-down — watch scale-down later
      ],
    },
    db_reads: {
      executor: "ramping-arrival-rate",
      exec: "dbReads",
      startRate: 2,
      timeUnit: "1s",
      preAllocatedVUs: 20,
      maxVUs: 100,
      stages: [
        { target: 10, duration: "1m" }, // warm-up alongside the cache scenario
        { target: 30, duration: "2m" }, // ramp the DB path
        { target: 30, duration: "5m" }, // sustain — pool/DB saturation shows here
        { target: 0, duration: "1m" },
      ],
    },
  },
  thresholds: {
    // Per-scenario: the cached contract stays at 300ms; the DB-backed read
    // gets its own honest budget (scan + findings query per request).
    "http_req_duration{scenario:findings}": ["p(95)<=300"],
    "http_req_duration{scenario:db_reads}": ["p(95)<=500"],
    http_req_failed: ["rate<0.01"],
  },
};

export function findings() {
  const res = http.get(`${BASE_URL}/api/scans/${SCAN_ID}/findings?page=0&size=50`);
  check(res, {
    "findings status is 200": (r) => r.status === 200,
    "returns findings": (r) => r.json("totalItems") > 0,
  });
}

export function dbReads() {
  const res = http.get(`${BASE_URL}/api/scans/${SCAN_ID}/optimized`);
  check(res, {
    "optimized status is 200": (r) => r.status === 200,
    "returns optimized bill": (r) => r.json("scanId") === SCAN_ID,
  });
}
