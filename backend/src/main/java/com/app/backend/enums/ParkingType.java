package com.app.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ParkingType implements DbValueEnum {
    FREE_PARKING("free_parking"),
    PAID_PARKING("paid_parking"),
    NO_PARKING("no_parking"),
    UNDERGROUND_PARKING("underground_parking"),
    OWN_PARKING_SPACE("own_parking_space");

    @EnumeratedValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static ParkingType fromDbValue(String value) {
        for (ParkingType v : values()) {
            if (v.dbValue.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown parking_type: " + value);
    }
}
