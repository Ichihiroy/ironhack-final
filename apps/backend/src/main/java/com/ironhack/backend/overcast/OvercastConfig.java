package com.ironhack.backend.overcast;

import com.ironhack.backend.overcast.ai.AiProperties;
import com.ironhack.backend.overcast.ai.AzureOpenAiClient;
import com.ironhack.backend.overcast.ai.ExplanationService;
import com.ironhack.backend.overcast.rules.RulesConfig;
import com.ironhack.backend.overcast.rules.RulesEngine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class OvercastConfig {

    @Bean
    public RulesConfig rulesConfig() {
        return RulesConfig.load();
    }

    @Bean
    public RulesEngine rulesEngine(RulesConfig config) {
        return new RulesEngine(config);
    }

    @Bean
    public ExplanationService explanationService(AzureOpenAiClient client) {
        return new ExplanationService(client);
    }

    /**
     * In-cluster the frontend and API share one ingress host (same-origin, CORS
     * unused). This mapping exists for local docker-compose where the frontend
     * runs on :8081 — read-only demo API, no credentials, so '*' is acceptable.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**").allowedOrigins("*").allowedMethods("GET", "POST");
            }
        };
    }
}
