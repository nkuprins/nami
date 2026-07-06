package com.app.backend.service;

import com.app.backend.entity.PendingMediaDeletion;
import com.app.backend.repository.PendingMediaDeletionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaCleanupServiceTest {

    @Mock private PendingMediaDeletionRepository repository;
    @Mock private UploadService uploadService;

    @InjectMocks
    private MediaCleanupService mediaCleanupService;

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void triggerAfterCommit() {
        new ArrayList<>(TransactionSynchronizationManager.getSynchronizations())
                .forEach(TransactionSynchronization::afterCommit);
    }

    @Test
    void enqueue_empty_savesNothing() {
        mediaCleanupService.enqueue(List.of());

        verifyNoInteractions(repository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void enqueue_persistsRowsInTransaction_thenDeletesFromS3AfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        when(uploadService.deleteObjects(anyList())).thenReturn(List.of());

        mediaCleanupService.enqueue(List.of("a.jpg", "b.jpg"));

        // Rows are persisted synchronously (in the caller's transaction) so a crash can't lose them.
        // Each photo is expanded to its variant keys so no derivative is orphaned.
        List<String> expanded = List.of(
                "a.jpg", "a_thumb.jpg", "a_card.jpg",
                "b.jpg", "b_thumb.jpg", "b_card.jpg");
        ArgumentCaptor<List<PendingMediaDeletion>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(PendingMediaDeletion::getCdnUrl)
                .containsExactlyElementsOf(expanded);
        // Nothing is deleted from S3 until the transaction commits.
        verifyNoInteractions(uploadService);

        triggerAfterCommit();

        verify(uploadService).deleteObjects(expanded);
        verify(repository).deleteAll(anyList());
    }

    @Test
    void drain_empty_doesNothing() {
        when(repository.findAll()).thenReturn(List.of());

        mediaCleanupService.drainPendingDeletions();

        verifyNoInteractions(uploadService);
    }

    @Test
    void drain_deletesAllRows_whenS3Succeeds() {
        PendingMediaDeletion a = new PendingMediaDeletion("url1");
        PendingMediaDeletion b = new PendingMediaDeletion("url2");
        when(repository.findAll()).thenReturn(List.of(a, b));
        when(uploadService.deleteObjects(List.of("url1", "url2"))).thenReturn(List.of());

        mediaCleanupService.drainPendingDeletions();

        verify(repository).deleteAll(List.of(a, b));
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void drain_keepsOnlyFailedRows_andBumpsAttempts() {
        PendingMediaDeletion ok = new PendingMediaDeletion("url1");
        PendingMediaDeletion bad = new PendingMediaDeletion("url2");
        when(repository.findAll()).thenReturn(List.of(ok, bad));
        when(uploadService.deleteObjects(List.of("url1", "url2"))).thenReturn(List.of("url2"));

        mediaCleanupService.drainPendingDeletions();

        verify(repository).deleteAll(List.of(ok));
        verify(repository).saveAll(List.of(bad));
        assertThat(bad.getAttempts()).isEqualTo(1);
        assertThat(bad.getLastError()).isNotNull();
    }

    @Test
    void drain_keepsAllRows_whenS3CallThrows() {
        PendingMediaDeletion a = new PendingMediaDeletion("url1");
        when(repository.findAll()).thenReturn(List.of(a));
        when(uploadService.deleteObjects(anyList())).thenThrow(new RuntimeException("S3 down"));

        mediaCleanupService.drainPendingDeletions();

        verify(repository, never()).deleteAll(anyList());
        verify(repository).saveAll(List.of(a));
        assertThat(a.getAttempts()).isEqualTo(1);
        assertThat(a.getLastError()).isEqualTo("S3 down");
    }
}
