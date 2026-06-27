package com.app.backend.job;

import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.service.EmailService;
import com.app.backend.service.UploadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.app.backend.enums.PropertyStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.app.backend.testutil.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InactiveAccountPurgeJobTest {

    @Mock private UserRepository userRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private UploadService uploadService;
    @Mock private EmailService emailService;

    @InjectMocks
    private InactiveAccountPurgeJob purgeJob;

    @BeforeEach
    void initTransactionSync() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSync() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void triggerAfterCommit() {
        new ArrayList<>(TransactionSynchronizationManager.getSynchronizations())
                .forEach(TransactionSynchronization::afterCommit);
    }

    @Test
    void deletesInactiveUsers_andCleansUpS3Photos() {
        User inactive = user("inactive@test.com");
        Property prop = propertyWithPhotos(inactive);
        when(userRepository.findAboutToBeInactive(any(), any(), any())).thenReturn(List.of());
        when(userRepository.findInactiveWithoutActiveListings(any(OffsetDateTime.class), any(PropertyStatus.class))).thenReturn(List.of(inactive));
        when(propertyRepository.findByOwner(inactive)).thenReturn(List.of(prop));

        purgeJob.runInactiveAccountJob();
        triggerAfterCommit();

        verify(userRepository).delete(inactive);
        verify(uploadService).deleteObjects(List.of(
                "https://cdn.test.local/uploads/photo1.jpg",
                "https://cdn.test.local/uploads/photo2.jpg"
        ));
    }

    @Test
    void doesNothing_whenNoInactiveUsers() {
        when(userRepository.findAboutToBeInactive(any(), any(), any())).thenReturn(List.of());
        when(userRepository.findInactiveWithoutActiveListings(any(), any())).thenReturn(List.of());

        purgeJob.runInactiveAccountJob();

        verify(userRepository, never()).delete(any());
        verify(uploadService, never()).deleteObjects(any());
    }

    @Test
    void handlesS3Failure_gracefully() {
        User inactive = user("inactive@test.com");
        Property prop = propertyWithPhotos(inactive);
        when(userRepository.findAboutToBeInactive(any(), any(), any())).thenReturn(List.of());
        when(userRepository.findInactiveWithoutActiveListings(any(), any())).thenReturn(List.of(inactive));
        when(propertyRepository.findByOwner(inactive)).thenReturn(List.of(prop));
        doThrow(new RuntimeException("S3 down")).when(uploadService).deleteObjects(any());

        purgeJob.runInactiveAccountJob();
        triggerAfterCommit();

        verify(userRepository).delete(inactive);
    }

    @Test
    void usesTwoYearCutoff_forPurge() {
        when(userRepository.findAboutToBeInactive(any(), any(), any())).thenReturn(List.of());
        when(userRepository.findInactiveWithoutActiveListings(any(OffsetDateTime.class), any(PropertyStatus.class)))
                .thenReturn(List.of());

        purgeJob.runInactiveAccountJob();

        ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(userRepository).findInactiveWithoutActiveListings(captor.capture(), any(PropertyStatus.class));

        OffsetDateTime cutoff = captor.getValue();
        OffsetDateTime twoYearsAgo = OffsetDateTime.now().minusYears(2);
        assertThat(cutoff.toInstant()).isBetween(
                twoYearsAgo.minusSeconds(5).toInstant(),
                twoYearsAgo.plusSeconds(5).toInstant()
        );
    }

    @Test
    void sendsInactivityWarnings_afterCommit() {
        User toWarn = user("warn@test.com");
        when(userRepository.findAboutToBeInactive(any(), any(), any())).thenReturn(List.of(toWarn));
        when(userRepository.findInactiveWithoutActiveListings(any(), any())).thenReturn(List.of());

        purgeJob.runInactiveAccountJob();

        verify(emailService, never()).sendInactivityWarningEmail(any(), any());

        triggerAfterCommit();

        verify(emailService).sendInactivityWarningEmail(toWarn.getEmail(), toWarn.getName());
    }
}
