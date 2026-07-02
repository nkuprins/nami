package com.app.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BathroomLayout implements DbValueEnum {
    SEPARATE("separate"),
    COMBINED("combined");

    @EnumeratedValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static BathroomLayout fromDbValue(String value) {
        for (BathroomLayout b : values()) {
            if (b.dbValue.equals(value)) return b;
        }
        throw new IllegalArgumentException("Unknown bathroom_layout: " + value);
    }
}
