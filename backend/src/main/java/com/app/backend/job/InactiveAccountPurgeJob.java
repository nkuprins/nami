package com.app.backend.job;

import com.app.backend.entity.User;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.UserRepository;
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
import java.util.List;

@Slf4j
@Component
@Profile("scheduler")
@RequiredArgsConstructor
public class InactiveAccountPurgeJob {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final MediaCleanupService mediaCleanupService;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 4 1 * *")
    @Transactional
    public void runInactiveAccountJob() {
        OffsetDateTime purgeCutoff = OffsetDateTime.now().minusYears(2);
        OffsetDateTime warnCutoff  = OffsetDateTime.now().minusMonths(23);
        sendInactivityWarnings(warnCutoff, purgeCutoff);
        purgeInactiveAccounts(purgeCutoff);
    }

    private void sendInactivityWarnings(OffsetDateTime warnCutoff, OffsetDateTime purgeCutoff) {
        List<User> toWarn = userRepository.findAboutToBeInactive(warnCutoff, purgeCutoff, PropertyStatus.ACTIVE);
        if (toWarn.isEmpty()) return;

        record EmailData(String email, String name) {}
        List<EmailData> emails = toWarn.stream()
                .map(u -> new EmailData(u.getEmail(), u.getName()))
                .toList();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (EmailData d : emails) {
                    try {
                        emailService.sendInactivityWarningEmail(d.email(), d.name());
                    } catch (Exception e) {
                        log.warn("Failed to send inactivity warning to {}: {}", d.email(), e.getMessage());
                    }
                }
                log.info("Inactivity warning emails sent to {} accounts", emails.size());
            }
        });
    }

    private void purgeInactiveAccounts(OffsetDateTime purgeCutoff) {
        List<User> inactive = userRepository.findInactiveWithoutActiveListings(purgeCutoff, PropertyStatus.ACTIVE);
        if (inactive.isEmpty()) return;

        log.info("Inactive account purge: found {} accounts to delete", inactive.size());

        List<String> allPhotoUrls = new ArrayList<>();
        for (User user : inactive) {
            listingRepository.findByOwner(user).stream()
                    .flatMap(l -> l.allMediaUrls().stream())
                    .forEach(allPhotoUrls::add);
            userRepository.delete(user);
        }

        if (!allPhotoUrls.isEmpty()) {
            mediaCleanupService.enqueue(allPhotoUrls);
        }

        log.info("Inactive account purge: deleted {} accounts", inactive.size());
    }
}
