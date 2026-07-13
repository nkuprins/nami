package com.app.backend.job;

import com.app.backend.config.AppProperties;
import com.app.backend.dto.cadastre.CadastreIngestStats;
import com.app.backend.repository.CadastreRepository;
import com.app.backend.service.CadastreIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the VZD cadastre mirror fresh: a weekly reload plus a one-off
 * bootstrap ingest when the mirror is empty on startup. Both are gated by
 * {@code app.cadastre.auto-ingest} so tests and local setups stay quiet.
 * Offset an hour after the address-register refresh so the two don't hit
 * VZD's open-data portal at the same instant.
 */
@Slf4j
@Component
@Profile("scheduler")
@RequiredArgsConstructor
public class CadastreRefreshJob {

    private final CadastreIngestService ingestService;
    private final CadastreRepository repository;
    private final AppProperties appProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapIfEmpty() {
        if (!enabled() || repository.countBuildings() > 0) {
            return;
        }
        log.info("Cadastre mirror is empty — bootstrapping ingest in the background");
        Thread.ofVirtual().name("cadastre-bootstrap").start(this::runIngest);
    }

    @Scheduled(cron = "0 30 4 * * MON")
    public void weeklyRefresh() {
        if (enabled()) {
            runIngest();
        }
    }

    private boolean enabled() {
        return appProperties.cadastre().autoIngest();
    }

    private void runIngest() {
        try {
            CadastreIngestStats stats = ingestService.ingest();
            log.info("Cadastre refresh finished: {}", stats);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Cadastre refresh interrupted");
        } catch (Exception e) {
            log.error("Cadastre refresh failed", e);
        }
    }
}
