package com.ironhack.backend.overcast.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Azure OpenAI wiring, server-side ONLY — the key never leaves the backend.
 * All-blank (the default) puts the whole app in deterministic-fallback mode.
 */
@ConfigurationProperties(prefix = "overcast.ai")
public record AiProperties(String endpoint, String apiKey, String deployment) {

    public boolean configured() {
        return notBlank(endpoint) && notBlank(apiKey) && notBlank(deployment);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
