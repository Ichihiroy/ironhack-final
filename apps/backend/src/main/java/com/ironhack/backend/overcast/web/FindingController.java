package com.ironhack.backend.overcast.web;

import com.ironhack.backend.overcast.ai.ExplanationService;
import com.ironhack.backend.overcast.repo.FindingRepository;
import com.ironhack.backend.overcast.service.NotFoundException;
import com.ironhack.backend.overcast.web.dto.Dtos.ExplainResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/findings")
public class FindingController {

    private final FindingRepository findings;
    private final ExplanationService explanations;

    public FindingController(FindingRepository findings, ExplanationService explanations) {
        this.findings = findings;
        this.explanations = explanations;
    }

    /**
     * AI phrases the explanation when a key is configured; otherwise the
     * deterministic template answers. Cached in finding.explanation_cache so
     * each finding pays for at most one AI call, ever.
     */
    @PostMapping("/{id}/explain")
    public ExplainResponse explain(@PathVariable String id) {
        var finding = findings.findById(id)
                .orElseThrow(() -> new NotFoundException("No finding with id '" + id + "'"));

        if (finding.explanationCache() != null && !finding.explanationCache().isBlank()) {
            return new ExplainResponse(finding.explanationCache(), finding.remediation(), "cache");
        }
        var explanation = explanations.explain(finding);
        findings.updateExplanation(id, explanation.explanation());
        return new ExplainResponse(explanation.explanation(), explanation.remediation(), explanation.source());
    }
}
