package com.ironhack.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Plain unit test — no Spring context, no database, keeps CI fail-fast and hermetic. */
class ItemControllerTest {

    private final ItemController controller = new ItemController();

    @Test
    void returnsSeededItems() {
        var response = controller.items();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull().hasSize(5);
        assertThat(response.getBody().getFirst().name()).isEqualTo("Alpha");
    }

    @Test
    void responseIsCacheable() {
        var response = controller.items();

        assertThat(response.getHeaders().getCacheControl()).contains("max-age=30");
    }
}
