package com.app.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HeatingType implements DbValueEnum {
    CENTRAL("central"),
    CENTRAL_GAS("central_gas"),
    GAS("gas"),
    ELECTRIC("electric"),
    HEAT_PUMP("heat_pump"),
    AIR_WATER_HEAT_PUMP("air_water_heat_pump"),
    GEOTHERMAL("geothermal"),
    SOLID_FUEL("solid_fuel"),
    STOVE("stove"),
    COMBINED("combined"),
    NONE("none");

    @EnumeratedValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static HeatingType fromDbValue(String value) {
        for (HeatingType h : values()) {
            if (h.dbValue.equals(value)) return h;
        }
        throw new IllegalArgumentException("Unknown heating_type: " + value);
    }
}
