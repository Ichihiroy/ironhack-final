package com.ironhack.backend.overcast.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/** Waste category of a finding — drives the UI color coding. */
public enum Category {
    IDLE, OVERSIZED, FORGOTTEN;

    @JsonValue
    public String json() {
        return name().toLowerCase();
    }

    public static Category fromDb(String value) {
        return valueOf(value.toUpperCase());
    }
}
