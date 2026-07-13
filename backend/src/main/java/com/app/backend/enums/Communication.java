package com.app.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Communication implements DbValueEnum {
    CABLE_TV("cable_tv"),
    INTERNET("internet"),
    TELEPHONE("telephone"),
    DIGITAL_TV("digital_tv");

    @EnumeratedValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static Communication fromDbValue(String value) {
        for (Communication v : values()) {
            if (v.dbValue.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown communication_type: " + value);
    }
}
