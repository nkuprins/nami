package com.app.backend.mapper;

import com.app.backend.dto.PropertyItemDto;
import com.app.backend.dto.PropertyListItemDto;
import com.app.backend.entity.*;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyFeature;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.app.backend.testutil.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;

class PropertyMapperTest {

    private final PropertyMapper mapper = new PropertyMapper();

    @Test
    void toDto_mapsAllScalarFields() {
        Listing l = listingWithPhotos(user());

        PropertyItemDto dto = mapper.toDto(l);

        assertThat(dto.id()).isEqualTo(l.getId());
        assertThat(dto.propertyId()).isEqualTo(l.getProperty().getId());
        assertThat(dto.ownerId()).isEqualTo(l.getOwner().getId());
        assertThat(dto.type()).isEqualTo(ListingType.BUY);
        assertThat(dto.propertyKind()).isEqualTo(PropertyCategory.APARTMENT);
        assertThat(dto.translations().get("lv").title()).isEqualTo(l.getTranslations().get("lv").getTitle());
        assertThat(dto.translations().get("en").title()).isEqualTo(l.getTranslations().get("en").getTitle());
        assertThat(dto.translations().get("lv").description()).isEqualTo(l.getTranslations().get("lv").getDescription());
        assertThat(dto.translations().get("en").description()).isEqualTo(l.getTranslations().get("en").getDescription());
        assertThat(dto.price().amount()).isEqualByComparingTo(l.getPrice());
        assertThat(dto.details().rooms()).isEqualTo(l.getProperty().getRooms());
        assertThat(dto.details().m2()).isEqualByComparingTo(l.getProperty().getM2());
        assertThat(dto.location().district()).isEqualTo(l.getProperty().getDistrictSlug());
        assertThat(dto.location().city()).isEqualTo(l.getProperty().getCitySlug());
    }

    @Test
    void toDto_mapsPhotos_andFeatures() {
        Listing l = listingWithPhotos(user());
        l.getProperty().setFeatures(Set.of(PropertyFeature.BALCONY, PropertyFeature.ELEVATOR));

        PropertyItemDto dto = mapper.toDto(l);

        assertThat(dto.media().photos()).hasSize(2);
        assertThat(dto.media().photos()).containsExactly(
                "https://cdn.test.local/uploads/photo1.jpg",
                "https://cdn.test.local/uploads/photo2.jpg"
        );
        assertThat(dto.features()).containsExactlyInAnyOrder(PropertyFeature.BALCONY, PropertyFeature.ELEVATOR);
    }

    @Test
    void toDto_returnsNullPhones_whenEmpty() {
        Listing l = listing(user());

        PropertyItemDto dto = mapper.toDto(l);

        assertThat(dto.phones()).isNull();
    }

    @Test
    void toDto_mapsCompletion_whenPresent() {
        Listing l = listing(user());
        l.setCompletion(com.app.backend.enums.PropertyCompletion.READY);

        PropertyItemDto dto = mapper.toDto(l);

        assertThat(dto.completion()).isEqualTo(PropertyCompletion.READY);
    }

    @Test
    void toListDto_mapsFirstPhotoOnly() {
        Listing l = listingWithPhotos(user());

        PropertyListItemDto dto = mapper.toListDto(l);

        assertThat(dto.photo()).isEqualTo("https://cdn.test.local/uploads/photo1.jpg");
    }

    @Test
    void toListDto_returnsNullPhoto_whenNoPhotos() {
        Listing l = listing(user());

        PropertyListItemDto dto = mapper.toListDto(l);

        assertThat(dto.photo()).isNull();
    }

    @Test
    void toListDto_mapsMultilingualTitles() {
        Listing l = listing(user());

        PropertyListItemDto dto = mapper.toListDto(l);

        assertThat(dto.translations().get("lv").title()).isEqualTo("Testa īpašums");
        assertThat(dto.translations().get("en").title()).isEqualTo("Test Property");
        assertThat(dto.translations().get("ru")).isNull();
    }

    @Test
    void toListDto_returnsNullCompletion_whenAbsent() {
        Listing l = listing(user());

        PropertyListItemDto dto = mapper.toListDto(l);

        assertThat(dto.completion()).isNull();
    }
}
