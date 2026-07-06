package com.app.backend.dto.property.response;

import com.app.backend.dto.property.model.Location;
import com.app.backend.dto.property.model.Media;
import com.app.backend.dto.property.model.PropertyDetails;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyFeature;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PropertyDto(
        UUID id,
        UUID ownerId,
        PropertyCategory propertyKind,
        PropertyDetails details,
        Media media,
        List<PropertyFeature> features,
        Location location
) {}
