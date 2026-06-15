package com.app.backend.converter;

import com.app.backend.enums.PropertyCategory;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PropertyCategoryConverter implements AttributeConverter<PropertyCategory, String> {

    @Override
    public String convertToDatabaseColumn(PropertyCategory attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public PropertyCategory convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PropertyCategory.fromDbValue(dbData);
    }
}
