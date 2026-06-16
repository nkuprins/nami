package com.app.backend.enums;

import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PropertyCompletion {
    READY("ready"),
    NOT_READY("not_ready");

    @EnumeratedValue
    private final String dbValue;

    public static PropertyCompletion fromDbValue(String value) {
        for (PropertyCompletion c : values()) {
            if (c.dbValue.equals(value)) return c;
        }
        throw new IllegalArgumentException("Unknown property_completion: " + value);
    }
}
