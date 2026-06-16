package com.app.backend.mapper;

import com.app.backend.dto.CoordsDto;
import com.app.backend.dto.PropertyItemDto;
import com.app.backend.entity.Property;
import com.app.backend.entity.PropertyPhoto;
import com.app.backend.enums.PropertyFeature;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PropertyMapper {

    public PropertyItemDto toDto(Property p) {
        List<String> photos = p.getPhotos().stream().map(PropertyPhoto::getUrl).toList();
        List<String> features = p.getFeatures().stream().map(PropertyFeature::getDbValue).toList();
        return new PropertyItemDto(
                p.getId(),
                p.getListingType().getDbValue(),
                p.getPropertyCategory().getDbValue(),
                p.getTitle(),
                p.getDescription(),
                p.getPrice(),
                p.getRooms(),
                p.getM2(),
                p.getLandM2(),
                p.getFloor(),
                p.getTotalFloors(),
                p.getYearBuilt(),
                features,
                p.getDistrictSlug(),
                p.getCitySlug(),
                p.getAddress(),
                new CoordsDto(p.getLat(), p.getLng()),
                photos,
                p.getPostedAt(),
                p.getCompletion() != null ? p.getCompletion().getDbValue() : null
        );
    }
}
