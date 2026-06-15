package com.app.backend.converter;

import com.app.backend.enums.PropertyStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PropertyStatusConverter implements AttributeConverter<PropertyStatus, String> {

    @Override
    public String convertToDatabaseColumn(PropertyStatus attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public PropertyStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PropertyStatus.fromDbValue(dbData);
    }
}
