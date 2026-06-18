package com.app.backend.service;

import com.app.backend.entity.SavedProperty;
import com.app.backend.entity.SavedPropertyId;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.repository.SavedPropertyRepository;
import com.app.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedService {

    private final SavedPropertyRepository savedPropertyRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UUID> getSavedIds(UUID userId) {
        return savedPropertyRepository.findByIdUserId(userId)
                .stream()
                .map(sp -> sp.getId().getPropertyId())
                .toList();
    }

    @Transactional
    public void save(UUID userId, UUID propertyId) {
        SavedPropertyId id = new SavedPropertyId(userId, propertyId);
        if (savedPropertyRepository.existsById(id)) return;
        if (!propertyRepository.existsById(propertyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        SavedProperty sp = new SavedProperty();
        sp.setId(id);
        sp.setUser(userRepository.getReferenceById(userId));
        sp.setProperty(propertyRepository.getReferenceById(propertyId));
        savedPropertyRepository.save(sp);
    }

    @Transactional
    public void unsave(UUID userId, UUID propertyId) {
        savedPropertyRepository.deleteById(new SavedPropertyId(userId, propertyId));
    }
}
