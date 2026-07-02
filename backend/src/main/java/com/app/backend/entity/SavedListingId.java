package com.app.backend.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record SavedListingId(UUID userId, UUID listingId) implements Serializable {}
