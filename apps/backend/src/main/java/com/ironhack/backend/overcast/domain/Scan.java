package com.ironhack.backend.overcast.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Scan(
        String id,
        String sourceCloud,
        String filename,
        Instant uploadedAt,
        String currency,
        BigDecimal totalMonthlyCost,
        BigDecimal totalMonthlyWaste) {}
