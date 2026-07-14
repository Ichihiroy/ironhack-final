# Overcast — 5-Minute Investor-Style Presentation Plan

Team **The Locals** · Ironhack DevOps capstone · target length **5:00 sharp**

> Replace `[Member 1/2/3]` with real names. Every claim in the script is backed
> by a file or a live demo in this repo — the "Backing" line under each slide
> says exactly which one, so any Q&A follow-up has a ready answer.

---

## 1. Theme & visual identity

**Deck theme: "Clear Skies"** — the product clears the fog off your cloud bill.

- **Background:** very dark navy (`#0B1220`), like a night sky / terminal.
- **Accent:** electric cyan (`#22D3EE`) for numbers and highlights; alarm
  amber (`#F59E0B`) only for "waste/money burning" figures.
- **Typography:** big bold sans (Inter/Montserrat) — one idea per slide, huge
  numbers, max 5 bullets, never full sentences on slides (sentences are for
  the speaker).
- **Recurring motif:** a cloud icon that starts "stormy" on the problem slide
  and turns "clear" by the closing slide.
- **Footer on every slide:** `overcast · thelocals` + slide number.

Tooling: Google Slides / PowerPoint with this palette, or reveal.js if you
want to present from the browser next to the live demo tabs.

---

## 2. Cast & roles

| Speaker | Role in the pitch | Persona | Total speaking time |
| ------- | ----------------- | ------- | ------------------- |
| **[Member 1]** — Product & Delivery Lead | Opens: hook, team, problem | The visionary: energy, big picture, money numbers | ~1:35 |
| **[Member 2]** — Cloud Platform Architect | Middle: product, architecture, how we ship | The builder: calm confidence, "we built this and here's how" | ~1:40 |
| **[Member 3]** — Security & Reliability Engineer | Closes: security, proof under load, resilience, the ask | The closer: measured, then rises to a strong finish | ~1:45 |

Handoffs are scripted (each speaker's last line names the next speaker) — this
is where amateur pitches lose 20 seconds of shuffling.

---

## 3. Run of show (timing at ~140 words/min, verified word counts)

| # | Slide | Speaker | Start | Length |
| - | ----- | ------- | ----- | ------ |
| 1 | Title & hook | Member 1 | 0:00 | 0:25 |
| 2 | Meet the team | Member 1 | 0:25 | 0:30 |
| 3 | The problem: cloud waste | Member 1 | 0:55 | 0:40 |
| 4 | The product: Overcast | Member 2 | 1:35 | 0:35 |
| 5 | The platform architecture | Member 2 | 2:10 | 0:40 |
| 6 | How we ship: GitOps | Member 2 | 2:50 | 0:25 |
| 7 | Security by construction | Member 3 | 3:15 | 0:35 |
| 8 | Proof under fire | Member 3 | 3:50 | 0:30 |
| 9 | The phoenix test & the ask | Member 3 | 4:20 | 0:40 |
|   | **Total** | | | **5:00** |

---

## 4. Slide-by-slide plan with full script

### Slide 1 — Title & hook
- **Speaker:** Member 1 · **0:00–0:25** (25 s, ~55 words)
- **Emotion/delivery:** High energy, direct eye contact, pause two beats after
  the first sentence. Do NOT introduce yourself yet — hook first.
- **Slide content:** Product wordmark **OVERCAST** + tagline *"See through
  your cloud bill."* One line below: `thelocals — Ironhack DevOps capstone`.
- **Image:** Stormy-cloud logo motif over dark navy; a faint, blurred
  cloud-bill screenshot in the background. No other text.

> **Script:** "Every company on the cloud is paying for things it doesn't
> use — every single month. We built the product that finds that wasted
> money, and the production platform that could run it for real customers
> tomorrow. We are The Locals, and this… is Overcast."

### Slide 2 — Meet the team
- **Speaker:** Member 1 · **0:25–0:55** (30 s, ~70 words)
- **Emotion/delivery:** Warm and proud; gesture to each member as they're
  named. Members nod/wave — rehearse this so it's snappy.
- **Slide content:** Three photo cards, name + one-line role under each:
  *Product & Delivery* · *Cloud Platform Architect* · *Security & Reliability*.
- **Image:** Three team photos (same background/crop for a professional look);
  small icon under each: 🚀 pipeline / ☁️ terraform / 🛡️ shield.

> **Script:** "Three people, three disciplines. I'm [Member 1] — product and
> delivery: I own the CI/CD pipelines and how an idea becomes a running
> release. [Member 2] is our cloud architect — every piece of Azure
> infrastructure you'll see is code they wrote. And [Member 3] owns security
> and reliability — identity, policies, monitoring. Nothing ships unless it
> passes their gates."

### Slide 3 — The problem: cloud waste
- **Speaker:** Member 1 · **0:55–1:35** (40 s, ~90 words)
- **Emotion/delivery:** Shift to serious. Slow down on the money number —
  let "a third" land. This is the investor-pain slide.
- **Slide content:** One giant amber figure: **"~30% of cloud spend is
  waste"** (cite: Flexera *State of the Cloud* — **verify the current year's
  figure before presenting**). Three small icons below: unattached disks ·
  idle public IPs · oversized VMs.
