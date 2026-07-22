package com.app.backend.dto.property.response;

import com.app.backend.dto.property.model.LocalizedText;
import com.app.backend.dto.property.model.Location;
import com.app.backend.dto.property.model.Media;
import com.app.backend.dto.property.model.Price;
import com.app.backend.dto.property.model.PropertyDetails;
import com.app.backend.enums.CommercialType;
import com.app.backend.enums.LandUse;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyFeature;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PropertyListItemDto(
        UUID id,
        UUID propertyId,
        UUID ownerId,
        ListingType type,
        PropertyCategory propertyKind,
        PropertyCategory newProjectKind,
        CommercialType commercialSubtype,
        LandUse landUse,
        Price price,
        PropertyDetails details,
        Map<String, LocalizedText> translations,
        Location location,
        List<PropertyFeature> features,
        String photo,
        OffsetDateTime postedAt,
        PropertyCompletion completion,
        OffsetDateTime expiresAt
) {}
