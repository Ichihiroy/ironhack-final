package com.ironhack.backend.overcast.seed;

import com.ironhack.backend.overcast.repo.ScanRepository;
import com.ironhack.backend.overcast.service.ScanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Seeds the "demo" scan from the hero sample bill on startup, so k6 and the
 * stage demo work with zero setup. Idempotent and replica-safe: a concurrent
 * pod winning the insert race is success, not failure.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_SCAN_ID = "demo";
    private static final String HERO_SAMPLE = "samples/azure-hero-messy.csv";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final ScanRepository scans;
    private final ScanService scanService;

    public DemoDataSeeder(ScanRepository scans, ScanService scanService) {
        this.scans = scans;
        this.scanService = scanService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (scans.exists(DEMO_SCAN_ID)) {
            log.info("Demo scan already present — skipping seed");
            return;
        }
        var resource = new ClassPathResource(HERO_SAMPLE);
        try (var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            var summary = scanService.ingest(reader, "azure-hero-messy.csv", DEMO_SCAN_ID);
            log.info("Seeded demo scan: {} findings, {}/mo waste",
                    summary.findingCount(), summary.totalMonthlyWaste());
        } catch (DuplicateKeyException e) {
            log.info("Another replica seeded the demo scan first — fine");
        }
    }
}
