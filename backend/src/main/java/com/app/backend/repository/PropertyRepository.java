package com.app.backend.repository;

import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {

    List<Property> findByOwner(User owner);

    long countByOwner(User owner);
}
