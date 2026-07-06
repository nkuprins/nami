package com.app.backend.job;

import com.app.backend.service.MediaCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler-role trigger for the durable media-cleanup drain. Split from {@link MediaCleanupService}
 * (which stays unprofiled because its {@code enqueue} is called by the web role) so the periodic
 * drain runs on exactly one instance and never double-fires when web and worker are deployed apart.
 */
@Component
@Profile("scheduler")
@RequiredArgsConstructor
public class MediaCleanupDrainJob {

    private final MediaCleanupService mediaCleanupService;

    @Scheduled(cron = "0 0 2 * * *")
    public void run() {
        mediaCleanupService.drainPendingDeletions();
    }
}
