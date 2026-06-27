package com.app.backend.spec;

import com.app.backend.IntegrationTestBase;
import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.enums.*;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.testutil.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PropertySpecIntegrationTest extends IntegrationTestBase {

    @Autowired private PropertyRepository propertyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User owner;

    @BeforeEach
    void setUpOwner() {
        User u = new User();
        u.setName("Spec Owner");
        u.setEmail("spec@test.com");
        u.setPasswordHash(passwordEncoder.encode("TestPassword12345"));
        u.setEmailVerified(true);
        owner = userRepository.save(u);
    }

    private Property save(Property p) {
        p.setId(null);
        p.setPostedAt(null);
        p.setUpdatedAt(null);
        return propertyRepository.save(p);
    }

    private static Specification<Property> spec(
            ListingType type, List<String> loc,
            BigDecimal priceMin, BigDecimal priceMax,
            List<Integer> rooms,
            BigDecimal m2Min, BigDecimal m2Max,
            Integer floorMin, Integer floorMax,
            Boolean notGround, Boolean notTop,
            Integer yearMin, Integer yearMax,
            List<PropertyFeature> features,
            PropertyCompletion completion) {
        Map<String, List<String>> locByCity = new HashMap<>();
        if (loc != null) {
            for (String entry : loc) {
                int colon = entry.indexOf(':');
                if (colon <= 0 || colon >= entry.length() - 1) continue;
                locByCity.computeIfAbsent(entry.substring(0, colon), k -> new ArrayList<>())
                        .add(entry.substring(colon + 1));
            }
        }
        PropertySearchCriteria criteria = PropertySearchCriteria.builder()
                .listingType(type)
                .locByCity(locByCity)
                .priceMin(priceMin).priceMax(priceMax)
                .rooms(rooms)
                .m2Min(m2Min).m2Max(m2Max)
                .floorMin(floorMin == null ? null : floorMin.shortValue())
                .floorMax(floorMax == null ? null : floorMax.shortValue())
                .notGround(notGround).notTop(notTop)
                .yearMin(yearMin == null ? null : yearMin.shortValue())
                .yearMax(yearMax == null ? null : yearMax.shortValue())
                .features(features)
                .completion(completion)
                .build();
        return PropertySpec.build(criteria);
    }

    private static Specification<Property> buySpec() {
        return spec(ListingType.BUY, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void returnsOnlyActiveProperties() {
        Property active = TestData.property(owner);
        active.setStatus(PropertyStatus.ACTIVE);
        save(active);

        Property inactive = TestData.property(owner);
        inactive.setStatus(PropertyStatus.INACTIVE);
        save(inactive);

        List<Property> results = propertyRepository.findAll(buySpec());
        assertThat(results).hasSize(1).allMatch(p -> p.getStatus() == PropertyStatus.ACTIVE);
    }

    @Test
    void filtersByListingType() {
        save(TestData.property(owner)); // BUY

        Property rent = TestData.property(owner);
        rent.setListingType(ListingType.RENT);
        save(rent);

        List<Property> buyResults = propertyRepository.findAll(
                spec(ListingType.BUY, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
        List<Property> rentResults = propertyRepository.findAll(
                spec(ListingType.RENT, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        assertThat(buyResults).hasSize(1).allMatch(p -> p.getListingType() == ListingType.BUY);
        assertThat(rentResults).hasSize(1).allMatch(p -> p.getListingType() == ListingType.RENT);
    }

    @Test
    void filtersByLocation() {
        Property riga = TestData.property(owner);
        riga.setCitySlug("riga");
        riga.setDistrictSlug("centre");
        save(riga);

        Property jurmala = TestData.property(owner);
        jurmala.setCitySlug("jurmala");
        jurmala.setDistrictSlug("majori");
        save(jurmala);

        List<Property> results = propertyRepository.findAll(
                spec(ListingType.BUY, List.of("riga:centre"), null, null, null, null, null, null, null, null, null, null, null, null, null));

        assertThat(results).hasSize(1)
                .allMatch(p -> p.getCitySlug().equals("riga") && p.getDistrictSlug().equals("centre"));
    }

    @Test
    void filtersByPriceRange() {
        Property cheap = TestData.property(owner);
        cheap.setPrice(new BigDecimal("50000.00"));
        save(cheap);

        Property mid = TestData.property(owner);
        mid.setPrice(new BigDecimal("150000.00"));
        save(mid);

        Property expensive = TestData.property(owner);
        expensive.setPrice(new BigDecimal("500000.00"));
        save(expensive);

        List<Property> results = propertyRepository.findAll(
                spec(ListingType.BUY, null, new BigDecimal("100000"), new BigDecimal("200000"), null, null, null, null, null, null, null, null, null, null, null));

        assertThat(results).hasSize(1).allMatch(p -> p.getPrice().compareTo(new BigDecimal("150000")) == 0);
    }

    @Test
    void filtersByRooms_includingSevenPlus() {
        Property twoRoom = TestData.property(owner);
        twoRoom.setRooms((short) 2);
        save(twoRoom);

        Property fiveRoom = TestData.property(owner);
        fiveRoom.setRooms((short) 5);
        save(fiveRoom);

        Property sevenRoom = TestData.property(owner);
        sevenRoom.setRooms((short) 7);
        save(sevenRoom);

        Property nineRoom = TestData.property(owner);
        nineRoom.setRooms((short) 9);
        save(nineRoom);

        // rooms=[2, 7] → exact 2-room + all 7+ room properties (5-room excluded)
        List<Property> results = propertyRepository.findAll(
                spec(ListingType.BUY, null, null, null, List.of(2, 7), null, null, null, null, null, null, null, null, null, null));

        assertThat(results).hasSize(3).allMatch(p -> p.getRooms() != 5);
    }

    @Test
    void filtersByFeatures_requiresAll() {
        Property withBoth = TestData.property(owner);
        withBoth.setFeatures(Set.of(PropertyFeature.BALCONY, PropertyFeature.ELEVATOR));
        save(withBoth);

        Property withOnly = TestData.property(owner);
        withOnly.setFeatures(Set.of(PropertyFeature.BALCONY));
        save(withOnly);

        List<Property> results = propertyRepository.findAll(
                spec(ListingType.BUY, null, null, null, null, null, null, null, null, null, null, null, null,
                        List.of(PropertyFeature.BALCONY, PropertyFeature.ELEVATOR), null));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getFeatures()).contains(PropertyFeature.BALCONY, PropertyFeature.ELEVATOR);
    }

    @Test
    void filtersByCompletion_forNewProject() {
        Property ready = TestData.property(owner);
        ready.setListingType(ListingType.NEW_PROJECT);
        ready.setCompletion(PropertyCompletion.READY);
        ready.setYearBuilt(null);
        save(ready);

        Property notReady = TestData.property(owner);
        notReady.setListingType(ListingType.NEW_PROJECT);
        notReady.setCompletion(PropertyCompletion.NOT_READY);
        notReady.setYearBuilt(null);
        save(notReady);

        List<Property> results = propertyRepository.findAll(
                spec(ListingType.NEW_PROJECT, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        PropertyCompletion.READY));

        assertThat(results).hasSize(1).allMatch(p -> p.getCompletion() == PropertyCompletion.READY);
    }

    @Test
    void filtersByYearRange() {
        Property old = TestData.property(owner);
        old.setYearBuilt((short) 1990);
        save(old);

        Property modern = TestData.property(owner);
        modern.setYearBuilt((short) 2020);
        save(modern);

        List<Property> results = propertyRepository.findAll(
                spec(ListingType.BUY, null, null, null, null, null, null, null, null, null, null, 2010, 2025, null, null));

        assertThat(results).hasSize(1).allMatch(p -> p.getYearBuilt() == 2020);
    }

    @Test
    void combinedFilters() {
        Property match = TestData.property(owner);
        match.setCitySlug("riga");
        match.setDistrictSlug("centre");
        match.setPrice(new BigDecimal("200000.00"));
        match.setRooms((short) 3);
        save(match);

        Property noMatch = TestData.property(owner);
        noMatch.setCitySlug("riga");
        noMatch.setDistrictSlug("centre");
        noMatch.setPrice(new BigDecimal("50000.00"));
        noMatch.setRooms((short) 1);
        save(noMatch);

        List<Property> results = propertyRepository.findAll(
                spec(ListingType.BUY, List.of("riga:centre"), new BigDecimal("100000"), null,
                        List.of(3), null, null, null, null, null, null, null, null, null, null));

        assertThat(results).hasSize(1).allMatch(p -> p.getRooms() == 3);
    }
}
