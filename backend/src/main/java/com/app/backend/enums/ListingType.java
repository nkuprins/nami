package com.app.backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ListingType {
    BUY("buy"),
    RENT("rent"),
    NEW_PROJECT("new-project");

    private final String dbValue;

    public static ListingType fromDbValue(String value) {
        for (ListingType t : values()) {
            if (t.dbValue.equals(value)) return t;
        }
        throw new IllegalArgumentException("Unknown listing_type: " + value);
    }
}
