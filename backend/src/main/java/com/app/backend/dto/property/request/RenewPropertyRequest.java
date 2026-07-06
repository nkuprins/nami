package com.app.backend.dto.property.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RenewPropertyRequest(
        @NotNull @Min(1) @Max(6) Integer durationMonths
) {}
