package com.app.backend.dto.export;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SavedPropertyExportDto(UUID listingId, OffsetDateTime savedAt) {}
