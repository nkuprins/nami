package com.app.backend.dto.property.request;

import com.app.backend.dto.property.model.LocalizedText;
import com.app.backend.dto.property.model.Location;
import com.app.backend.dto.property.model.Media;
import com.app.backend.dto.property.model.PhoneContact;
import com.app.backend.dto.property.model.Price;
import com.app.backend.dto.property.model.PropertyDetails;
import com.app.backend.enums.CommercialType;
import com.app.backend.enums.Communication;
import com.app.backend.enums.LandUse;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.ParkingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyExtra;
import com.app.backend.enums.PropertyFeature;
import com.app.backend.enums.SecurityFeature;
import com.app.backend.enums.StoveType;
import com.app.backend.enums.VentilationSystem;
import com.app.backend.validation.ValidPropertyRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
@ValidPropertyRequest
public record CreatePropertyRequest(
        @NotNull ListingType type,
        @NotNull PropertyCategory propertyKind,
        PropertyCategory newProjectKind,
        CommercialType commercialSubtype,
        LandUse landUse,
        @NotNull @Valid Price price,
        @NotNull @Valid PropertyDetails details,
        Map<String, LocalizedText> translations,
        @NotNull @Valid Location location,
        List<PropertyFeature> features,
        List<VentilationSystem> ventilationSystems,
        List<Communication> communications,
        List<StoveType> stove,
        List<SecurityFeature> security,
        List<PropertyExtra> extras,
        List<ParkingType> parking,
        @Valid Media media,
        @Valid List<PhoneContact> phones,
        PropertyCompletion completion,
        @NotNull @Min(1) @Max(6) Integer durationMonths,
        Boolean confirmedDuplicate
) implements PropertyRequest {}
