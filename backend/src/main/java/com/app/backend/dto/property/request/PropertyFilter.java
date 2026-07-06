package com.app.backend.dto.property.request;

import com.app.backend.enums.EnergyClass;
import com.app.backend.enums.HeatingType;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyFeature;
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
        List<String> loc,
        @DecimalMin("0.01") @DecimalMax("999999999999.99") BigDecimal priceMin,
        @DecimalMin("0.01") @DecimalMax("999999999999.99") BigDecimal priceMax,
        List<Integer> rooms,
        List<Integer> bedrooms,
        List<Integer> bathrooms,
        @DecimalMin("1.00") @DecimalMax("9999.99") BigDecimal m2Min,
        @DecimalMin("1.00") @DecimalMax("9999.99") BigDecimal m2Max,
        @Min(0) @Max(100) Short floorMin,
        @Min(0) @Max(100) Short floorMax,
        Boolean notGround,
        Boolean notTop,
        @Min(1800) @Max(2035) Short yearMin,
        @Min(1800) @Max(2035) Short yearMax,
        List<HeatingType> heating,
        List<EnergyClass> energyClass,
        List<PropertyFeature> features,
        PropertyCompletion completion
) {}
