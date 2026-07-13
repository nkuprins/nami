package com.app.backend.enums;

import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    USER("user"),
    ADMIN("admin");

    @EnumeratedValue
    private final String dbValue;

    public static UserRole fromDbValue(String value) {
        for (UserRole r : values()) {
            if (r.dbValue.equals(value)) return r;
        }
        throw new IllegalArgumentException("Unknown user_role: " + value);
    }
}
