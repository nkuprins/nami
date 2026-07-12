package com.app.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SewageType implements DbValueEnum {
    CENTRAL("central"),
    LOCAL("local");

    @EnumeratedValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static SewageType fromDbValue(String value) {
        for (SewageType s : values()) {
            if (s.dbValue.equals(value)) return s;
        }
        throw new IllegalArgumentException("Unknown sewage_type: " + value);
    }
}
