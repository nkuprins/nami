package com.app.backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PropertyStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String dbValue;

    public static PropertyStatus fromDbValue(String value) {
        for (PropertyStatus s : values()) {
            if (s.dbValue.equals(value)) return s;
        }
        throw new IllegalArgumentException("Unknown property_status: " + value);
    }
}
