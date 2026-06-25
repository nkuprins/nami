package com.app.backend.service;

import com.app.backend.entity.SavedProperty;
import com.app.backend.entity.SavedPropertyId;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.repository.SavedPropertyRepository;
import com.app.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedServiceTest {

    @Mock private SavedPropertyRepository savedPropertyRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SavedService savedService;

    @Test
    void getSavedIds_returnsPropertyIds() {
        UUID userId = UUID.randomUUID();
        UUID propId1 = UUID.randomUUID();
        UUID propId2 = UUID.randomUUID();

        SavedProperty sp1 = new SavedProperty();
        sp1.setId(new SavedPropertyId(userId, propId1));
        SavedProperty sp2 = new SavedProperty();
        sp2.setId(new SavedPropertyId(userId, propId2));

        when(savedPropertyRepository.findByIdUserId(userId)).thenReturn(List.of(sp1, sp2));

        List<UUID> result = savedService.getSavedIds(userId);

        assertThat(result).containsExactly(propId1, propId2);
    }

    @Test
    void getSavedIds_returnsEmptyList_whenNoneSaved() {
        UUID userId = UUID.randomUUID();
        when(savedPropertyRepository.findByIdUserId(userId)).thenReturn(List.of());

        assertThat(savedService.getSavedIds(userId)).isEmpty();
    }

    @Test
    void save_createsSavedProperty_whenNotAlreadySaved() {
        UUID userId = UUID.randomUUID();
        UUID propId = UUID.randomUUID();
        SavedPropertyId id = new SavedPropertyId(userId, propId);
        when(savedPropertyRepository.existsById(id)).thenReturn(false);
        when(propertyRepository.existsById(propId)).thenReturn(true);
        when(userRepository.getReferenceById(userId)).thenReturn(null);
        when(propertyRepository.getReferenceById(propId)).thenReturn(null);

        savedService.save(userId, propId);

        verify(savedPropertyRepository).save(any(SavedProperty.class));
    }

    @Test
    void save_doesNothing_whenAlreadySaved() {
        UUID userId = UUID.randomUUID();
        UUID propId = UUID.randomUUID();
        when(savedPropertyRepository.existsById(new SavedPropertyId(userId, propId))).thenReturn(true);

        savedService.save(userId, propId);

        verify(savedPropertyRepository, never()).save(any());
    }

    @Test
    void save_throwsNotFound_whenPropertyNotFound() {
        UUID userId = UUID.randomUUID();
        UUID propId = UUID.randomUUID();
        when(savedPropertyRepository.existsById(any())).thenReturn(false);
        when(propertyRepository.existsById(propId)).thenReturn(false);

        assertThatThrownBy(() -> savedService.save(userId, propId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void unsave_deletesById() {
        UUID userId = UUID.randomUUID();
        UUID propId = UUID.randomUUID();

        savedService.unsave(userId, propId);

        verify(savedPropertyRepository).deleteById(new SavedPropertyId(userId, propId));
    }
}
