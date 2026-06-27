package com.app.backend.job;

import com.app.backend.entity.Property;
import com.app.backend.entity.PropertyTranslation;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PropertyExpiryJob {

    private final PropertyRepository propertyRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runPropertyExpiryJob() {
        sendExpiryWarnings();
        expireListings();
    }

    private void sendExpiryWarnings() {
        OffsetDateTime warnCutoff = OffsetDateTime.now().plusDays(7);
        List<Property> expiring = propertyRepository.findExpiringUnwarned(PropertyStatus.ACTIVE, warnCutoff);

        if (expiring.isEmpty()) return;

        record OwnerDigest(String email, String name, List<String> titles) {}
        Map<String, OwnerDigest> byOwner = new LinkedHashMap<>();
        for (Property p : expiring) {
            p.setExpiryWarningSent(true);
            String email = p.getOwner().getEmail();
            byOwner.compute(email, (k, existing) -> {
                List<String> titles = existing != null ? existing.titles() : new ArrayList<>();
                titles.add(resolveTitle(p.getTranslations()));
                return new OwnerDigest(email, p.getOwner().getName(), titles);
            });
        }

        List<OwnerDigest> digests = List.copyOf(byOwner.values());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (OwnerDigest d : digests) {
                    try {
                        emailService.sendListingExpiryWarningEmail(d.email(), d.name(), d.titles());
                    } catch (Exception e) {
                        log.warn("Failed to send expiry warning to {}: {}", d.email(), e.getMessage());
                    }
                }
                log.info("Listing expiry warnings sent to {} owners ({} listings)", digests.size(), expiring.size());
            }
        });
    }

    private void expireListings() {
        List<Property> expired = propertyRepository.findExpired(PropertyStatus.ACTIVE, OffsetDateTime.now());

        if (expired.isEmpty()) return;

        log.info("Property expiry: found {} listings to expire", expired.size());

        record OwnerDigest(String email, String name, List<String> titles) {}
        Map<String, OwnerDigest> byOwner = new LinkedHashMap<>();
        for (Property p : expired) {
            p.setStatus(PropertyStatus.INACTIVE);
            String email = p.getOwner().getEmail();
            byOwner.compute(email, (k, existing) -> {
                List<String> titles = existing != null ? existing.titles() : new ArrayList<>();
                titles.add(resolveTitle(p.getTranslations()));
                return new OwnerDigest(email, p.getOwner().getName(), titles);
            });
        }

        List<OwnerDigest> digests = List.copyOf(byOwner.values());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (OwnerDigest d : digests) {
                    try {
                        emailService.sendListingExpiredEmail(d.email(), d.name(), d.titles());
                    } catch (Exception e) {
                        log.warn("Failed to send expiry notification to {}: {}", d.email(), e.getMessage());
                    }
                }
                log.info("Property expiry: expired {} listings across {} owners", expired.size(), digests.size());
            }
        });
    }

    private static String resolveTitle(Map<String, PropertyTranslation> translations) {
        PropertyTranslation lv = translations.get("lv");
        if (lv != null) return lv.getTitle();
        PropertyTranslation en = translations.get("en");
        if (en != null) return en.getTitle();
        PropertyTranslation ru = translations.get("ru");
        return ru != null ? ru.getTitle() : "";
    }
}
