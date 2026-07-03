package com.app.backend.repository;

import com.app.backend.entity.PendingMediaDeletion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PendingMediaDeletionRepository extends JpaRepository<PendingMediaDeletion, UUID> {
}
