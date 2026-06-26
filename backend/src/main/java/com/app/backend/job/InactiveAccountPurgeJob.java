package com.app.backend.job;

import com.app.backend.entity.PropertyPhoto;
import com.app.backend.entity.User;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.service.EmailService;
import com.app.backend.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class InactiveAccountPurgeJob {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final UploadService uploadService;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 4 1 * *")
    @Transactional
    public void purgeInactiveAccounts() {
        OffsetDateTime purgeCutoff = OffsetDateTime.now().minusYears(2);
        OffsetDateTime warnCutoff  = OffsetDateTime.now().minusMonths(23);

        List<User> toWarn = userRepository.findAboutToBeInactive(warnCutoff, purgeCutoff, PropertyStatus.ACTIVE);
        toWarn.forEach(u -> {
            try {
                emailService.sendInactivityWarningEmail(u.getEmail(), u.getName());
            } catch (Exception e) {
                log.warn("Failed to send inactivity warning to user {}: {}", u.getId(), e.getMessage());
            }
        });
        if (!toWarn.isEmpty()) {
            log.info("Inactivity warning emails sent to {} accounts", toWarn.size());
        }

        List<User> inactive = userRepository.findInactiveWithoutActiveListings(purgeCutoff, PropertyStatus.ACTIVE);

        if (inactive.isEmpty()) return;

        log.info("Inactive account purge: found {} accounts to delete", inactive.size());

        List<String> allPhotoUrls = new ArrayList<>();
        for (User user : inactive) {
            propertyRepository.findByOwner(user).stream()
                    .flatMap(p -> p.getPhotos().stream())
                    .map(PropertyPhoto::getUrl)
                    .forEach(allPhotoUrls::add);
            userRepository.delete(user);
        }

        if (!allPhotoUrls.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        uploadService.deleteObjects(allPhotoUrls);
                    } catch (Exception e) {
                        log.warn("Failed to delete S3 objects during inactive account purge: {}", e.getMessage());
                    }
                }
            });
        }

        log.info("Inactive account purge: deleted {} accounts", inactive.size());
    }
}
