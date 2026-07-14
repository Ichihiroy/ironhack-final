package com.ironhack.backend.overcast.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Waste category of a finding — drives the UI color coding. GOVERNANCE is
 * the $0 bucket (tag hygiene): never money, never counted as waste.
 */
public enum Category {
    IDLE, OVERSIZED, FORGOTTEN, GOVERNANCE;

    @JsonValue
    public String json() {
        return name().toLowerCase();
    }

    public static Category fromDb(String value) {
        return valueOf(value.toUpperCase());
    }
}
