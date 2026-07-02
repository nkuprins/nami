package com.app.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PropertyCategory implements DbValueEnum {
    APARTMENT("apartment"),
    HOUSE("house");

    @EnumeratedValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static PropertyCategory fromDbValue(String value) {
        for (PropertyCategory c : values()) {
            if (c.dbValue.equals(value)) return c;
        }
        throw new IllegalArgumentException("Unknown property_category: " + value);
    }
}
