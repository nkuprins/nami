package com.app.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PropertyExtra implements DbValueEnum {
    SEPARATE_ENTRANCE("separate_entrance"),
    ENCLOSED_YARD("enclosed_yard"),
    PRIVATE_GARDEN("private_garden"),
    FURNITURE("furniture"),
    FURNITURE_POSSIBLE("furniture_possible");

    @EnumeratedValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static PropertyExtra fromDbValue(String value) {
        for (PropertyExtra v : values()) {
            if (v.dbValue.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown property_extra: " + value);
    }
}
