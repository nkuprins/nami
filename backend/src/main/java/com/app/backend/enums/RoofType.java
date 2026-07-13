package com.app.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoofType implements DbValueEnum {
    BITUMEN("bitumen"),
    ETERNIT("eternit"),
    PVC("pvc"),
    ROLL_MATERIAL("roll_material"),
    STEEL("steel"),
    STONE("stone"),
    TILE("tile"),
    WHITE_TIN("white_tin"),
    ZINC_PLATE("zinc_plate");

    @EnumeratedValue
    @JsonValue
    private final String dbValue;

    @JsonCreator
    public static RoofType fromDbValue(String value) {
        for (RoofType v : values()) {
            if (v.dbValue.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown roof_type: " + value);
    }
}
