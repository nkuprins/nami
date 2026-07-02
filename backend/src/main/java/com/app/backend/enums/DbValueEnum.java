package com.app.backend.enums;

/**
 * Enums whose wire/DB representation is a lowercase {@code dbValue} rather than
 * {@link Enum#name()}. Implemented so a single Spring {@code ConverterFactory}
 * can bind query-parameter strings (e.g. {@code ?heating=heat_pump}) to the
 * matching constant. JSON (de)serialization is handled separately via
 * {@code @JsonValue}/{@code @JsonCreator} on each enum.
 */
public interface DbValueEnum {
    String getDbValue();
}
