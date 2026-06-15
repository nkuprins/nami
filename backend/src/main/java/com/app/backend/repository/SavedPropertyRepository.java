package com.app.backend.repository;

import com.app.backend.entity.SavedProperty;
import com.app.backend.entity.SavedPropertyId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SavedPropertyRepository extends JpaRepository<SavedProperty, SavedPropertyId> {
    List<SavedProperty> findByIdUserId(UUID userId);
}
