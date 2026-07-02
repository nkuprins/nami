package com.app.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PropertyFeature implements DbValueEnum {
    BALCONY("balcony"),
    PARKING("parking"),
    ELEVATOR("elevator"),
    FURNISHED("furnished"),
    PETS("pets"),
    NEW_BUILDING("new_building"),
    BASEMENT("basement");

    @EnumeratedValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static PropertyFeature fromDbValue(String value) {
        for (PropertyFeature f : values()) {
            if (f.dbValue.equals(value)) return f;
        }
        throw new IllegalArgumentException("Unknown property_feature: " + value);
    }
}
