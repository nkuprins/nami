package com.app.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * A property's location. Shared by request and response DTOs; {@code coords} is
 * left null on list cards (dropped by {@code NON_NULL}).
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Location(
        @NotBlank String district,
        @NotBlank String city,
        @NotBlank String address,
        @NotNull @Valid CoordsDto coords
) {}
