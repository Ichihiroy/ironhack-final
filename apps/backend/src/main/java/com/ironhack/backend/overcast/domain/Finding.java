package com.ironhack.backend.overcast.domain;

import java.math.BigDecimal;

public record Finding(
        String id,
        String scanId,
        String resourceId,
        String resourceType,
        String resourceGroup,
        String region,
        String ruleId,
        Category category,
        BigDecimal monthlyCost,
        BigDecimal monthlySaving,
        String remediation,
        String explanationCache) {}