- **Image:** A real (anonymized) Azure cost-export CSV screenshot — wall of
  cryptic rows — with a red glow. Message: *nobody can read this.*

> **Script:** "Here's the dirty secret of the cloud: industry surveys put
> waste at roughly a third of every cloud bill. Disks that outlived their
> VMs. Public IPs attached to nothing. Machines sized for a launch day that
> never came. Finance sees a number; engineers see ten thousand resources;
> nobody connects the two. That gap is money burning every month — and that
> gap is our market. [Member 2] will show you what we built."

### Slide 4 — The product: Overcast
- **Speaker:** Member 2 · **1:35–2:10** (35 s, ~80 words)
- **Emotion/delivery:** Calm confidence, demo-owner tone. If a live browser
  tab is allowed, flick to the real app for 5 seconds; otherwise the
  screenshot carries it.
- **Slide content:** Product screenshot center-stage. Three-step strip:
  **Upload CSV → Findings + € saved/month → AI summary.** Badge: *Azure &
  AWS exports supported.* URL bar visible with the padlock:
  `https://thelocals-ironhack-prod.westeurope.cloudapp.azure.com`.
- **Image:** Real Overcast UI screenshot showing a scan result: findings
  table with estimated monthly savings + the AI-written summary panel.
- **Backing:** `apps/backend/.../overcast/` (csv parsers, rules, ai),
  README "What is this?".

> **Script:** "Overcast is deliberately simple to use. Drop in your Azure or
> AWS cost export — the CSV your provider already gives you. Our rules
> engine flags the waste: unattached disks, idle IPs, oversized VMs — each
> with estimated monthly savings. Then an AI assistant writes the summary
> your CFO actually reads. And this isn't a mockup — it's live, right now,
> at that address, behind HTTPS. What's underneath is the part investors
> should care about."

### Slide 5 — The platform architecture
- **Speaker:** Member 2 · **2:10–2:50** (40 s, ~92 words)
- **Emotion/delivery:** This is the flex slide — steady, unhurried, point at
  the diagram as you trace the flow left to right.
- **Slide content:** The architecture diagram, three callout chips:
  *"1 Terraform apply = entire cloud"* · *"DB has no public endpoint"* ·
  *"Secrets never touch git."*
- **Image:** Render of the README mermaid flowchart (dev → CI → ACR/git →
  Argo CD → AKS; Key Vault CSI; SQL private endpoint; users → ingress).
  Re-style it in deck colors — don't paste a low-res screenshot.
- **Backing:** `infra/terraform/` (AKS, ACR, Key Vault, SQL private
  endpoint, VNet, monitoring), `docs/architecture.md`, ADRs 0001–0009.

