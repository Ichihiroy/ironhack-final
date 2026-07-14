package com.ironhack.backend.overcast.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thin Azure OpenAI chat-completions client (plain REST, no SDK). Returns
 * Optional.empty() on ANY problem — no key, timeout, quota, bad response —
 * so callers always fall back to the deterministic path. The AI can only
 * ever produce prose; it has no way to influence a savings figure.
 *
 * Routes: by default the evergreen "/openai/v1" endpoint (no api-version,
 * current parameter names — what recent models expect). Setting
 * AZURE_OPENAI_API_VERSION to a dated version ("2024-06-01") switches to the
 * legacy per-deployment route for older resources.
 *
 * Timeouts are hard requirements: without them a slow model call rides past
 * the ingress' 60s proxy timeout and the user sees a 504 instead of the
 * deterministic fallback.
 */
@Component
public class AzureOpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(AzureOpenAiClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(40);

    private final AiProperties props;
    private final RestClient rest;

    public AzureOpenAiClient(AiProperties props) {
        this.props = props;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        this.rest = RestClient.builder().requestFactory(factory).build();
    }

    public boolean configured() {
        return props.configured();
    }

    /** One chat completion; empty on any failure (caller falls back). */
    public Optional<String> complete(String systemPrompt, String userPrompt) {
        if (!props.configured()) return Optional.empty();
        try {
            String base = props.endpoint().replaceAll("/+$", "");
            String apiVersion = props.apiVersionOrDefault("v1");
            boolean legacy = !"v1".equalsIgnoreCase(apiVersion);

            String url;
            Map<String, Object> body = new LinkedHashMap<>();
            if (legacy) {
                url = base + "/openai/deployments/" + props.deployment()
                        + "/chat/completions?api-version=" + apiVersion;
                body.put("max_tokens", 800);
            } else {
                url = base + "/openai/v1/chat/completions";
                body.put("model", props.deployment());
                // reasoning models spend part of the budget thinking — keep
                // headroom so the visible answer isn't truncated to nothing
                body.put("max_completion_tokens", 800);
            }
            // no temperature: newer models only accept the default, and the
            // numbers are the rules engine's anyway — only phrasing varies
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)));

            JsonNode response = rest.post()
                    .uri(url)
                    .header("api-key", props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String content = response == null ? null
                    : response.path("choices").path(0).path("message").path("content").asText(null);
            return Optional.ofNullable(content).filter(s -> !s.isBlank());
        } catch (RestClientResponseException e) {
            log.warn("Azure OpenAI returned {} — using deterministic fallback. Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Azure OpenAI call failed, using deterministic fallback: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
