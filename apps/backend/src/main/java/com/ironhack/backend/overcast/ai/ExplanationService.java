package com.ironhack.backend.overcast.ai;

import com.ironhack.backend.overcast.domain.Finding;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Turns findings into human prose. AI-optional by design: with a key the
 * text is phrased by Azure OpenAI; without one, deterministic templates.
 * Either way the numbers quoted are the rules engine's — this class never
 * computes or alters a saving.
 */
public class ExplanationService {

    static final String NO_AI_NOTICE =
            "AI assistant is offline (no Azure OpenAI key configured) — showing deterministic results.";

    private static final String EXPLAIN_SYSTEM = """
            You are Overcast, a cloud cost analyst. Explain in under 120 words why the \
            given billing finding was flagged and give a one-line fix. The dollar \
            figures were computed by a deterministic rules engine — quote them exactly, \
            never recalculate or invent numbers.""";

    private static final String ASK_SYSTEM = """
            You are Overcast, a cloud cost analyst. Answer the user's question using \
            ONLY the scan findings provided as context. All dollar figures come from a \
            deterministic rules engine — quote them exactly, never recalculate or \
            invent numbers. If the context cannot answer the question, say so.""";

    private final Function<Prompt, Optional<String>> ai;

    public record Prompt(String system, String user) {}

    public record Explanation(String explanation, String remediation, String source) {}

    public ExplanationService(AzureOpenAiClient client) {
        this(p -> client.complete(p.system(), p.user()));
    }

    /** Test seam: inject any completion function (or one that always declines). */
    ExplanationService(Function<Prompt, Optional<String>> ai) {
        this.ai = ai;
    }

    /** Explanation for one finding; caller handles the explanation_cache column. */
    public Explanation explain(Finding f) {
        Optional<String> aiText = ai.apply(new Prompt(EXPLAIN_SYSTEM, explainPrompt(f)));
        return aiText
                .map(text -> new Explanation(text, f.remediation(), "ai"))
                .orElseGet(() -> new Explanation(fallbackExplanation(f), f.remediation(), "fallback"));
    }

    /** Free-text Q&A grounded on this scan's findings only. */
    public Explanation ask(String question, String scanContextJson, List<Finding> topFindings) {
        String user = "Scan findings (JSON):\n" + scanContextJson + "\n\nQuestion: " + question;
        Optional<String> aiText = ai.apply(new Prompt(ASK_SYSTEM, user));
        return aiText
                .map(text -> new Explanation(text, "", "ai"))
                .orElseGet(() -> new Explanation(fallbackAnswer(topFindings), "", "fallback"));
    }

    private String explainPrompt(Finding f) {
        return "Finding: rule=%s, category=%s, resource=%s (%s), resource_group=%s, region=%s, monthly_cost=%s, monthly_saving=%s, suggested_fix=%s"
                .formatted(f.ruleId(), f.category().json(), f.resourceId(), f.resourceType(),
                        f.resourceGroup(), f.region(), f.monthlyCost(), f.monthlySaving(), f.remediation());
    }

    private String fallbackExplanation(Finding f) {
        String why = switch (f.ruleId()) {
            case "unattached_disk" -> "This managed disk is not attached to any VM, so every dollar it bills is pure waste.";
            case "orphaned_public_ip" -> "This public IP address is not associated with any NIC or load balancer but still bills hourly.";
            case "old_snapshot" -> "This snapshot is well past the retention window and no lifecycle policy manages it.";
            case "prev_gen_vm" -> "This VM runs a previous-generation SKU; the current generation offers the same specs for less.";
            case "nonprod_247" -> "This non-production VM ran essentially the whole month; nights and weekends are paid-for idle time.";
            case "ondemand_vs_reserved" -> "This VM ran the entire month at on-demand rates; sustained load is exactly what reservations discount.";
            case "premium_storage_nonprod" -> "This premium SSD serves a non-production workload that rarely needs its IOPS class.";
            case "untagged" -> "This resource has no owner/env tags, so nobody is accountable for its cost (flagged, not counted as waste).";
            default -> "This resource matched waste rule '" + f.ruleId() + "'.";
        };
        return why + " Estimated saving: " + f.monthlySaving() + "/mo (deterministic rules engine).";
    }

    private String fallbackAnswer(List<Finding> topFindings) {
        StringBuilder sb = new StringBuilder(NO_AI_NOTICE)
                .append(" Top cost-saving opportunities in this scan:");
        int i = 1;
        for (Finding f : topFindings.stream().limit(5).toList()) {
            sb.append("\n").append(i++).append(". ")
                    .append(shortName(f.resourceId()))
                    .append(" — ").append(f.remediation())
                    .append(" (saves ").append(f.monthlySaving()).append("/mo)");
        }
        return sb.toString();
    }

    private static String shortName(String resourceId) {
        int idx = resourceId.lastIndexOf('/');
        return idx >= 0 ? resourceId.substring(idx + 1) : resourceId;
    }
}
