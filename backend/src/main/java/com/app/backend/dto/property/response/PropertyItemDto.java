package com.app.backend.dto.property.response;

import com.app.backend.dto.property.model.LocalizedText;
import com.app.backend.dto.property.model.Location;
import com.app.backend.dto.property.model.Media;
import com.app.backend.dto.property.model.Price;
import com.app.backend.dto.property.model.PropertyDetails;
import com.app.backend.enums.Communication;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.ParkingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyExtra;
import com.app.backend.enums.PropertyFeature;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.enums.SecurityFeature;
import com.app.backend.enums.StoveType;
import com.app.backend.enums.VentilationSystem;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PropertyItemDto(
        UUID id,
        UUID propertyId,
        UUID ownerId,
        ListingType type,
        PropertyCategory propertyKind,
        Price price,
        PropertyDetails details,
        Map<String, LocalizedText> translations,
        List<String> availableLocales,
        Location location,
        List<PropertyFeature> features,
        List<VentilationSystem> ventilationSystems,
        List<Communication> communications,
        List<StoveType> stove,
        List<SecurityFeature> security,
        List<PropertyExtra> extras,
        List<ParkingType> parking,
        Media media,
        List<String> phones,
        OffsetDateTime postedAt,
        PropertyCompletion completion,
        OffsetDateTime expiresAt,
        PropertyStatus status
) {}
