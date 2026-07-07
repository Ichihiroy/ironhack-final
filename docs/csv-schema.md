# CSV schema — Azure Cost Management usage details

Overcast ingests the **Azure Cost Management "usage details" CSV export** and
normalizes it into one internal model that the rules engine consumes. This doc
is the contract between the export and
[`AzureUsageCsvParser`](../apps/backend/src/main/java/com/ironhack/backend/overcast/csv/AzureUsageCsvParser.java);
keep the two in sync.

## How to get the file

Azure Portal → **Cost Management + Billing** → **Cost analysis** →
*Download* / *Exports* → **usage details** (CSV). Column names vary slightly by
API version, so the parser accepts several header aliases per field.

## Columns

Headers are matched **case-insensitively**; the first alias found wins.

| Normalized field | Accepted headers (aliases) | Required | Notes |
| ---------------- | -------------------------- | :------: | ----- |
| `resourceId` | `ResourceId`, `InstanceId` | ✅ | Full ARM id; its last segment is the display name. Rows are grouped by this. |
| `resourceType` | `ResourceType`, `ConsumedService` | ✅ | e.g. `Microsoft.Compute/virtualMachines`. Drives resource classification. |
| `resourceGroup` | `ResourceGroup`, `ResourceGroupName` | ✅ | Used by the non-prod pattern (`dev\|test\|sandbox\|qa`). |
| `cost` | `Cost`, `CostInBillingCurrency`, `PreTaxCost` | ✅ | Per-row cost; **summed** across a resource's rows. |
| `region` | `ResourceLocation`, `Location` | | |
| `meter` | `MeterName` | | Descriptive only. |
| `sku` | `SKU`, `MeterSubCategory`, `ServiceTier` | | Matched against the prev-gen SKU list and premium-storage check. |
| `quantity` | `Quantity`, `UsageQuantity` | | **Summed**; for VMs this is usage hours, compared to `sustained_hours` (700). |
| `unitPrice` | `UnitPrice`, `EffectivePrice` | | Carried for reference. |
| `currency` | `Currency`, `BillingCurrency`, `BillingCurrencyCode` | | First non-blank value becomes the scan currency (default `USD`). |
| `tags` | `Tags` | | JSON object (braces optional). Matched against `required_tags` (`owner`, `env`). |
| `associatedResource` | `AssociatedResource` | | **Enrichment** — see below. |
| `ageDays` | `AgeDays` | | **Enrichment** — integer age for snapshot rule. |

Missing any of the four required columns → HTTP 400 with a message naming the
column.

## Multi-row aggregation

A usage-details export has **one row per meter per resource per day/period**, so
a single VM appears many times. The parser groups all rows by `resourceId` and:

- **sums** `cost` and `quantity` across the group (total monthly cost / usage);
- takes descriptive fields (`resourceType`, `sku`, `region`, `tags`,
  `associatedResource`, `ageDays`) from the **primary meter** — the row with the
  highest single cost — which is the representative line for that resource.

This means `monthly_cost` in a finding is the resource's full monthly spend, not
one meter line.

## Enrichment columns (`AssociatedResource`, `AgeDays`)

Raw Azure exports do not include attachment state or resource age. Overcast's
sample generator and the documented ETL add two optional columns so the
"forgotten resource" rules can fire deterministically:

- **`AssociatedResource`** — the NIC/VM/LB a disk or public IP is attached to.
  The distinction is deliberate and load-bearing:
  - **column present, value blank** → the resource is *known to be unattached*
    → `unattached_disk` / `orphaned_public_ip` fire.
  - **column present, value set** → attached (counts as "premium disk attached"
    for `premium_storage_nonprod`).
  - **column absent entirely** → attachment *unknown*; the association-based
    rules stay silent rather than guess. (In the parser this is the
    `null` vs empty-string distinction on `associatedResource`.)
- **`AgeDays`** — integer age of a snapshot; `old_snapshot` fires when it
  exceeds `snapshot_age_days` (90). Absent/blank → rule stays silent.

Without these columns the cost/utilization rules (prev-gen, non-prod 24/7,
reserved-instance, premium storage) still work from cost, SKU, resource group,
and usage hours alone.

## Sample files

- [`azure-hero-messy.csv`](../apps/backend/src/main/resources/samples/azure-hero-messy.csv)
  — the seeded `demo` scan; flagged waste totals **$2,300.42/mo**.
- [`azure-small-clean.csv`](../apps/backend/src/main/resources/samples/azure-small-clean.csv)
  — a tidy bill that produces **zero** findings.

## AWS CUR — documented adapter stub (not implemented)

AWS **Cost and Usage Report** (CUR) support would slot in as a second parser
producing the same `NormalizedResource` model — the rules engine and everything
downstream are cloud-agnostic. The mapping would be roughly:

| Normalized field | AWS CUR column |
| ---------------- | -------------- |
| `resourceId` | `lineItem/ResourceId` |
| `resourceType` | `product/ProductName` / `lineItem/UsageType` |
| `resourceGroup` | *(no native equivalent — derive from a cost-allocation tag)* |
| `region` | `product/region` |
| `sku` | `product/instanceType` |
| `quantity` | `lineItem/UsageAmount` |
| `unitPrice` | `lineItem/UnblendedRate` |
| `cost` | `lineItem/UnblendedCost` |
| `currency` | `lineItem/CurrencyCode` |
| `tags` | `resourceTags/user:*` |

Only the parser and the SKU/prev-gen lists would change; `source_cloud` on the
scan already records provenance (`azure` today). **This is a design note only —
no AWS adapter ships in this repo.**
