package com.app.backend.dto.property.model;

import com.app.backend.enums.BathroomLayout;
import com.app.backend.enums.EnergyClass;
import com.app.backend.enums.HeatingType;
import com.app.backend.enums.RoofType;
import com.app.backend.enums.SewageType;
import com.app.backend.enums.VentilationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * The physical attributes of a property. Shared by request and response DTOs:
 * validation fires only where a request marks the field {@code @Valid};
 * {@code NON_NULL} trims absent optional fields on responses (e.g. list cards
 * leave {@code bathroomLayout}, {@code heating}, {@code energyClass} and
 * {@code maintenanceCost} null).
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PropertyDetails(
        @NotNull @Min(1) Short rooms,
        @Min(0) @Max(100) Short bedrooms,
        @Min(0) @Max(100) Short bathrooms,
        BathroomLayout bathroomLayout,
        @NotNull @DecimalMin("1.00") @DecimalMax("9999.99") @Digits(integer = 4, fraction = 2) BigDecimal m2,
        @DecimalMin("0.01") @DecimalMax("999999.99") @Digits(integer = 6, fraction = 2) BigDecimal landM2,
        @Min(0) @Max(100) Short floor,
        @Min(1) @Max(100) Short totalFloors,
        @Min(1800) @Max(2035) Short yearBuilt,
        HeatingType heating,
        EnergyClass energyClass,
        @DecimalMin("0.00") @DecimalMax("99999999.99") @Digits(integer = 8, fraction = 2) BigDecimal maintenanceCost,
        SewageType sewage,
        VentilationType ventilation,
        RoofType roof
) {}
