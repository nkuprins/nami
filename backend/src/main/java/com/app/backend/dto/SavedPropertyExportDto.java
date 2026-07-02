package com.app.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SavedPropertyExportDto(UUID listingId, OffsetDateTime savedAt) {}
