package com.app.backend.mapper;

import com.app.backend.dto.CoordsDto;
import com.app.backend.dto.PropertyItemDto;
import com.app.backend.dto.PropertyListItemDto;
import com.app.backend.entity.Property;
import com.app.backend.entity.PropertyPhone;
import com.app.backend.entity.PropertyPhoto;
import com.app.backend.entity.PropertyPlan;
import com.app.backend.entity.PropertyTranslation;
import com.app.backend.enums.PropertyFeature;
import com.app.backend.enums.SupportedLocale;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class PropertyMapper {

    public PropertyItemDto toDto(Property p) {
        List<String> photos = p.getPhotos().stream().map(PropertyPhoto::getUrl).toList();
        List<String> plans = p.getPlans().stream().map(PropertyPlan::getUrl).toList();
        List<String> features = p.getFeatures().isEmpty() ? null : p.getFeatures().stream().map(PropertyFeature::getDbValue).toList();
        List<String> phones = p.getPhones().stream().map(PropertyPhone::getPhone).toList();
        Map<String, PropertyTranslation> tr = p.getTranslations();
        return PropertyItemDto.builder()
                .id(p.getId())
                .ownerId(p.getOwner().getId())
                .type(p.getListingType().getDbValue())
                .propertyKind(p.getPropertyCategory().getDbValue())
                .titleLv(title(tr, SupportedLocale.LV.code))
                .titleEn(title(tr, SupportedLocale.EN.code))
                .titleRu(title(tr, SupportedLocale.RU.code))
                .descriptionLv(desc(tr, SupportedLocale.LV.code))
                .descriptionEn(desc(tr, SupportedLocale.EN.code))
                .descriptionRu(desc(tr, SupportedLocale.RU.code))
                .price(p.getPrice())
                .rooms(p.getRooms())
                .m2(p.getM2())
                .landM2(p.getLandM2())
                .floor(p.getFloor())
                .totalFloors(p.getTotalFloors())
                .yearBuilt(p.getYearBuilt())
                .features(features)
                .district(p.getDistrictSlug())
                .city(p.getCitySlug())
                .address(p.getAddress())
                .coords(new CoordsDto(p.getLat(), p.getLng()))
                .photos(photos)
                .plans(plans.isEmpty() ? null : plans)
                .phones(phones.isEmpty() ? null : phones)
                .videoUrl(p.getVideoUrl())
                .postedAt(p.getPostedAt())
                .completion(p.getCompletion() != null ? p.getCompletion().getDbValue() : null)
                .build();
    }

    public PropertyListItemDto toListDto(Property p) {
        List<String> features = p.getFeatures().isEmpty() ? null : p.getFeatures().stream().map(PropertyFeature::getDbValue).toList();
        String photo = p.getPhotos().isEmpty() ? null : p.getPhotos().get(0).getUrl();
        Map<String, PropertyTranslation> tr = p.getTranslations();
        return PropertyListItemDto.builder()
                .id(p.getId())
                .ownerId(p.getOwner().getId())
                .type(p.getListingType().getDbValue())
                .propertyKind(p.getPropertyCategory().getDbValue())
                .titleLv(title(tr, SupportedLocale.LV.code))
                .titleEn(title(tr, SupportedLocale.EN.code))
                .titleRu(title(tr, SupportedLocale.RU.code))
                .price(p.getPrice())
                .rooms(p.getRooms())
                .m2(p.getM2())
                .landM2(p.getLandM2())
                .floor(p.getFloor())
                .totalFloors(p.getTotalFloors())
                .yearBuilt(p.getYearBuilt())
                .features(features)
                .district(p.getDistrictSlug())
                .city(p.getCitySlug())
                .address(p.getAddress())
                .photo(photo)
                .postedAt(p.getPostedAt())
                .completion(p.getCompletion() != null ? p.getCompletion().getDbValue() : null)
                .build();
    }

    private static String title(Map<String, PropertyTranslation> tr, String locale) {
        return field(tr, locale, PropertyTranslation::getTitle);
    }

    private static String desc(Map<String, PropertyTranslation> tr, String locale) {
        return field(tr, locale, PropertyTranslation::getDescription);
    }

    private static String field(Map<String, PropertyTranslation> tr, String locale,
                                Function<PropertyTranslation, String> fn) {
        PropertyTranslation t = tr.get(locale);
        return t != null ? fn.apply(t) : null;
    }
}
