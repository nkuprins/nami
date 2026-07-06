package com.app.backend.dto.property.request;

import com.app.backend.dto.property.model.LocalizedText;
import com.app.backend.dto.property.model.Price;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCompletion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Adds a new listing (e.g. rent) to a property that already has at least one
 * other listing (e.g. buy). The property's physical attributes, location and
 * media are not repeated here — they already exist on the target property.
 */
@Builder(toBuilder = true)
public record AddListingRequest(
        @NotNull ListingType type,
        @NotNull @Valid Price price,
        Map<String, LocalizedText> translations,
        List<@NotBlank String> phones,
        PropertyCompletion completion,
        @NotNull @Min(1) @Max(6) Integer durationMonths
) {}
