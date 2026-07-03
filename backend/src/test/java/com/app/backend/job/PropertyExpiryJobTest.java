package com.app.backend.job;

import com.app.backend.entity.Listing;
import com.app.backend.entity.User;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.service.EmailService;
import com.app.backend.service.MediaCleanupService;
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

    @Mock private ListingRepository listingRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private EmailService emailService;
    @Mock private MediaCleanupService mediaCleanupService;

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
        Listing l = listing(owner);
        l.setStatus(PropertyStatus.ACTIVE);
        when(listingRepository.findExpired(eq(PropertyStatus.ACTIVE), any())).thenReturn(List.of(l));
        when(listingRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());
        when(listingRepository.findInactiveExpiredBefore(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();

        assertThat(l.getStatus()).isEqualTo(PropertyStatus.INACTIVE);
    }

    @Test
    void sendsExpiredEmail_afterCommit() {
        User owner = user("owner@test.com");
        Listing l = listing(owner);
        when(listingRepository.findExpired(eq(PropertyStatus.ACTIVE), any())).thenReturn(List.of(l));
        when(listingRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());
        when(listingRepository.findInactiveExpiredBefore(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();

        verify(emailService, never()).sendListingExpiredEmail(any(), any(), any());

        triggerAfterCommit();

        verify(emailService).sendListingExpiredEmail(eq(owner.getEmail()), eq(owner.getName()), any());
    }

    @Test
    void groupsExpiredEmails_byOwner() {
        User owner = user("owner@test.com");
        Listing l1 = listing(owner);
        Listing l2 = listing(owner);
        when(listingRepository.findExpired(eq(PropertyStatus.ACTIVE), any())).thenReturn(List.of(l1, l2));
        when(listingRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());
        when(listingRepository.findInactiveExpiredBefore(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();
        triggerAfterCommit();

        verify(emailService, times(1)).sendListingExpiredEmail(eq(owner.getEmail()), eq(owner.getName()), any());
    }

    // --- sendExpiryWarnings ---

    @Test
    void setsExpiryWarningSent_andSendsWarningEmail() {
        User owner = user("owner@test.com");
        Listing l = listing(owner);
        l.setExpiryWarningSent(false);
        when(listingRepository.findExpiringUnwarned(eq(PropertyStatus.ACTIVE), any())).thenReturn(List.of(l));
        when(listingRepository.findExpired(any(), any())).thenReturn(List.of());
        when(listingRepository.findInactiveExpiredBefore(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();

        assertThat(l.isExpiryWarningSent()).isTrue();

        verify(emailService, never()).sendListingExpiryWarningEmail(any(), any(), any());
        triggerAfterCommit();
        verify(emailService).sendListingExpiryWarningEmail(eq(owner.getEmail()), eq(owner.getName()), any());
    }

    @Test
    void usesSevenDayLookahead_forWarnings() {
        when(listingRepository.findExpiringUnwarned(eq(PropertyStatus.ACTIVE), any())).thenReturn(List.of());
        when(listingRepository.findExpired(any(), any())).thenReturn(List.of());
        when(listingRepository.findInactiveExpiredBefore(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();

        ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(listingRepository).findExpiringUnwarned(any(), captor.capture());

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
        Listing l = listingWithPhotos(owner);
        l.setStatus(PropertyStatus.INACTIVE);
        when(listingRepository.findInactiveExpiredBefore(eq(PropertyStatus.INACTIVE), any())).thenReturn(List.of(l));
        when(listingRepository.findExpired(any(), any())).thenReturn(List.of());
        when(listingRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());
        when(listingRepository.countByPropertyId(l.getProperty().getId())).thenReturn(0L);

        expiryJob.runPropertyExpiryJob();

        verify(listingRepository).deleteAll(List.of(l));
    }

    @Test
    void enqueuesS3Deletion_forPurgedListings() {
        User owner = user("owner@test.com");
        Listing l = listingWithPhotos(owner);
        l.setStatus(PropertyStatus.INACTIVE);
        when(listingRepository.findInactiveExpiredBefore(eq(PropertyStatus.INACTIVE), any())).thenReturn(List.of(l));
        when(listingRepository.findExpired(any(), any())).thenReturn(List.of());
        when(listingRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());
        when(listingRepository.countByPropertyId(l.getProperty().getId())).thenReturn(0L);

        expiryJob.runPropertyExpiryJob();

        verify(mediaCleanupService).enqueue(List.of(
                "https://cdn.test.local/uploads/photo1.jpg",
                "https://cdn.test.local/uploads/photo2.jpg"
        ));
    }

    @Test
    void usesNinetyDayCutoff_forPurge() {
        when(listingRepository.findInactiveExpiredBefore(eq(PropertyStatus.INACTIVE), any())).thenReturn(List.of());
        when(listingRepository.findExpired(any(), any())).thenReturn(List.of());
        when(listingRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();

        ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(listingRepository).findInactiveExpiredBefore(any(), captor.capture());

        OffsetDateTime ninetyDaysAgo = OffsetDateTime.now().minusDays(90);
        assertThat(captor.getValue().toInstant()).isBetween(
                ninetyDaysAgo.minusSeconds(5).toInstant(),
                ninetyDaysAgo.plusSeconds(5).toInstant()
        );
    }

    @Test
    void skipsS3Call_whenPurgedListingsHaveNoMedia() {
        User owner = user("owner@test.com");
        Listing l = listing(owner);
        l.setStatus(PropertyStatus.INACTIVE);
        when(listingRepository.findInactiveExpiredBefore(eq(PropertyStatus.INACTIVE), any())).thenReturn(List.of(l));
        when(listingRepository.findExpired(any(), any())).thenReturn(List.of());
        when(listingRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());
        when(listingRepository.countByPropertyId(l.getProperty().getId())).thenReturn(0L);

        expiryJob.runPropertyExpiryJob();
        triggerAfterCommit();

        verify(mediaCleanupService, never()).enqueue(any());
    }

    @Test
    void doesNothing_whenNoListingsToProcess() {
        when(listingRepository.findExpired(any(), any())).thenReturn(List.of());
        when(listingRepository.findExpiringUnwarned(any(), any())).thenReturn(List.of());
        when(listingRepository.findInactiveExpiredBefore(any(), any())).thenReturn(List.of());

        expiryJob.runPropertyExpiryJob();

        verify(listingRepository, never()).deleteAll(any());
        verify(emailService, never()).sendListingExpiredEmail(any(), any(), any());
        verify(mediaCleanupService, never()).enqueue(any());
    }
}
