package com.app.backend.enums;

import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PropertyStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    PENDING_REVIEW("pending_review");

    @EnumeratedValue
    private final String dbValue;

    public static PropertyStatus fromDbValue(String value) {
        for (PropertyStatus s : values()) {
            if (s.dbValue.equals(value)) return s;
        }
        throw new IllegalArgumentException("Unknown property_status: " + value);
    }
}
