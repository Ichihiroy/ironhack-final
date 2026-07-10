# Overcast — 5-Minute Presentation Script

**Audience:** mixed — engineers *and* non-technical judges. Everyone must follow.
**Rule of delivery:** the spine is plain English. Technical depth rides along in the
`[for the engineers]` asides — say those faster, almost as throwaways. They signal
credibility without losing the room.

**Total: 5:00.** ~110–130 words per minute. Practice to the clock twice.

---

### The 10-second version (memorize this — it's your anchor)
> "Overcast finds the money you're wasting in the cloud. But the real project is the
> **factory** behind it — one that builds, secures, and ships software to the cloud
> with no servers touched by hand and no passwords stored anywhere."

Three words to land: **automated · secure · self-running.**

---

## 0:00 – 0:45 · The hook (problem everyone feels)

> "Quick show of hands — who's ever gotten a bill that was way bigger than you
> expected?
>
> That's what happens to companies in the cloud, every single month. They rent
> computers from Amazon, Microsoft, Google — and they leave things running that
> nobody uses. An idle server here. An oversized database there. Something a
> developer spun up in 2022 and forgot.
>
> On average, **about a third of all cloud spending is pure waste.** For a mid-size
> company that's millions a year — money lit on fire, quietly."

*(On screen: the Overcast landing page — clean, dark, one big number.)*

## 0:45 – 1:30 · What Overcast does (show, don't tell)

> "So we built **Overcast**. You give it your cloud bill — one file — and it hands you
> back a list: here's what's wasted, here's how to fix it, and here's the exact dollar
> amount you'll save."

*(Click **Scan a bill**. The counter lands on **$2,300 / month**.)*

> "This messy example bill? **$2,300 a month** in waste — found in seconds. And notice:
> every number comes from fixed, auditable rules — not a guessing AI. The AI only writes
> the plain-English explanation. So the savings figure is one you can actually take to
> your finance team and defend."

> `[for the engineers]` *A deterministic rules engine owns every cent; the LLM is
> text-only with a fallback path — pull the API key and the numbers are byte-for-byte
> identical.*

## 1:30 – 2:00 · The twist (raise the stakes)

> "Now — here's the part that matters. The app you just saw? **That's the easy part.**
>
> The hard part — the actual engineering project — is everything you *don't* see: how
> this thing gets built, tested, locked down, and shipped to a real cloud without a
> single human logging into a server. That's what I want to show you next."

## 2:00 – 3:30 · The platform, in four punchy pillars

> "Think of it like a modern car factory. Four things make it special.

> **One — the whole factory is a blueprint.**
> Our entire cloud — the servers, the network, the database — is written down as code.
> One command builds it, exactly the same, every time. No clicking around, no 'it works
> on my machine.' If it burned down tomorrow, we'd rebuild it from the text file.
> `[for the engineers]` *Terraform, remote-state-locked, workload identity, private
> endpoints — one apply.*

> **Two — an automated assembly line.**
> Every change to the code goes down a conveyor belt: it's tested, scanned for security
> holes, and **shrink-wrapped with a tamper-proof seal** before it's allowed anywhere
> near production. And the cloud *pulls* the approved version in by itself — nobody
> pushes a deploy by hand.
> `[for the engineers]` *Two path-filtered pipelines, Trivy gate, cosign keyless signing,
> pull-based GitOps with Argo CD — CI holds zero cluster credentials.*

> **Three — no passwords. Anywhere.**
> This is my favorite. There are **no stored passwords or secret keys** in this entire
> project. Every machine proves who it is with a short-lived digital ID badge that
> expires in minutes. If a hacker stole our *entire* codebase, they'd get nothing that
> unlocks anything.
> `[for the engineers]` *OIDC federation end-to-end, Key Vault via workload identity,
> admin accounts disabled, default-deny networking, signed-image admission control.*

> **Four — it watches and heals itself.**
> The system monitors its own health, and if it gets busy, it grows more capacity
> automatically — then shrinks back down when the rush is over. Let me show you that
> live."

## 3:30 – 4:30 · Live demo (the 'wow')

*(Split screen: a traffic-generator terminal on the left, the Grafana dashboard on the right.)*

> "I'm about to throw a flood of traffic at it — hundreds of requests a second, like a
> sudden rush of customers."

*(Start the k6 load test.)*

> "Watch the right side. Right now it's running three copies of the app. As the pressure
> climbs… **there — it's adding more copies, by itself.** No one told it to. It felt the
> load and grew. And the response time" *(point)* "stays fast the whole way through."

*(Let it scale up visibly.)*

> "And when I stop the traffic… it scales back down and gives the capacity back — because
> you shouldn't pay for servers you're not using. Which, if you think about it, is the
> exact same problem Overcast was built to solve."

> `[for the engineers]` *HPA on CPU target plus cluster autoscaler on a dedicated user
> node pool; p95 held under 300 ms at 350 req/s.*

> *(No-cluster fallback: run the same load test against `docker compose` locally and show
> the live metrics at `/actuator/prometheus` + the dashboard JSON — same story, smaller stage.)*

## 4:30 – 5:00 · The close

> "So — two things to take away.
>
> Overcast finds the money you're wasting in the cloud. That's the product.
>
> But the *real* deliverable is the factory behind it: **automated** end to end,
> **secure** with no passwords to steal, and **self-running** — it scales and heals on
> its own. And because the app is cleanly separated from the platform, you could swap
> Overcast out for *any* product tomorrow and it would ride the exact same rails.
>
> That's a production-grade cloud platform — built from scratch. Thank you."

*(Hold on the architecture diagram or the $2,300 number. Take questions.)*

---

## Delivery cheat-sheet

| Beat | Time | One job |
| ---- | ---- | ------- |
| Hook | 0:45 | Make them *feel* the wasted-money problem |
| App demo | 0:45 | Show the $2,300, land "auditable, not AI-guessed" |
| Twist | 0:30 | "The app is the easy part" |
| 4 pillars | 1:30 | Blueprint · Assembly line · No passwords · Self-healing |
| Live demo | 1:00 | It grows by itself → shrinks back |
| Close | 0:30 | Automated · Secure · Self-running · Swappable |

**If you're running long,** cut the fourth pillar's words and let the live demo make the
point. **If a demo breaks,** say the line and move on — never debug on stage; the
architecture diagram is your safety net.

## Anticipated questions (crisp answers)
- **"Is the AI making up the savings?"** → No. Fixed rules compute every number; the AI
  only writes the explanation. Remove the AI entirely and the figures don't change.
- **"How is it secure without passwords?"** → Short-lived identity tokens instead of
  stored secrets — nothing to leak, nothing to rotate, nothing to steal from the code.
- **"What if a bad version ships?"** → One-line undo: we revert the change in Git and the
  cluster rolls itself back to the previous, already-scanned version automatically.
- **"Is it actually running in Azure?"** → The whole platform is defined as code and
  validated end to end; it stands up with one command. *(Adjust to your live status.)*
