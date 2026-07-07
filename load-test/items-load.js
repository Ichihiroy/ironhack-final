import http from "k6/http";
import { check } from "k6";

// HPA demo: ramp GET /api/scans/demo/findings past 300 req/s and hold, so
// backend CPU crosses the 70% HPA target and replicas visibly scale 3 → max on
// the Grafana "Platform" dashboard (HPA panel). The "demo" scan is seeded at
// startup (DemoDataSeeder) and its findings are served from the Redis cache, so
// this exercises the hot cached read path — no DB round-trip per request. See
// README.md.
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
// Seeded at startup with zero setup; overridable if pointing at another scan.
const SCAN_ID = __ENV.SCAN_ID || "demo";

export const options = {
  scenarios: {
    findings: {
      executor: "ramping-arrival-rate",
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
  },
  thresholds: {
    http_req_duration: ["p(95)<=300"], // contract: p95 within 300ms under load
    http_req_failed: ["rate<0.01"],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/api/scans/${SCAN_ID}/findings?page=0&size=50`);
  check(res, {
    "status is 200": (r) => r.status === 200,
    "returns findings": (r) => r.json("totalItems") > 0,
  });
}
