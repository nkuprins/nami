package com.app.backend.mapper;

import com.app.backend.dto.property.model.LocalizedText;
import com.app.backend.dto.property.request.CreatePropertyRequest;
import com.app.backend.dto.property.response.PropertyItemDto;
import com.app.backend.dto.property.response.PropertyListItemDto;
import com.app.backend.entity.*;
import com.app.backend.enums.Communication;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.ParkingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyExtra;
import com.app.backend.enums.PropertyFeature;
import com.app.backend.enums.RoofType;
import com.app.backend.enums.SecurityFeature;
import com.app.backend.enums.StoveType;
import com.app.backend.enums.VentilationSystem;
import org.junit.jupiter.api.Test;

import java.util.List;
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
        assertThat(dto.details().rooms()).isEqualTo(l.getRooms());
        assertThat(dto.details().m2()).isEqualByComparingTo(l.getM2());
        assertThat(dto.location().district()).isEqualTo(l.getProperty().getDistrictSlug());
        assertThat(dto.location().city()).isEqualTo(l.getProperty().getCitySlug());
    }

    @Test
    void toDto_mapsPhotos_andFeatures() {
        Listing l = listingWithPhotos(user());
        l.setFeatures(Set.of(PropertyFeature.BALCONY, PropertyFeature.ELEVATOR));

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
        l.setCompletion(PropertyCompletion.READY);

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

    @Test
    void toDto_withNullLocale_returnsAllLocales_andAvailableLocales() {
        Listing l = listing(user()); // has lv + en, no ru

        PropertyItemDto dto = mapper.toDto(l, null);

        assertThat(dto.translations()).containsOnlyKeys("lv", "en");
        assertThat(dto.availableLocales()).containsExactly("lv", "en");
    }

    @Test
    void toDto_withLocale_returnsOnlyThatLocale() {
        Listing l = listing(user()); // has lv + en

        PropertyItemDto dto = mapper.toDto(l, "en");

        assertThat(dto.translations()).containsOnlyKeys("en");
        assertThat(dto.translations().get("en").title()).isEqualTo("Test Property");
        assertThat(dto.translations().get("en").description()).isNotBlank();
        assertThat(dto.availableLocales()).containsExactly("lv", "en");
    }

    @Test
    void toDto_withMissingLocale_fallsBackButKeepsRequestedKey() {
        Listing l = listing(user()); // no ru → ru falls back to lv

        PropertyItemDto dto = mapper.toDto(l, "ru");

        assertThat(dto.translations()).containsOnlyKeys("ru");
        assertThat(dto.translations().get("ru").title()).isEqualTo("Testa īpašums");
        assertThat(dto.availableLocales()).containsExactly("lv", "en");
    }

    @Test
    void toTranslation_returnsRequestedLocale_whenPresent() {
        Listing l = listing(user());

        LocalizedText t = mapper.toTranslation(l, "en");

        assertThat(t.title()).isEqualTo("Test Property");
    }

    @Test
    void toTranslation_fallsBack_whenLocaleMissing() {
        Listing l = listing(user()); // no ru → lv per fallback order

        LocalizedText t = mapper.toTranslation(l, "ru");

        assertThat(t.title()).isEqualTo("Testa īpašums");
    }

    @Test
    void toDto_mapsPhysicalFromListing() {
        Listing l = listing(user());
        l.setFloor((short) 2);

        PropertyItemDto dto = mapper.toDto(l);

        assertThat(dto.details().floor()).isEqualTo((short) 2);
        assertThat(dto.details().rooms()).isEqualTo(l.getRooms());
    }

    @Test
    void toDto_mapsRoof_andAttributeSets() {
        Listing l = listing(user());
        l.setRoof(RoofType.TILE);
        l.setVentilationSystems(Set.of(VentilationSystem.AIR_CONDITIONER));
        l.setCommunications(Set.of(Communication.INTERNET, Communication.CABLE_TV));
        l.setStove(Set.of(StoveType.GAS_STOVE));
        l.setSecurity(Set.of(SecurityFeature.GUARD));
        l.setExtras(Set.of(PropertyExtra.FURNITURE));
        l.setParking(Set.of(ParkingType.FREE_PARKING));

        PropertyItemDto dto = mapper.toDto(l);

        assertThat(dto.details().roof()).isEqualTo(RoofType.TILE);
        assertThat(dto.ventilationSystems()).containsExactly(VentilationSystem.AIR_CONDITIONER);
        assertThat(dto.communications()).containsExactlyInAnyOrder(Communication.CABLE_TV, Communication.INTERNET);
        assertThat(dto.stove()).containsExactly(StoveType.GAS_STOVE);
        assertThat(dto.security()).containsExactly(SecurityFeature.GUARD);
        assertThat(dto.extras()).containsExactly(PropertyExtra.FURNITURE);
        assertThat(dto.parking()).containsExactly(ParkingType.FREE_PARKING);
    }

    @Test
    void toDto_returnsNullAttributeSets_whenEmpty() {
        Listing l = listing(user());

        PropertyItemDto dto = mapper.toDto(l);

        assertThat(dto.ventilationSystems()).isNull();
        assertThat(dto.parking()).isNull();
        assertThat(dto.details().roof()).isNull();
    }

    @Test
    void applyListingContent_writesRoof_andAttributeSets() {
        Listing l = new Listing();
        CreatePropertyRequest base = createPropertyRequest();
        CreatePropertyRequest req = base.toBuilder()
                .details(base.details().toBuilder().roof(RoofType.STEEL).build())
                .ventilationSystems(List.of(VentilationSystem.AIR_CONDITIONER))
                .communications(List.of(Communication.INTERNET, Communication.CABLE_TV))
                .stove(List.of(StoveType.GAS_STOVE))
                .security(List.of(SecurityFeature.GUARD))
                .extras(List.of(PropertyExtra.FURNITURE))
                .parking(List.of(ParkingType.FREE_PARKING))
                .build();

        mapper.applyListingContent(l, req);

        assertThat(l.getRoof()).isEqualTo(RoofType.STEEL);
        assertThat(l.getVentilationSystems()).containsExactly(VentilationSystem.AIR_CONDITIONER);
        assertThat(l.getCommunications()).containsExactlyInAnyOrder(Communication.CABLE_TV, Communication.INTERNET);
        assertThat(l.getStove()).containsExactly(StoveType.GAS_STOVE);
        assertThat(l.getSecurity()).containsExactly(SecurityFeature.GUARD);
        assertThat(l.getExtras()).containsExactly(PropertyExtra.FURNITURE);
        assertThat(l.getParking()).containsExactly(ParkingType.FREE_PARKING);
    }
}
