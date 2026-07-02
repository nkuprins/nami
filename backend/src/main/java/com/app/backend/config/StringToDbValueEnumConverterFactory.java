package com.app.backend.config;

import com.app.backend.enums.DbValueEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

/**
 * Binds query-parameter strings to {@link DbValueEnum} constants by their
 * {@code dbValue} (e.g. {@code heat_pump}) instead of Spring's default
 * {@link Enum#name()} matching. A bad value throws {@link IllegalArgumentException},
 * which Spring wraps into a 400 during data binding.
 */
public class StringToDbValueEnumConverterFactory implements ConverterFactory<String, DbValueEnum> {

    @Override
    public <T extends DbValueEnum> Converter<String, T> getConverter(Class<T> targetType) {
        return source -> {
            for (T candidate : targetType.getEnumConstants()) {
                if (candidate.getDbValue().equals(source)) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException(
                    "Unknown " + targetType.getSimpleName() + ": " + source);
        };
    }
}
