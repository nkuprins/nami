package com.app.backend.job;

import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.repository.PropertyRepository;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.app.backend.testutil.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyExpiryJobTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private EmailService emailService;
    @Mock private UploadService uploadService;

    @InjectMocks
    private PropertyExpiryJob expiryJob;

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

    // --- expireListings ---

    @Test
    void setsStatusInactive_forExpiredActiveListings() {
        User owner = user("owner@test.com");
        Property p = property(owner);
        p.setStatus(PropertyStatus.ACTIVE);
        when(propertyRepository.findExpired(eq(PropertyStatus.ACTIVE), any())).thenReturn(List.of(p));
        when(propertyRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());
        when(propertyRepository.findInactiveExpiredBefore(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();

        assertThat(p.getStatus()).isEqualTo(PropertyStatus.INACTIVE);
    }

    @Test
    void sendsExpiredEmail_afterCommit() {
        User owner = user("owner@test.com");
        Property p = property(owner);
        when(propertyRepository.findExpired(eq(PropertyStatus.ACTIVE), any())).thenReturn(List.of(p));
        when(propertyRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());
        when(propertyRepository.findInactiveExpiredBefore(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();

        verify(emailService, never()).sendListingExpiredEmail(any(), any(), any());

        triggerAfterCommit();

        verify(emailService).sendListingExpiredEmail(eq(owner.getEmail()), eq(owner.getName()), any());
    }

    @Test
    void groupsExpiredEmails_byOwner() {
        User owner = user("owner@test.com");
        Property p1 = property(owner);
        Property p2 = property(owner);
        when(propertyRepository.findExpired(eq(PropertyStatus.ACTIVE), any())).thenReturn(List.of(p1, p2));
        when(propertyRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());
        when(propertyRepository.findInactiveExpiredBefore(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();
        triggerAfterCommit();

        // one email digest per owner, not one per listing
        verify(emailService, times(1)).sendListingExpiredEmail(eq(owner.getEmail()), eq(owner.getName()), any());
    }

    // --- sendExpiryWarnings ---

    @Test
    void setsExpiryWarningSent_andSendsWarningEmail() {
        User owner = user("owner@test.com");
        Property p = property(owner);
        p.setExpiryWarningSent(false);
        when(propertyRepository.findExpiringUnwarned(eq(PropertyStatus.ACTIVE), any())).thenReturn(List.of(p));
        when(propertyRepository.findExpired(any(), any())).thenReturn(List.of());
        when(propertyRepository.findInactiveExpiredBefore(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();

        assertThat(p.isExpiryWarningSent()).isTrue();

        verify(emailService, never()).sendListingExpiryWarningEmail(any(), any(), any());
        triggerAfterCommit();
        verify(emailService).sendListingExpiryWarningEmail(eq(owner.getEmail()), eq(owner.getName()), any());
    }

    @Test
    void usesSevenDayLookahead_forWarnings() {
        when(propertyRepository.findExpiringUnwarned(eq(PropertyStatus.ACTIVE), any())).thenReturn(List.of());
        when(propertyRepository.findExpired(any(), any())).thenReturn(List.of());
        when(propertyRepository.findInactiveExpiredBefore(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();

        ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(propertyRepository).findExpiringUnwarned(any(), captor.capture());

        OffsetDateTime sevenDaysFromNow = OffsetDateTime.now().plusDays(7);
        assertThat(captor.getValue().toInstant()).isBetween(
                sevenDaysFromNow.minusSeconds(5).toInstant(),
                sevenDaysFromNow.plusSeconds(5).toInstant()
        );
    }

    // --- purgeExpiredInactiveListings ---

    @Test
    void deletesInactiveListings_olderThan90Days() {
        User owner = user("owner@test.com");
        Property p = propertyWithPhotos(owner);
        p.setStatus(PropertyStatus.INACTIVE);
        when(propertyRepository.findInactiveExpiredBefore(eq(PropertyStatus.INACTIVE), any())).thenReturn(List.of(p));
        when(propertyRepository.findExpired(any(), any())).thenReturn(List.of());
        when(propertyRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();

        verify(propertyRepository).deleteAll(List.of(p));
    }

    @Test
    void deletesS3Objects_afterCommit_forPurgedListings() {
        User owner = user("owner@test.com");
        Property p = propertyWithPhotos(owner);
        p.setStatus(PropertyStatus.INACTIVE);
        when(propertyRepository.findInactiveExpiredBefore(eq(PropertyStatus.INACTIVE), any())).thenReturn(List.of(p));
        when(propertyRepository.findExpired(any(), any())).thenReturn(List.of());
        when(propertyRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();

        verify(uploadService, never()).deleteObjects(any());
        triggerAfterCommit();

        verify(uploadService).deleteObjects(List.of(
                "https://cdn.test.local/uploads/photo1.jpg",
                "https://cdn.test.local/uploads/photo2.jpg"
        ));
    }

    @Test
    void usesNinetyDayCutoff_forPurge() {
        when(propertyRepository.findInactiveExpiredBefore(eq(PropertyStatus.INACTIVE), any())).thenReturn(List.of());
        when(propertyRepository.findExpired(any(), any())).thenReturn(List.of());
        when(propertyRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();

        ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(propertyRepository).findInactiveExpiredBefore(any(), captor.capture());

        OffsetDateTime ninetyDaysAgo = OffsetDateTime.now().minusDays(90);
        assertThat(captor.getValue().toInstant()).isBetween(
                ninetyDaysAgo.minusSeconds(5).toInstant(),
                ninetyDaysAgo.plusSeconds(5).toInstant()
        );
    }

    @Test
    void handlesS3Failure_gracefully_onPurge() {
        User owner = user("owner@test.com");
        Property p = propertyWithPhotos(owner);
        p.setStatus(PropertyStatus.INACTIVE);
        when(propertyRepository.findInactiveExpiredBefore(eq(PropertyStatus.INACTIVE), any())).thenReturn(List.of(p));
        when(propertyRepository.findExpired(any(), any())).thenReturn(List.of());
        when(propertyRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());
        doThrow(new RuntimeException("S3 down")).when(uploadService).deleteObjects(any());

        expiryJob.runPropertyExpiryJob();
        triggerAfterCommit();

        // DB deletion already happened before the S3 failure
        verify(propertyRepository).deleteAll(List.of(p));
    }

    @Test
    void skipsS3Call_whenPurgedListingsHaveNoMedia() {
        User owner = user("owner@test.com");
        Property p = property(owner); // no photos or plans
        p.setStatus(PropertyStatus.INACTIVE);
        when(propertyRepository.findInactiveExpiredBefore(eq(PropertyStatus.INACTIVE), any())).thenReturn(List.of(p));
        when(propertyRepository.findExpired(any(), any())).thenReturn(List.of());
        when(propertyRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();
        triggerAfterCommit();

        verify(uploadService, never()).deleteObjects(any());
    }

    @Test
    void doesNothing_whenNoListingsToProcess() {
        when(propertyRepository.findExpired(any(), any())).thenReturn(List.of());
        when(propertyRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());
        when(propertyRepository.findInactiveExpiredBefore(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();

        verify(propertyRepository, never()).deleteAll(any());
        verify(emailService, never()).sendListingExpiredEmail(any(), any(), any());
        verify(uploadService, never()).deleteObjects(any());
    }
}
