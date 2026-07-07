package com.ironhack.backend.overcast.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.ironhack.backend.overcast.domain.Category;
import com.ironhack.backend.overcast.domain.Finding;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * The AI is optional and text-only. These tests run with NO key (the CI
 * default) by injecting a completion function that always declines, and prove
 * the deterministic fallback carries the rules engine's numbers unchanged.
 */
class ExplanationServiceTest {

    private final ExplanationService offline = new ExplanationService(p -> Optional.empty());

    private Finding finding(String rule, Category cat, String saving) {
        return new Finding("f1", "demo",
                "/subscriptions/s/resourceGroups/rg-dev/providers/Microsoft.Compute/disks/disk-x",
                "Microsoft.Compute/disks", "rg-dev", "westeurope",
                rule, cat, new BigDecimal("40.00"), new BigDecimal(saving),
                "Snapshot then delete unattached disk 'disk-x' in rg-dev.", null);
    }

    @Test
    void fallbackExplanationQuotesTheEngineSavingVerbatim() {
        var explanation = offline.explain(finding("unattached_disk", Category.FORGOTTEN, "40.00"));

        assertThat(explanation.source()).isEqualTo("fallback");
        assertThat(explanation.explanation()).contains("40.00"); // the engine's number, unaltered
        assertThat(explanation.remediation()).contains("disk-x");
    }

    @Test
    void fallbackAnswerAnnouncesOfflineModeAndListsTopFindings() {
        var answer = offline.ask(
                "where is my money going?",
                "{\"summary\":{}}",
                List.of(finding("unattached_disk", Category.FORGOTTEN, "40.00")));

        assertThat(answer.source()).isEqualTo("fallback");
        assertThat(answer.explanation()).contains("AI assistant is offline");
        assertThat(answer.explanation()).contains("40.00");
    }

    @Test
    void whenAiRespondsItsTextIsUsedButNumbersStillComeFromTheFinding() {
        // Simulate a key being present: AI returns prose. It cannot change the
        // saving — that value only ever comes from the Finding it was handed.
        ExplanationService online = new ExplanationService(
                p -> Optional.of("This disk is unattached; delete it to save money."));

        var f = finding("unattached_disk", Category.FORGOTTEN, "40.00");
        var explanation = online.explain(f);

        assertThat(explanation.source()).isEqualTo("ai");
        assertThat(explanation.explanation()).isEqualTo("This disk is unattached; delete it to save money.");
        assertThat(f.monthlySaving()).isEqualByComparingTo("40.00"); // untouched
    }
}
