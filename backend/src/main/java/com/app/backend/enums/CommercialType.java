package com.app.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommercialType implements DbValueEnum {
    OFFICE("office"),
    WAREHOUSE("warehouse"),
    RETAIL("retail"),
    INDUSTRIAL("industrial"),
    HOSPITALITY("hospitality");

    @EnumeratedValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static CommercialType fromDbValue(String value) {
        for (CommercialType c : values()) {
            if (c.dbValue.equals(value)) return c;
        }
        throw new IllegalArgumentException("Unknown commercial_type: " + value);
    }
}
