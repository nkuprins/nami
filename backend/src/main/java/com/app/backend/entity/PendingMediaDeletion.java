package com.app.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pending_media_deletions")
@Getter
@Setter
@NoArgsConstructor
public class PendingMediaDeletion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "cdn_url", nullable = false)
    private String cdnUrl;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "last_error")
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public PendingMediaDeletion(String cdnUrl) {
        this.cdnUrl = cdnUrl;
    }
}
