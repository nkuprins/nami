package com.app.backend.service;

import com.app.backend.entity.PendingMediaDeletion;
import com.app.backend.messaging.MediaVariants;
import com.app.backend.repository.PendingMediaDeletionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Durable deletion of media from S3. Callers record obsolete media inside their own transaction
 * via {@link #enqueue}; an immediate post-commit delete handles the happy path, and
 * {@link #drainPendingDeletions} retries anything a crash or S3 error left behind. This guarantees
 * media is never orphaned by a failure between the DB commit and the S3 delete.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaCleanupService {

    private final PendingMediaDeletionRepository repository;
    private final UploadService uploadService;

    /**
     * Records the given media for deletion in the caller's transaction, then attempts an immediate
     * best-effort delete once that transaction commits. Anything not confirmed deleted survives as a
     * pending row for {@link #drainPendingDeletions} to retry. Must run inside the caller's
     * transaction (MANDATORY) so the pending rows commit atomically with the change that obsoleted
     * the media — otherwise a rollback would leave rows scheduling deletion of still-referenced media.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(List<String> cdnUrls) {
        if (cdnUrls.isEmpty()) return;

        // Photos have resized derivatives in S3; schedule those for deletion too so none are orphaned.
        // Media without variants (e.g. plans) just yields keys that don't exist, which S3 delete no-ops.
        List<PendingMediaDeletion> pending = MediaVariants.withVariants(cdnUrls).stream()
                .map(PendingMediaDeletion::new)
                .toList();
        repository.saveAll(pending);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteAndClear(pending);
            }
        });
    }

    /**
     * Retries media that the immediate post-commit delete never confirmed (crash or S3 error).
     * Triggered on a schedule by {@code MediaCleanupDrainJob} (scheduler role); kept here so the
     * transactional delete logic lives with the rest of the cleanup service.
     */
    @Transactional
    public void drainPendingDeletions() {
        List<PendingMediaDeletion> pending = repository.findAll();
        if (pending.isEmpty()) return;

        log.info("Media cleanup drain: retrying {} pending deletion(s)", pending.size());
        deleteAndClear(pending);
    }

    /** Deletes the given media from S3 and removes the rows that were confirmed gone. */
    private void deleteAndClear(List<PendingMediaDeletion> pending) {
        List<String> urls = pending.stream().map(PendingMediaDeletion::getCdnUrl).toList();

        List<String> failedUrls;
        try {
            failedUrls = uploadService.deleteObjects(urls);
        } catch (Exception e) {
            markFailed(pending, e.getMessage());
            return;
        }

        List<PendingMediaDeletion> deleted = pending.stream()
                .filter(p -> !failedUrls.contains(p.getCdnUrl()))
                .toList();
        repository.deleteAll(deleted);

        if (!failedUrls.isEmpty()) {
            markFailed(pending.stream().filter(p -> failedUrls.contains(p.getCdnUrl())).toList(),
                    "S3 reported a delete error");
        }
    }

    private void markFailed(List<PendingMediaDeletion> pending, String error) {
        for (PendingMediaDeletion p : pending) {
            p.setAttempts(p.getAttempts() + 1);
            p.setLastError(error);
        }
        repository.saveAll(pending);
        log.warn("Media cleanup: {} object(s) left pending after failed delete: {}", pending.size(), error);
    }
}
