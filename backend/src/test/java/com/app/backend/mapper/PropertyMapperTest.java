package com.app.backend.mapper;

import com.app.backend.dto.PropertyItemDto;
import com.app.backend.dto.PropertyListItemDto;
import com.app.backend.entity.*;
import com.app.backend.enums.PropertyFeature;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.app.backend.testutil.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;

class PropertyMapperTest {

    private final PropertyMapper mapper = new PropertyMapper();

    @Test
    void toDto_mapsAllScalarFields() {
        Property p = propertyWithPhotos(user());

        PropertyItemDto dto = mapper.toDto(p);

        assertThat(dto.id()).isEqualTo(p.getId());
        assertThat(dto.ownerId()).isEqualTo(p.getOwner().getId());
        assertThat(dto.type()).isEqualTo("buy");
        assertThat(dto.propertyKind()).isEqualTo("apartment");
        assertThat(dto.titleLv()).isEqualTo(p.getTranslations().get("lv").getTitle());
        assertThat(dto.titleEn()).isEqualTo(p.getTranslations().get("en").getTitle());
        assertThat(dto.descriptionLv()).isEqualTo(p.getTranslations().get("lv").getDescription());
        assertThat(dto.descriptionEn()).isEqualTo(p.getTranslations().get("en").getDescription());
        assertThat(dto.price()).isEqualByComparingTo(p.getPrice());
        assertThat(dto.rooms()).isEqualTo(p.getRooms());
        assertThat(dto.m2()).isEqualByComparingTo(p.getM2());
        assertThat(dto.district()).isEqualTo(p.getDistrictSlug());
        assertThat(dto.city()).isEqualTo(p.getCitySlug());
    }

    @Test
    void toDto_mapsPhotos_andFeatures() {
        Property p = propertyWithPhotos(user());
        p.setFeatures(Set.of(PropertyFeature.BALCONY, PropertyFeature.ELEVATOR));

        PropertyItemDto dto = mapper.toDto(p);

        assertThat(dto.photos()).hasSize(2);
        assertThat(dto.photos()).containsExactly(
                "https://cdn.test.local/uploads/photo1.jpg",
                "https://cdn.test.local/uploads/photo2.jpg"
        );
        assertThat(dto.features()).containsExactlyInAnyOrder("balcony", "elevator");
    }

    @Test
    void toDto_returnsNullPhones_whenEmpty() {
        Property p = property(user());

        PropertyItemDto dto = mapper.toDto(p);

        assertThat(dto.phones()).isNull();
    }

    @Test
    void toDto_mapsCompletion_whenPresent() {
        Property p = property(user());
        p.setCompletion(com.app.backend.enums.PropertyCompletion.READY);

        PropertyItemDto dto = mapper.toDto(p);

        assertThat(dto.completion()).isEqualTo("ready");
    }

    @Test
    void toListDto_mapsFirstPhotoOnly() {
        Property p = propertyWithPhotos(user());

        PropertyListItemDto dto = mapper.toListDto(p);

        assertThat(dto.photo()).isEqualTo("https://cdn.test.local/uploads/photo1.jpg");
    }

    @Test
    void toListDto_returnsNullPhoto_whenNoPhotos() {
        Property p = property(user());

        PropertyListItemDto dto = mapper.toListDto(p);

        assertThat(dto.photo()).isNull();
    }

    @Test
    void toListDto_mapsMultilingualTitles() {
        Property p = property(user());

        PropertyListItemDto dto = mapper.toListDto(p);

        assertThat(dto.titleLv()).isEqualTo("Testa īpašums");
        assertThat(dto.titleEn()).isEqualTo("Test Property");
        assertThat(dto.titleRu()).isNull();
    }

    @Test
    void toListDto_returnsNullCompletion_whenAbsent() {
        Property p = property(user());

        PropertyListItemDto dto = mapper.toListDto(p);

        assertThat(dto.completion()).isNull();
    }
}