> **Script:** "One Terraform apply builds everything you see: the Kubernetes
> cluster with autoscaling node pools, the container registry, Key Vault,
> and an Azure SQL database that has no public endpoint at all — it's
> reachable only through a private link inside our network. The database
> password? Generated by Terraform, stored in Key Vault, mounted into the
> pod. It has never existed in a file, a repo, or a chat. Staging and
> production run identical code — only one values file differs. Every
> non-obvious choice has a written justification: nine architecture decision
> records."

### Slide 6 — How we ship: GitOps
- **Speaker:** Member 2 · **2:50–3:15** (25 s, ~58 words)
- **Emotion/delivery:** Punchy, quicker tempo. The "rollback is git revert"
  line deserves a half-smile pause.
- **Slide content:** Horizontal pipeline graphic: **PR → test → scan → sign
  → git commit → Argo CD pulls → cluster.** Big line under it: *"CI holds
  zero cluster credentials. Rollback = `git revert`."*
- **Image:** Split image: GitHub Actions green pipeline run (left) + Argo CD
  UI showing both apps Healthy/Synced (right).
- **Backing:** `.github/workflows/backend-ci-cd.yml`, `gitops/values/`,
  ADR-0008, `infra/terraform/gitops.tf`.

> **Script:** "And nobody — human or CI — ever deploys by hand. The pipeline
> tests, scans, signs, and then just commits the new image tag to git. Argo
> CD inside the cluster pulls it and reconciles. If someone edits the
> cluster manually, it heals itself back to git. Rollback is a one-line git
> revert. [Member 3] — how do we keep it safe?"

### Slide 7 — Security by construction
- **Speaker:** Member 3 · **3:15–3:50** (35 s, ~80 words)
- **Emotion/delivery:** Measured, deliberate, almost quiet — security speaks
  in facts. Count the layers on your fingers.
- **Slide content:** Five stacked layers (defense-in-depth graphic):
  **Identity: OIDC only, zero stored secrets** · **Network: default-deny** ·
  **Supply chain: Trivy gate + cosign signatures** · **Admission: Kyverno
  policies** · **Repo: gitleaks + pinned actions.**
- **Image:** Layered shield diagram; small inset screenshot of a Trivy scan
  table or a Kyverno policy report.
- **Backing:** `security/SECURITY.md` (index of every control),
  `security/networkpolicies.yaml`, `gitops/policies/`, ADR-0004, ADR-0009.

> **Script:** "This repository contains zero credentials. Not encrypted —
> zero. CI proves its identity to Azure with OIDC tokens that expire in
> minutes. Inside the cluster, the network is default-deny: the backend is
> reachable only from the frontend, the ingress, and monitoring — nothing
> else. Every image is vulnerability-gated before it's pushed,
> cryptographically signed, and Kubernetes itself refuses unsigned images,
> root containers, or anything not from our registry. Security isn't a
> checklist here — it's the construction."

### Slide 8 — Proof under fire
- **Speaker:** Member 3 · **3:50–4:20** (30 s, ~70 words)
- **Emotion/delivery:** Rising energy. These are the numbers slide-readers
  photograph — say them slowly and exactly.
- **Slide content:** Three giant cyan stats: **350 req/s sustained** ·
  **p95 < 300 ms** · **replicas 3 → max, automatically.**
- **Image:** Grafana **Platform** dashboard screenshot with the HPA
  replica-count panel visibly stepping up during the k6 run; small inset of
  the k6 terminal summary (checks passed).
- **Backing:** `load-test/items-load.js` (thresholds coded in),
  `observability/` dashboards — reproducible live in ~9 min (GUIDE.md §7).

> **Script:** "We don't ask you to trust it — we measured it. Under a
> sustained load test of three hundred and fifty requests per second, the
> 95th percentile response time stays under three hundred milliseconds,
> while the platform scales itself from three replicas to maximum and back
> down — no human involved. Those dashboards and alerts you see are the
> same ones on call would use. And one more thing… our platform dies every
> night."

