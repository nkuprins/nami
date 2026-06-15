package com.app.backend.repository;

import com.app.backend.entity.PropertyPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PropertyPhotoRepository extends JpaRepository<PropertyPhoto, UUID> {
    List<PropertyPhoto> findByPropertyIdOrderByPositionAsc(UUID propertyId);
}
