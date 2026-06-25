package com.app.backend.mapper;

import com.app.backend.dto.CoordsDto;
import com.app.backend.dto.PropertyItemDto;
import com.app.backend.dto.PropertyListItemDto;
import com.app.backend.entity.Property;
import com.app.backend.entity.PropertyPhone;
import com.app.backend.entity.PropertyPhoto;
import com.app.backend.enums.PropertyFeature;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PropertyMapper {

    public PropertyItemDto toDto(Property p) {
        List<String> photos = p.getPhotos().stream().map(PropertyPhoto::getUrl).toList();
        List<String> features = p.getFeatures().stream().map(PropertyFeature::getDbValue).toList();
        List<String> phones = p.getPhones().stream().map(PropertyPhone::getPhone).toList();
        return new PropertyItemDto(
                p.getId(),
                p.getOwner().getId(),
                p.getListingType().getDbValue(),
                p.getPropertyCategory().getDbValue(),
                p.getTitleLv(),
                p.getTitleEn(),
                p.getDescriptionLv(),
                p.getDescriptionEn(),
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
                phones.isEmpty() ? null : phones,
                p.getVideoUrl(),
                p.getPostedAt(),
                p.getCompletion() != null ? p.getCompletion().getDbValue() : null
        );
    }

    public PropertyListItemDto toListDto(Property p) {
        List<String> features = p.getFeatures().stream().map(PropertyFeature::getDbValue).toList();
        String photo = p.getPhotos().stream()
                .findFirst()
                .map(PropertyPhoto::getUrl)
                .orElse(null);
        return new PropertyListItemDto(
                p.getId(),
                p.getOwner().getId(),
                p.getListingType().getDbValue(),
                p.getPropertyCategory().getDbValue(),
                p.getTitleLv(),
                p.getTitleEn(),
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
                photo,
                p.getPostedAt(),
                p.getCompletion() != null ? p.getCompletion().getDbValue() : null
        );
    }
}
