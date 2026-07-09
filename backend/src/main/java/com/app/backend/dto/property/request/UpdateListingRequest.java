package com.app.backend.dto.property.request;

import com.app.backend.dto.property.model.LocalizedText;
import com.app.backend.dto.property.model.Media;
import com.app.backend.dto.property.model.Price;
import com.app.backend.dto.property.model.PropertyDetails;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyFeature;
import com.app.backend.validation.ValidPropertyRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Updates a self-contained listing (its physical attributes, media, features and
 * terms). The shared address (location) is edited via the separate property
 * endpoint instead.
 */
@Builder(toBuilder = true)
@ValidPropertyRequest
public record UpdateListingRequest(
        @NotNull ListingType type,
        @NotNull PropertyCategory propertyKind,
        @NotNull @Valid Price price,
        @NotNull @Valid PropertyDetails details,
        Map<String, LocalizedText> translations,
        List<PropertyFeature> features,
        @Valid Media media,
        List<@NotBlank String> phones,
        PropertyCompletion completion
) implements PropertyRequest {}
