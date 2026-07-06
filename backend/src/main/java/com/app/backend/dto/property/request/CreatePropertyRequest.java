package com.app.backend.dto.property.request;

import com.app.backend.dto.property.model.LocalizedText;
import com.app.backend.dto.property.model.Location;
import com.app.backend.dto.property.model.Media;
import com.app.backend.dto.property.model.Price;
import com.app.backend.dto.property.model.PropertyDetails;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyFeature;
import com.app.backend.validation.ValidPropertyRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
@ValidPropertyRequest
public record CreatePropertyRequest(
        @NotNull ListingType type,
        @NotNull PropertyCategory propertyKind,
        @NotNull @Valid Price price,
        @NotNull @Valid PropertyDetails details,
        Map<String, LocalizedText> translations,
        @NotNull @Valid Location location,
        List<PropertyFeature> features,
        @Valid Media media,
        List<@NotBlank String> phones,
        PropertyCompletion completion,
        @NotNull @Min(1) @Max(6) Integer durationMonths,
        Boolean confirmedDuplicate
) implements PropertyRequest {}
