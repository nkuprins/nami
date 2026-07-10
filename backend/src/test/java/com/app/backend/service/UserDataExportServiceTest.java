package com.app.backend.service;

import com.app.backend.dto.export.UserExportDto;
import com.app.backend.entity.*;
import com.app.backend.exception.AuthException;
import com.app.backend.mapper.PropertyMapper;
import com.app.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.app.backend.testutil.TestData.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDataExportServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private SavedListingRepository savedListingRepository;
    @Spy private PropertyMapper propertyMapper = new PropertyMapper();

    @InjectMocks
    private UserDataExportService userDataExportService;

    @Test
    void returnsDto_withOwnedAndSavedProperties() {
        User user = user();
        Listing owned = listing(user);
        UUID savedListingId = UUID.randomUUID();
        SavedListing saved = new SavedListing();
        saved.setId(new SavedListingId(user.getId(), savedListingId));
        saved.setSavedAt(OffsetDateTime.now());

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(listingRepository.findByOwner(user)).thenReturn(List.of(owned));
        when(savedListingRepository.findByIdUserId(user.getId())).thenReturn(List.of(saved));

        UserExportDto result = userDataExportService.export(user.getId());

        assertThat(result.id()).isEqualTo(user.getId());
        assertThat(result.email()).isEqualTo(user.getEmail());
        assertThat(result.ownedProperties()).hasSize(1);
        assertThat(result.savedProperties()).hasSize(1);
        assertThat(result.savedProperties().getFirst().listingId()).isEqualTo(savedListingId);
    }

    @Test
    void returnsDto_withEmptyLists_whenNoPropertiesOrSaved() {
        User user = user();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(listingRepository.findByOwner(user)).thenReturn(List.of());
        when(savedListingRepository.findByIdUserId(user.getId())).thenReturn(List.of());

        UserExportDto result = userDataExportService.export(user.getId());

        assertThat(result.ownedProperties()).isEmpty();
        assertThat(result.savedProperties()).isEmpty();
    }

    @Test
    void throwsUnauthorized_whenUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDataExportService.export(id))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
