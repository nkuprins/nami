package com.app.backend.job;

import com.app.backend.entity.PropertyPhoto;
import com.app.backend.entity.User;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InactiveAccountPurgeJob {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final UploadService uploadService;

    @Scheduled(cron = "0 0 4 1 * *")
    @Transactional
    public void purgeInactiveAccounts() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusYears(2);
        List<User> inactive = userRepository.findInactiveWithoutActiveListings(cutoff);

        if (inactive.isEmpty()) return;

        log.info("Inactive account purge: found {} accounts to delete", inactive.size());

        for (User user : inactive) {
            List<String> photoUrls = propertyRepository.findByOwner(user).stream()
                    .flatMap(p -> p.getPhotos().stream())
                    .map(PropertyPhoto::getUrl)
                    .toList();

            userRepository.delete(user);

            if (!photoUrls.isEmpty()) {
                try {
                    uploadService.deleteObjects(photoUrls);
                } catch (Exception e) {
                    log.warn("Failed to delete S3 objects for inactive user {}: {}", user.getId(), e.getMessage());
                }
            }
        }

        log.info("Inactive account purge: deleted {} accounts", inactive.size());
    }
}
