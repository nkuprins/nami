package com.app.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StoveType implements DbValueEnum {
    ELECTRIC_STOVE("electric_stove"),
    WOOD_BURNING("wood_burning"),
    GAS_STOVE("gas_stove");

    @EnumeratedValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static StoveType fromDbValue(String value) {
        for (StoveType v : values()) {
            if (v.dbValue.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown stove_type: " + value);
    }
}
