package com.ironhack.backend.overcast.rules;

import com.ironhack.backend.overcast.domain.Category;
import com.ironhack.backend.overcast.domain.NormalizedResource;

import java.math.BigDecimal;

/** One rule firing on one resource, before persistence. */
public record RuleMatch(
        NormalizedResource resource,
        String ruleId,
        Category category,
        BigDecimal monthlySaving,
        String remediation) {}
