package com.app.backend.converter;

import com.app.backend.enums.PropertyCompletion;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PropertyCompletionConverter implements AttributeConverter<PropertyCompletion, String> {

    @Override
    public String convertToDatabaseColumn(PropertyCompletion attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public PropertyCompletion convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PropertyCompletion.fromDbValue(dbData);
    }
}
