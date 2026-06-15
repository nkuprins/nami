package com.app.backend.converter;

import com.app.backend.enums.PropertyFeature;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PropertyFeatureConverter implements AttributeConverter<PropertyFeature, String> {

    @Override
    public String convertToDatabaseColumn(PropertyFeature attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public PropertyFeature convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PropertyFeature.fromDbValue(dbData);
    }
}
