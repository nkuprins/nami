package com.app.backend.enums;

import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PropertyCategory {
    APARTMENT("apartment"),
    HOUSE("house");

    @EnumeratedValue
    private final String dbValue;

    public static PropertyCategory fromDbValue(String value) {
        for (PropertyCategory c : values()) {
            if (c.dbValue.equals(value)) return c;
        }
        throw new IllegalArgumentException("Unknown property_category: " + value);
    }
}
