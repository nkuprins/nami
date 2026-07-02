package com.app.backend.service;

import com.app.backend.entity.SavedListing;
import com.app.backend.entity.SavedListingId;
import com.app.backend.exception.ApiException;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.SavedListingRepository;
import com.app.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedService {

    private final SavedListingRepository savedListingRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UUID> getSavedIds(UUID userId) {
        return savedListingRepository.findByIdUserId(userId)
                .stream()
                .map(sl -> sl.getId().listingId())
                .toList();
    }

    @Transactional
    public void save(UUID userId, UUID listingId) {
        SavedListingId id = new SavedListingId(userId, listingId);
        if (savedListingRepository.existsById(id)) return;
        if (!listingRepository.existsById(listingId)) {
            throw new ApiException(HttpStatus.NOT_FOUND);
        }
        SavedListing sl = new SavedListing();
        sl.setId(id);
        sl.setUser(userRepository.getReferenceById(userId));
        sl.setListing(listingRepository.getReferenceById(listingId));
        savedListingRepository.save(sl);
    }

    @Transactional
    public void unsave(UUID userId, UUID listingId) {
        savedListingRepository.deleteById(new SavedListingId(userId, listingId));
    }
}
