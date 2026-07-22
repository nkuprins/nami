package com.app.backend.dto.property.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * A property's location. Shared by request and response DTOs; {@code coords} is
 * left null on list cards (dropped by {@code NON_NULL}).
 *
 * <p>{@code arBuildingCode} links the address to a State Address Register
 * building; {@code address} is then derived server-side from the register
 * (street + house number, or a quoted rural house name) plus the free-typed
 * {@code apartment}. Both are null on legacy free-text addresses.
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Location(
        @NotBlank String district,
        @NotBlank String city,
        @NotBlank String address,
        Long arBuildingCode,
        // The building's register street; derived server-side from arBuildingCode,
        // never trusted from the client. Null on legacy/rural addresses.
        Long arStreetCode,
        @Size(max = 16) String apartment,
        // Cadastral parcel the plot was picked from (land & commercial); null otherwise.
        @Size(max = 32) String cadastreParcelNr,
        @NotNull @Valid CoordsDto coords
) {}
