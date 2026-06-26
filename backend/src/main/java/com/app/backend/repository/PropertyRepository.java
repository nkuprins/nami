package com.app.backend.repository;

import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {

    @EntityGraph(attributePaths = {"owner"})
    List<Property> findByOwner(User owner);

    @EntityGraph(attributePaths = {"owner"})
    Page<Property> findAll(Specification<Property> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"owner", "photos", "plans", "phones", "features", "translations"})
    Optional<Property> findById(UUID id);
}
