package com.ironhack.backend.overcast.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.ironhack.backend.overcast.csv.AzureUsageCsvParser;
import com.ironhack.backend.overcast.domain.Category;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * THE deterministic-money guarantee, locked as a regression test. If any
 * constant, predicate, or the hero sample changes, this ledger must be
 * re-derived on purpose — the savings number can never drift silently.
 *
 * No Spring, no DB, no AI, no clock: same CSV in, same dollars out.
 */
class RulesEngineTest {

    private final RulesConfig config = RulesConfig.load();
    private final RulesEngine engine = new RulesEngine(config);
    private final AzureUsageCsvParser parser = new AzureUsageCsvParser();

    private RulesEngine.Result evaluateSample(String resource) {
        try (var reader = new InputStreamReader(
                getClass().getResourceAsStream(resource), StandardCharsets.UTF_8)) {
            return engine.evaluate(parser.parse(reader).resources());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void heroBillTotalsExactlyTheSeededWaste() {
        var result = evaluateSample("/samples/azure-hero-messy.csv");

        // Hand-verified ledger (see docs/csv-schema.md); every cent accounted for.
        assertThat(result.totalMonthlyWaste()).isEqualByComparingTo("2300.42");
    }

    @Test
    void heroBillPerRuleBreakdownIsStable() {
        var result = evaluateSample("/samples/azure-hero-messy.csv");

        Map<String, BigDecimal> byRule = new LinkedHashMap<>();
        for (RuleMatch m : result.matches()) {
            byRule.merge(m.ruleId(), m.monthlySaving(), BigDecimal::add);
        }

        assertThat(byRule.get("unattached_disk")).isEqualByComparingTo("195.25");
        assertThat(byRule.get("orphaned_public_ip")).isEqualByComparingTo("10.95");
        assertThat(byRule.get("old_snapshot")).isEqualByComparingTo("231.63");
        assertThat(byRule.get("nonprod_247")).isEqualByComparingTo("1024.92");
        assertThat(byRule.get("ondemand_vs_reserved")).isEqualByComparingTo("705.46");
        assertThat(byRule.get("premium_storage_nonprod")).isEqualByComparingTo("132.21");
        assertThat(byRule.get("untagged")).isEqualByComparingTo("0");
    }

    @Test
    void cleanBillHasZeroWasteAndNoFindings() {
        var result = evaluateSample("/samples/azure-small-clean.csv");

        assertThat(result.matches()).isEmpty();
        assertThat(result.totalMonthlyWaste()).isEqualByComparingTo("0");
    }

    @Test
    void everySavingIsCappedAtTheResourceCostAndNeverDoubleCounts() {
        var result = evaluateSample("/samples/azure-hero-messy.csv");

        // Invariant that makes the number trustworthy: for any resource, the
        // sum of its findings' savings never exceeds its monthly cost.
        Map<String, BigDecimal> savingByResource = new LinkedHashMap<>();
        Map<String, BigDecimal> costByResource = new LinkedHashMap<>();
        for (RuleMatch m : result.matches()) {
            savingByResource.merge(m.resource().resourceId(), m.monthlySaving(), BigDecimal::add);
            costByResource.putIfAbsent(m.resource().resourceId(), m.resource().monthlyCost());
        }
        savingByResource.forEach((id, saving) ->
                assertThat(saving).as("saving <= cost for %s", id)
                        .isLessThanOrEqualTo(costByResource.get(id)));
    }

    @Test
    void untaggedFlagsCarryZeroDollarsSoGovernanceNeverInflatesWaste() {
        var result = evaluateSample("/samples/azure-hero-messy.csv");

        assertThat(result.matches())
                .filteredOn(m -> m.ruleId().equals("untagged"))
                .isNotEmpty()
                .allSatisfy(m -> {
                    assertThat(m.category()).isEqualTo(Category.FORGOTTEN);
                    assertThat(m.monthlySaving()).isEqualByComparingTo("0");
                });
    }

    @Test
    void engineRefusesToStartWhenYamlAndCodeDiverge() {
        // The 1:1 check between rules-config.yaml and the implementations is what
        // lets the YAML be audited as the authoritative rule list.
        assertThat(config.rules().keySet()).containsExactlyInAnyOrder(
                "unattached_disk", "orphaned_public_ip", "old_snapshot", "prev_gen_vm",
                "nonprod_247", "ondemand_vs_reserved", "premium_storage_nonprod", "untagged");
    }
}
