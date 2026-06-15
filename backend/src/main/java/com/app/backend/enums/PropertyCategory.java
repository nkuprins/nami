package com.app.backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PropertyCategory {
    APARTMENT("apartment"),
    HOUSE("house");

    private final String dbValue;

    public static PropertyCategory fromDbValue(String value) {
        for (PropertyCategory c : values()) {
            if (c.dbValue.equals(value)) return c;
        }
        throw new IllegalArgumentException("Unknown property_category: " + value);
    }
}
