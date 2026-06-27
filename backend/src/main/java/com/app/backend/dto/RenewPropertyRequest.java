package com.app.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RenewPropertyRequest(
        @NotNull @Min(1) @Max(6) Integer durationMonths
) {}
