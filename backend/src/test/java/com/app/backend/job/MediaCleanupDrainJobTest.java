package com.app.backend.job;

import com.app.backend.service.MediaCleanupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MediaCleanupDrainJobTest {

    @Mock private MediaCleanupService mediaCleanupService;

    @InjectMocks
    private MediaCleanupDrainJob job;

    @Test
    void run_delegatesToDrain() {
        job.run();
        verify(mediaCleanupService).drainPendingDeletions();
    }
}
