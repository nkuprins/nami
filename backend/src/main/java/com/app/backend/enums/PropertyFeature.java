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
    BASEMENT("basement"),
    RENOVATED("renovated"),
    AIR_CONDITIONING("air_conditioning"),
    TERRACE("terrace"),
    SAUNA("sauna"),
    FIREPLACE("fireplace"),
    UNDERFLOOR_HEATING("underfloor_heating"),
    INDIVIDUAL_METERS("individual_meters"),
    STORAGE_ROOM("storage_room"),
    WALK_IN_CLOSET("walk_in_closet"),
    POOL("pool"),
    BATHTUB("bathtub"),
    SHOWER("shower"),
    WASHING_MACHINE("washing_machine"),
    BOILER("boiler"),
    GLAZED_BALCONY("glazed_balcony"),
    FRENCH_BALCONY("french_balcony"),
    LOGGIA("loggia");

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
