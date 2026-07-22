package com.app.backend.dto.property.request;

import com.app.backend.enums.BathroomLayout;
import com.app.backend.enums.CommercialType;
import com.app.backend.enums.Communication;
import com.app.backend.enums.EnergyClass;
import com.app.backend.enums.HeatingType;
import com.app.backend.enums.LandUse;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.ParkingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyExtra;
import com.app.backend.enums.PropertyFeature;
import com.app.backend.enums.RoofType;
import com.app.backend.enums.SecurityFeature;
import com.app.backend.enums.SewageType;
import com.app.backend.enums.StoveType;
import com.app.backend.enums.VentilationSystem;
import com.app.backend.enums.VentilationType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record PropertyFilter(
        @NotNull ListingType type,
        PropertyCategory kind,
        CommercialType commercialSubtype,
        LandUse landUse,
        List<String> loc,
        List<Long> street,
        @DecimalMin("0.01") @DecimalMax("999999999999.99") BigDecimal priceMin,
        @DecimalMin("0.01") @DecimalMax("999999999999.99") BigDecimal priceMax,
        List<Integer> rooms,
        List<Integer> bedrooms,
        List<Integer> bathrooms,
        @DecimalMin("1.00") @DecimalMax("9999.99") BigDecimal m2Min,
        @DecimalMin("1.00") @DecimalMax("9999.99") BigDecimal m2Max,
        @DecimalMin("1.00") @DecimalMax("999999.99") BigDecimal landM2Min,
        @DecimalMin("1.00") @DecimalMax("999999.99") BigDecimal landM2Max,
        @Min(0) @Max(100) Short floorMin,
        @Min(0) @Max(100) Short floorMax,
        Boolean notGround,
        Boolean notTop,
        @Min(1800) @Max(2035) Short yearMin,
        @Min(1800) @Max(2035) Short yearMax,
        @DecimalMin("0.01") @DecimalMax("99999.99") BigDecimal maintenanceCostMax,
        BathroomLayout bathroomLayout,
        Boolean vatIncluded,
        List<HeatingType> heating,
        List<EnergyClass> energyClass,
        List<SewageType> sewage,
        List<VentilationType> ventilation,
        List<RoofType> roof,
        List<PropertyFeature> features,
        List<VentilationSystem> ventilationSystems,
        List<Communication> communications,
        List<StoveType> stove,
        List<SecurityFeature> security,
        List<PropertyExtra> extras,
        List<ParkingType> parking,
        PropertyCompletion completion
) {}
