package com.app.backend.job;

import com.app.backend.config.AppProperties;
import com.app.backend.dto.address.AddressIngestStats;
import com.app.backend.repository.AddressRegistryRepository;
import com.app.backend.service.AddressRegistryIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the State Address Register mirror fresh: a weekly reload (VZD publishes
 * updates continuously) plus a one-off bootstrap ingest when the mirror is
 * empty on startup, so a fresh deployment self-populates. Both are gated by
 * {@code app.address-register.auto-ingest} so tests and local setups without
 * the register stay quiet.
 */
@Slf4j
@Component
@Profile("scheduler")
@RequiredArgsConstructor
public class AddressRegistryRefreshJob {

    private final AddressRegistryIngestService ingestService;
    private final AddressRegistryRepository repository;
    private final AppProperties appProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapIfEmpty() {
        if (!enabled() || repository.countTerritories() > 0) {
            return;
        }
        log.info("Address register mirror is empty — bootstrapping ingest in the background");
        Thread.ofVirtual().name("address-register-bootstrap").start(this::runIngest);
    }

    @Scheduled(cron = "0 30 3 * * MON")
    public void weeklyRefresh() {
        if (enabled()) {
            runIngest();
        }
    }

    private boolean enabled() {
        return appProperties.addressRegister().autoIngest();
    }

    private void runIngest() {
        try {
            AddressIngestStats stats = ingestService.ingest();
            log.info("Address register refresh finished: {}", stats);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Address register refresh interrupted");
        } catch (Exception e) {
            log.error("Address register refresh failed", e);
        }
    }
}
