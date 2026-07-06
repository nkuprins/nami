package com.app.backend.dto.export;

import com.app.backend.dto.property.response.PropertyItemDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserExportDto(
        UUID id,
        String name,
        String email,
        boolean emailVerified,
        OffsetDateTime createdAt,
        List<PropertyItemDto> ownedProperties,
        List<SavedPropertyExportDto> savedProperties
) {}
