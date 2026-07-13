package com.app.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SecurityFeature implements DbValueEnum {
    LOCKING_ENTRANCE("locking_entrance"),
    GUARD("guard"),
    SECURITY_SYSTEM("security_system"),
    STEEL_DOOR("steel_door"),
    VIDEO_CAMERAS("video_cameras");

    @EnumeratedValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static SecurityFeature fromDbValue(String value) {
        for (SecurityFeature v : values()) {
            if (v.dbValue.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown security_feature: " + value);
    }
}
