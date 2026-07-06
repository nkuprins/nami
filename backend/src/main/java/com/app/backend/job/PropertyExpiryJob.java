package com.app.backend.job;

import com.app.backend.entity.Listing;
import com.app.backend.entity.ListingTranslation;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.service.EmailService;
import com.app.backend.service.MediaCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
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
import java.util.UUID;

@Slf4j
@Component
@Profile("scheduler")
@RequiredArgsConstructor
public class PropertyExpiryJob {

    private final ListingRepository listingRepository;
    private final PropertyRepository propertyRepository;
    private final EmailService emailService;
    private final MediaCleanupService mediaCleanupService;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runPropertyExpiryJob() {
        sendExpiryWarnings();
        expireListings();
        purgeExpiredInactiveListings();
    }

    private void sendExpiryWarnings() {
        OffsetDateTime warnCutoff = OffsetDateTime.now().plusDays(7);
        List<Listing> expiring = listingRepository.findExpiringUnwarned(PropertyStatus.ACTIVE, warnCutoff);

        if (expiring.isEmpty()) return;

        record OwnerDigest(String email, String name, List<String> titles) {}
        Map<String, OwnerDigest> byOwner = new LinkedHashMap<>();
        for (Listing l : expiring) {
            l.setExpiryWarningSent(true);
            String email = l.getOwner().getEmail();
            byOwner.compute(email, (k, existing) -> {
                List<String> titles = existing != null ? existing.titles() : new ArrayList<>();
                titles.add(resolveTitle(l.getTranslations()));
                return new OwnerDigest(email, l.getOwner().getName(), titles);
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
        List<Listing> expired = listingRepository.findExpired(PropertyStatus.ACTIVE, OffsetDateTime.now());

        if (expired.isEmpty()) return;

        log.info("Listing expiry: found {} listings to expire", expired.size());

        record OwnerDigest(String email, String name, List<String> titles) {}
        Map<String, OwnerDigest> byOwner = new LinkedHashMap<>();
        for (Listing l : expired) {
            l.setStatus(PropertyStatus.INACTIVE);
            String email = l.getOwner().getEmail();
            byOwner.compute(email, (k, existing) -> {
                List<String> titles = existing != null ? existing.titles() : new ArrayList<>();
                titles.add(resolveTitle(l.getTranslations()));
                return new OwnerDigest(email, l.getOwner().getName(), titles);
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
                log.info("Listing expiry: expired {} listings across {} owners", expired.size(), digests.size());
            }
        });
    }

    private void purgeExpiredInactiveListings() {
        OffsetDateTime purgeCutoff = OffsetDateTime.now().minusDays(90);
        List<Listing> toPurge = listingRepository.findInactiveExpiredBefore(PropertyStatus.INACTIVE, purgeCutoff);

        if (toPurge.isEmpty()) return;

        log.info("Listing purge: found {} inactive listings to permanently delete", toPurge.size());

        // Collect media URLs and property IDs before deletion
        List<String> allMediaUrls = new ArrayList<>();
        List<UUID> propertyIds = new ArrayList<>();
        for (Listing l : toPurge) {
            allMediaUrls.addAll(l.getProperty().allMediaUrls());
            propertyIds.add(l.getProperty().getId());
        }

        listingRepository.deleteAll(toPurge);

        // Delete orphan properties (those with no remaining listings)
        for (UUID propId : propertyIds) {
            if (listingRepository.countByPropertyId(propId) == 0) {
                propertyRepository.deleteById(propId);
            }
        }

        log.info("Listing purge: permanently deleted {} inactive listings", toPurge.size());

        if (!allMediaUrls.isEmpty()) {
            mediaCleanupService.enqueue(allMediaUrls);
        }
    }

    private static String resolveTitle(Map<String, ListingTranslation> translations) {
        ListingTranslation lv = translations.get("lv");
        if (lv != null) return lv.getTitle();
        ListingTranslation en = translations.get("en");
        if (en != null) return en.getTitle();
        ListingTranslation ru = translations.get("ru");
        return ru != null ? ru.getTitle() : "";
    }
}
