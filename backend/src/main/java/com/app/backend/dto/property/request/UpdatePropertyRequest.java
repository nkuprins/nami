package com.app.backend.dto.property.request;

import com.app.backend.dto.property.model.Location;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * Updates a property's shared address (location). Every physical/media attribute
 * lives on the listing now and is edited via the listing endpoint instead.
 */
@Builder(toBuilder = true)
public record UpdatePropertyRequest(
        @NotNull @Valid Location location
) {}
