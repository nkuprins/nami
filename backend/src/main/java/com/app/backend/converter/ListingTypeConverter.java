package com.app.backend.converter;

import com.app.backend.enums.ListingType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ListingTypeConverter implements AttributeConverter<ListingType, String> {

    @Override
    public String convertToDatabaseColumn(ListingType attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public ListingType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ListingType.fromDbValue(dbData);
    }
}
