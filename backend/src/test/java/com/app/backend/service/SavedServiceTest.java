package com.app.backend.service;

import com.app.backend.entity.SavedListing;
import com.app.backend.entity.SavedListingId;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.SavedListingRepository;
import com.app.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.app.backend.exception.ApiException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedServiceTest {

    @Mock private SavedListingRepository savedListingRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SavedService savedService;

    @Test
    void getSavedIds_returnsListingIds() {
        UUID userId = UUID.randomUUID();
        UUID listingId1 = UUID.randomUUID();
        UUID listingId2 = UUID.randomUUID();

        SavedListing sl1 = new SavedListing();
        sl1.setId(new SavedListingId(userId, listingId1));
        SavedListing sl2 = new SavedListing();
        sl2.setId(new SavedListingId(userId, listingId2));

        when(savedListingRepository.findByIdUserId(userId)).thenReturn(List.of(sl1, sl2));

        List<UUID> result = savedService.getSavedIds(userId);

        assertThat(result).containsExactly(listingId1, listingId2);
    }

    @Test
    void getSavedIds_returnsEmptyList_whenNoneSaved() {
        UUID userId = UUID.randomUUID();
        when(savedListingRepository.findByIdUserId(userId)).thenReturn(List.of());

        assertThat(savedService.getSavedIds(userId)).isEmpty();
    }

    @Test
    void save_createsSavedListing_whenNotAlreadySaved() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        SavedListingId id = new SavedListingId(userId, listingId);
        when(savedListingRepository.existsById(id)).thenReturn(false);
        when(listingRepository.existsById(listingId)).thenReturn(true);
        when(userRepository.getReferenceById(userId)).thenReturn(null);
        when(listingRepository.getReferenceById(listingId)).thenReturn(null);

        savedService.save(userId, listingId);

        verify(savedListingRepository).save(any(SavedListing.class));
    }

    @Test
    void save_doesNothing_whenAlreadySaved() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(savedListingRepository.existsById(new SavedListingId(userId, listingId))).thenReturn(true);

        savedService.save(userId, listingId);

        verify(savedListingRepository, never()).save(any());
    }

    @Test
    void save_throwsNotFound_whenListingNotFound() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        when(savedListingRepository.existsById(any())).thenReturn(false);
        when(listingRepository.existsById(listingId)).thenReturn(false);

        assertThatThrownBy(() -> savedService.save(userId, listingId))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void unsave_deletesById() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        savedService.unsave(userId, listingId);

        verify(savedListingRepository).deleteById(new SavedListingId(userId, listingId));
    }
}
