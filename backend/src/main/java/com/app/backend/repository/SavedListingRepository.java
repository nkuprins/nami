package com.app.backend.repository;

import com.app.backend.entity.SavedListing;
import com.app.backend.entity.SavedListingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SavedListingRepository extends JpaRepository<SavedListing, SavedListingId> {
    List<SavedListing> findByIdUserId(UUID userId);
}
