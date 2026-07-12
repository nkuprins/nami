package com.app.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VentilationType implements DbValueEnum {
    NATURAL("natural"),
    MECHANICAL("mechanical"),
    RECUPERATION("recuperation");

    @EnumeratedValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static VentilationType fromDbValue(String value) {
        for (VentilationType v : values()) {
            if (v.dbValue.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ventilation_type: " + value);
    }
}
