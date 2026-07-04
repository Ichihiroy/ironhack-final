package com.ironhack.backend;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Placeholder endpoint proving the pipeline end to end. Swapping in the real
 * product means replacing this class (and friends) — the contract (/api/items,
 * port 8080, actuator probes) stays stable so infra, Helm and CI never change.
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    public record Item(long id, String name, String category) {}

    private static final List<Item> ITEMS = List.of(
            new Item(1, "Alpha", "demo"),
            new Item(2, "Bravo", "demo"),
            new Item(3, "Charlie", "demo"),
            new Item(4, "Delta", "demo"),
            new Item(5, "Echo", "demo"));

    @GetMapping
    public ResponseEntity<List<Item>> items() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic())
                .body(ITEMS);
    }
}
