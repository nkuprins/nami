package com.app.backend.enums;

import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PropertyFeature {
    BALCONY("balcony"),
    PARKING("parking"),
    ELEVATOR("elevator"),
    FURNISHED("furnished"),
    PETS("pets"),
    NEW_BUILDING("new_building");

    @EnumeratedValue
    private final String dbValue;

    public static PropertyFeature fromDbValue(String value) {
        for (PropertyFeature f : values()) {
            if (f.dbValue.equals(value)) return f;
        }
        throw new IllegalArgumentException("Unknown property_feature: " + value);
    }
}
