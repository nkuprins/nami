package com.app.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LandUse implements DbValueEnum {
    RESIDENTIAL("residential"),
    COMMERCIAL("commercial"),
    AGRICULTURAL("agricultural"),
    FOREST("forest");

    @EnumeratedValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static LandUse fromDbValue(String value) {
        for (LandUse u : values()) {
            if (u.dbValue.equals(value)) return u;
        }
        throw new IllegalArgumentException("Unknown land_use: " + value);
    }
}