### Slide 9 — The phoenix test & the ask
- **Speaker:** Member 3 · **4:20–5:00** (40 s, ~92 words)
- **Emotion/delivery:** Start with the surprise ("dies every night" cliff),
  build to the strongest, slowest, most confident close of the pitch. Final
  line: stop, smile, hold.
- **Slide content:** Left: 🔥 *"Every night, our entire Azure subscription is
  deleted."* Right: 🌅 *"Every morning: 1 script + 1 pipeline ≈ rebuilt from
  git."* Closing banner: **"Overcast — the product finds wasted money; the
  platform is ready to scale. Join us."** + repo QR code.
- **Image:** Split "burn/rebuild" visual; the green morning `infra-apply`
  pipeline run as the rebuild proof.
- **Backing:** GUIDE.md "Morning kickstart", `infra/bootstrap/bootstrap.sh`,
  `infra/terraform/platform.tf` — everything declarative, nothing
  hand-installed.

> **Script:** "That's not a metaphor. Our training subscription is wiped
> every midnight — clusters, databases, everything. And every morning we
> rebuild the entire company from one script and one pipeline run, because
> every single piece lives in git. Most companies write a disaster-recovery
> plan and pray. We *execute* ours daily. So here's Overcast: a product
> that finds real money, on a platform that's secure by construction,
> proven under load, and literally reborn every day. We're The Locals —
> and we're ready to scale. Thank you."

---

## 5. Image asset checklist (capture before deck-building)

| # | Asset | How to get it |
| - | ----- | ------------- |
| 1 | Overcast UI with scan results + AI summary | Browser at `https://thelocals-ironhack-prod.westeurope.cloudapp.azure.com`, upload a sample CSV, screenshot with the padlock visible |
| 2 | Architecture diagram | Re-draw the README mermaid flowchart in deck colors (mermaid.live export → recolor, or draw in Figma/Slides) |
| 3 | GitHub Actions green run | Actions → latest `backend-ci-cd` run → screenshot the job graph |
| 4 | Argo CD apps Healthy/Synced | `kubectl -n argocd port-forward svc/argocd-server 8082:80` → screenshot app tiles |
| 5 | Grafana HPA panel during load | Run `k6 run -e BASE_URL=https://<host> load-test/items-load.js`, screenshot the Platform dashboard mid-climb + k6 terminal summary |
| 6 | Trivy/Kyverno inset | Screenshot the Trivy step output in a CI run, or `kubectl get policyreports -n app-staging` |
| 7 | Anonymized cost-export CSV | Open a sample export, blur any identifiers |
| 8 | Team photos ×3 | Same background, same crop |
| 9 | Repo QR code | Any QR generator → repo URL |

Capture 3–5 during one morning-kickstart session — they all exist at once
right after a rebuild + load test.

---

## 6. Rehearsal & contingency notes

- **Word counts are pre-fitted** to ~140 wpm; if a rehearsal runs over 5:10,
  cut slide 2 to 20 s (name + role only, drop the last sentence) — never cut
  slides 8–9, they're the close.
- **Time it three times** with a visible phone timer; mark where you should
  be at 2:00 (mid-slide-5) and 3:30 (mid-slide-7) as checkpoints.
- **Live demo discipline:** the app tab on slide 4 is optional garnish, max
  5 s. If wifi/cluster is questionable, screenshots only — the numbers on
  slides 8–9 carry the credibility, not a live click.
- **Q&A ammunition** (each member owns their area): defense-day talking
  points already exist in GUIDE.md — NGINX-over-AGIC trade-off, why unit
  tests only, RBAC-mode Key Vault, why the RG lives in bootstrap, and
  pull-based GitOps rollback demo (`kubectl scale` → Argo heals it).
- **The one external stat** (≈30% cloud waste) is the only claim not backed
  by this repo — check the latest Flexera/industry figure and put the source
  in tiny text on slide 3.
