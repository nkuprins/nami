package com.app.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VentilationSystem implements DbValueEnum {
    CLIMATE_CONTROL("climate_control"),
    SUPPLY_VENTILATION("supply_ventilation"),
    AIR_CONDITIONER("air_conditioner");

    @EnumeratedValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static VentilationSystem fromDbValue(String value) {
        for (VentilationSystem v : values()) {
            if (v.dbValue.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ventilation_system: " + value);
    }
}
