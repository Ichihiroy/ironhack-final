import http from "k6/http";
import { check } from "k6";

// HPA demo: ramp GET /api/items past 300 req/s and hold, so backend CPU crosses
// the 70% HPA target and replicas visibly scale 3 → max on the Grafana
// "Platform" dashboard (HPA panel). See README.md.
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export const options = {
  scenarios: {
    items: {
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
  const res = http.get(`${BASE_URL}/api/items`);
  check(res, {
    "status is 200": (r) => r.status === 200,
    "returns items": (r) => r.json().length > 0,
  });
}
