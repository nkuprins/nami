package com.app.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnergyClass implements DbValueEnum {
    A("A"),
    B("B"),
    C("C"),
    D("D"),
    E("E"),
    F("F"),
    G("G");

    @EnumeratedValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static EnergyClass fromDbValue(String value) {
        for (EnergyClass e : values()) {
            if (e.dbValue.equals(value)) return e;
        }
        throw new IllegalArgumentException("Unknown energy_class: " + value);
    }
}
