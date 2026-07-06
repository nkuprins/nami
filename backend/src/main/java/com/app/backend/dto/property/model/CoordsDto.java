package com.app.backend.dto.property.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record CoordsDto(
        @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
        @DecimalMin("-180.0") @DecimalMax("180.0") double lng
) {}
