package com.app.backend.spec;

import com.app.backend.IntegrationTestBase;
import com.app.backend.entity.Listing;
import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.enums.*;
import com.app.backend.repository.ListingRepository;
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
    @Autowired private ListingRepository listingRepository;
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

    private Listing save(Listing l) {
        l.setId(null);
        l.getProperty().setId(null);
        l.getProperty().setUpdatedAt(null);
        Property savedProperty = propertyRepository.save(l.getProperty());
        l.setProperty(savedProperty);
        l.setPostedAt(null);
        l.setUpdatedAt(null);
        return listingRepository.save(l);
    }

    private static Specification<Listing> spec(
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

    private static Specification<Listing> buySpec() {
        return spec(ListingType.BUY, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void returnsOnlyActiveProperties() {
        Listing active = TestData.listing(owner);
        active.setStatus(PropertyStatus.ACTIVE);
        save(active);

        Listing inactive = TestData.listing(owner);
        inactive.setStatus(PropertyStatus.INACTIVE);
        save(inactive);

        List<Listing> results = listingRepository.findAll(buySpec());
        assertThat(results).hasSize(1).allMatch(l -> l.getStatus() == PropertyStatus.ACTIVE);
    }

    @Test
    void filtersByListingType() {
        save(TestData.listing(owner)); // BUY

        Listing rent = TestData.listing(owner);
        rent.setListingType(ListingType.RENT);
        save(rent);

        List<Listing> buyResults = listingRepository.findAll(
                spec(ListingType.BUY, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
        List<Listing> rentResults = listingRepository.findAll(
                spec(ListingType.RENT, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        assertThat(buyResults).hasSize(1).allMatch(l -> l.getListingType() == ListingType.BUY);
        assertThat(rentResults).hasSize(1).allMatch(l -> l.getListingType() == ListingType.RENT);
    }

    @Test
    void filtersByLocation() {
        Listing riga = TestData.listing(owner);
        riga.getProperty().setCitySlug("riga");
        riga.getProperty().setDistrictSlug("centre");
        save(riga);

        Listing jurmala = TestData.listing(owner);
        jurmala.getProperty().setCitySlug("jurmala");
        jurmala.getProperty().setDistrictSlug("majori");
        save(jurmala);

        List<Listing> results = listingRepository.findAll(
                spec(ListingType.BUY, List.of("riga:centre"), null, null, null, null, null, null, null, null, null, null, null, null, null));

        assertThat(results).hasSize(1)
                .allMatch(l -> l.getProperty().getCitySlug().equals("riga")
                        && l.getProperty().getDistrictSlug().equals("centre"));
    }

    @Test
    void filtersByPriceRange() {
        Listing cheap = TestData.listing(owner);
        cheap.setPrice(new BigDecimal("50000.00"));
        save(cheap);

        Listing mid = TestData.listing(owner);
        mid.setPrice(new BigDecimal("150000.00"));
        save(mid);

        Listing expensive = TestData.listing(owner);
        expensive.setPrice(new BigDecimal("500000.00"));
        save(expensive);

        List<Listing> results = listingRepository.findAll(
                spec(ListingType.BUY, null, new BigDecimal("100000"), new BigDecimal("200000"), null, null, null, null, null, null, null, null, null, null, null));

        assertThat(results).hasSize(1).allMatch(l -> l.getPrice().compareTo(new BigDecimal("150000")) == 0);
    }

    @Test
    void filtersByRooms_includingSevenPlus() {
        Listing twoRoom = TestData.listing(owner);
        twoRoom.setRooms((short) 2);
        save(twoRoom);

        Listing fiveRoom = TestData.listing(owner);
        fiveRoom.setRooms((short) 5);
        save(fiveRoom);

        Listing sevenRoom = TestData.listing(owner);
        sevenRoom.setRooms((short) 7);
        save(sevenRoom);

        Listing nineRoom = TestData.listing(owner);
        nineRoom.setRooms((short) 9);
        save(nineRoom);

        // rooms=[2, 7] → exact 2-room + all 7+ room listings (5-room excluded)
        List<Listing> results = listingRepository.findAll(
                spec(ListingType.BUY, null, null, null, List.of(2, 7), null, null, null, null, null, null, null, null, null, null));

        assertThat(results).hasSize(3).allMatch(l -> l.getRooms() != 5);
    }

    @Test
    void filtersByFeatures_requiresAll() {
        Listing withBoth = TestData.listing(owner);
        withBoth.setFeatures(Set.of(PropertyFeature.BALCONY, PropertyFeature.ELEVATOR));
        save(withBoth);

        Listing withOnly = TestData.listing(owner);
        withOnly.setFeatures(Set.of(PropertyFeature.BALCONY));
        save(withOnly);

        List<Listing> results = listingRepository.findAll(
                spec(ListingType.BUY, null, null, null, null, null, null, null, null, null, null, null, null,
                        List.of(PropertyFeature.BALCONY, PropertyFeature.ELEVATOR), null));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getFeatures()).contains(PropertyFeature.BALCONY, PropertyFeature.ELEVATOR);
    }

    @Test
    void filtersByCompletion_forNewProject() {
        Listing ready = TestData.listing(owner);
        ready.setPropertyCategory(PropertyCategory.NEW_PROJECT);
        ready.setNewProjectKind(PropertyCategory.APARTMENT);
        ready.setCompletion(PropertyCompletion.READY);
        ready.setYearBuilt(null);
        save(ready);

        Listing notReady = TestData.listing(owner);
        notReady.setPropertyCategory(PropertyCategory.NEW_PROJECT);
        notReady.setNewProjectKind(PropertyCategory.APARTMENT);
        notReady.setCompletion(PropertyCompletion.NOT_READY);
        notReady.setYearBuilt(null);
        save(notReady);

        List<Listing> results = listingRepository.findAll(
                spec(ListingType.BUY, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        PropertyCompletion.READY));

        assertThat(results).hasSize(1).allMatch(l -> l.getCompletion() == PropertyCompletion.READY);
    }

    @Test
    void filtersByYearRange() {
        Listing old = TestData.listing(owner);
        old.setYearBuilt((short) 1990);
        save(old);

        Listing modern = TestData.listing(owner);
        modern.setYearBuilt((short) 2020);
        save(modern);

        List<Listing> results = listingRepository.findAll(
                spec(ListingType.BUY, null, null, null, null, null, null, null, null, null, null, 2010, 2025, null, null));

        assertThat(results).hasSize(1).allMatch(l -> l.getYearBuilt() == 2020);
    }

    @Test
    void combinedFilters() {
        Listing match = TestData.listing(owner);
        match.getProperty().setCitySlug("riga");
        match.getProperty().setDistrictSlug("centre");
        match.setPrice(new BigDecimal("200000.00"));
        match.setRooms((short) 3);
        save(match);

        Listing noMatch = TestData.listing(owner);
        noMatch.getProperty().setCitySlug("riga");
        noMatch.getProperty().setDistrictSlug("centre");
        noMatch.setPrice(new BigDecimal("50000.00"));
        noMatch.setRooms((short) 1);
        save(noMatch);

        List<Listing> results = listingRepository.findAll(
                spec(ListingType.BUY, List.of("riga:centre"), new BigDecimal("100000"), null,
                        List.of(3), null, null, null, null, null, null, null, null, null, null));

        assertThat(results).hasSize(1).allMatch(l -> l.getRooms() == 3);
    }

    private static Specification<Listing> buildSpec(PropertySearchCriteria.PropertySearchCriteriaBuilder builder) {
        return PropertySpec.build(builder.listingType(ListingType.BUY).build());
    }

    @Test
    void filtersByBedrooms_includingSevenPlus() {
        Listing two = TestData.listing(owner);
        two.setRooms((short) 5);
        two.setBedrooms((short) 2);
        save(two);

        Listing four = TestData.listing(owner);
        four.setRooms((short) 5);
        four.setBedrooms((short) 4);
        save(four);

        Listing eight = TestData.listing(owner);
        eight.setRooms((short) 9);
        eight.setBedrooms((short) 8);
        save(eight);

        // bedrooms=[2, 7] → exact 2 + all 7+ (4-bedroom excluded)
        List<Listing> results = listingRepository.findAll(
                buildSpec(PropertySearchCriteria.builder().bedrooms(List.of(2, 7))));

        assertThat(results).hasSize(2)
                .allMatch(l -> l.getBedrooms() == 2 || l.getBedrooms() >= 7);
    }

    @Test
    void filtersByBathrooms() {
        Listing one = TestData.listing(owner);
        one.setBathrooms((short) 1);
        save(one);

        Listing three = TestData.listing(owner);
        three.setBathrooms((short) 3);
        save(three);

        List<Listing> results = listingRepository.findAll(
                buildSpec(PropertySearchCriteria.builder().bathrooms(List.of(1))));

        assertThat(results).hasSize(1).allMatch(l -> l.getBathrooms() == 1);
    }

    @Test
    void filtersByHeating() {
        Listing gas = TestData.listing(owner);
        gas.setHeating(HeatingType.GAS);
        save(gas);

        Listing central = TestData.listing(owner);
        central.setHeating(HeatingType.CENTRAL);
        save(central);

        List<Listing> results = listingRepository.findAll(
                buildSpec(PropertySearchCriteria.builder().heating(List.of(HeatingType.GAS))));

        assertThat(results).hasSize(1).allMatch(l -> l.getHeating() == HeatingType.GAS);
    }

    @Test
    void filtersByEnergyClass() {
        Listing a = TestData.listing(owner);
        a.setEnergyClass(EnergyClass.A);
        save(a);

        Listing c = TestData.listing(owner);
        c.setEnergyClass(EnergyClass.C);
        save(c);

        Listing e = TestData.listing(owner);
        e.setEnergyClass(EnergyClass.E);
        save(e);

        List<Listing> results = listingRepository.findAll(
                buildSpec(PropertySearchCriteria.builder().energyClass(List.of(EnergyClass.A, EnergyClass.C))));

        assertThat(results).hasSize(2)
                .allMatch(l -> l.getEnergyClass() == EnergyClass.A || l.getEnergyClass() == EnergyClass.C);
    }

    @Test
    void filtersByFloor_acrossDistinctListingsAtOneScope() {
        Listing high = TestData.listing(owner); // floor 3
        save(high);

        Listing low = TestData.listing(owner);
        low.setFloor((short) 1);
        save(low);

        // floorMax=2 matches only the floor-1 listing.
        List<Listing> results = listingRepository.findAll(
                buildSpec(PropertySearchCriteria.builder().floorMax((short) 2)));

        assertThat(results).hasSize(1).allMatch(l -> l.getFloor() == 1);
    }

    @Test
    void filtersByLandArea_excludesListingsWithoutLand() {
        Listing smallPlot = TestData.listing(owner);
        smallPlot.setPropertyCategory(PropertyCategory.HOUSE);
        smallPlot.setLandM2(new BigDecimal("500.00"));
        save(smallPlot);

        Listing bigPlot = TestData.listing(owner);
        bigPlot.setPropertyCategory(PropertyCategory.HOUSE);
        bigPlot.setLandM2(new BigDecimal("1200.00"));
        save(bigPlot);

        Listing apartment = TestData.listing(owner); // landM2 == null
        save(apartment);

        // landM2Min=800 matches only the 1200 m² plot; the null-land apartment is excluded.
        List<Listing> results = listingRepository.findAll(
                buildSpec(PropertySearchCriteria.builder().landM2Min(new BigDecimal("800"))));

        assertThat(results).hasSize(1)
                .allMatch(l -> l.getLandM2().compareTo(new BigDecimal("1200")) == 0);
    }

    @Test
    void filtersByBathroomLayout() {
        Listing separate = TestData.listing(owner);
        separate.setBathroomLayout(BathroomLayout.SEPARATE);
        save(separate);

        Listing combined = TestData.listing(owner);
        combined.setBathroomLayout(BathroomLayout.COMBINED);
        save(combined);

        List<Listing> results = listingRepository.findAll(
                buildSpec(PropertySearchCriteria.builder().bathroomLayout(BathroomLayout.SEPARATE)));

        assertThat(results).hasSize(1).allMatch(l -> l.getBathroomLayout() == BathroomLayout.SEPARATE);
    }

    @Test
    void filtersByMaintenanceCostMax_excludesListingsWithoutCost() {
        Listing cheap = TestData.listing(owner);
        cheap.setMaintenanceCost(new BigDecimal("50.00"));
        save(cheap);

        Listing pricey = TestData.listing(owner);
        pricey.setMaintenanceCost(new BigDecimal("200.00"));
        save(pricey);

        Listing unknown = TestData.listing(owner); // maintenanceCost == null
        save(unknown);

        // maintenanceCostMax=100 matches only the €50 listing; the null-cost one is excluded.
        List<Listing> results = listingRepository.findAll(
                buildSpec(PropertySearchCriteria.builder().maintenanceCostMax(new BigDecimal("100"))));

        assertThat(results).hasSize(1)
                .allMatch(l -> l.getMaintenanceCost().compareTo(new BigDecimal("50")) == 0);
    }

    @Test
    void filtersByVatIncluded() {
        Listing withVat = TestData.listing(owner);
        withVat.setVatIncluded(true);
        save(withVat);

        Listing withoutVat = TestData.listing(owner); // vatIncluded == false by default
        save(withoutVat);

        List<Listing> results = listingRepository.findAll(
                buildSpec(PropertySearchCriteria.builder().vatIncluded(true)));

        assertThat(results).hasSize(1).allMatch(Listing::isVatIncluded);
    }

    @Test
    void filtersByRoof() {
        Listing tile = TestData.listing(owner);
        tile.setRoof(RoofType.TILE);
        save(tile);

        Listing steel = TestData.listing(owner);
        steel.setRoof(RoofType.STEEL);
        save(steel);

        List<Listing> results = listingRepository.findAll(
                buildSpec(PropertySearchCriteria.builder().roof(List.of(RoofType.TILE))));

        assertThat(results).hasSize(1).allMatch(l -> l.getRoof() == RoofType.TILE);
    }

    @Test
    void filtersByParking_matchesAny() {
        // ANY-of: a listing matches if it has at least one of the selected values, even
        // when it also carries an unselected value — the distinguishing case vs ALL-of.
        Listing freeAndPaid = TestData.listing(owner);
        freeAndPaid.setParking(Set.of(ParkingType.FREE_PARKING, ParkingType.PAID_PARKING));
        save(freeAndPaid);

        Listing underground = TestData.listing(owner);
        underground.setParking(Set.of(ParkingType.UNDERGROUND_PARKING));
        save(underground);

        Listing none = TestData.listing(owner); // no parking set
        save(none);

        List<Listing> results = listingRepository.findAll(buildSpec(PropertySearchCriteria.builder()
                .parking(List.of(ParkingType.FREE_PARKING, ParkingType.UNDERGROUND_PARKING))));

        assertThat(results).hasSize(2)
                .allMatch(l -> l.getParking().contains(ParkingType.FREE_PARKING)
                        || l.getParking().contains(ParkingType.UNDERGROUND_PARKING));
    }

    @Test
    void filtersByVentilationSystems_matchesAny() {
        Listing ac = TestData.listing(owner);
        ac.setVentilationSystems(Set.of(VentilationSystem.AIR_CONDITIONER));
        save(ac);

        Listing climate = TestData.listing(owner);
        climate.setVentilationSystems(Set.of(VentilationSystem.CLIMATE_CONTROL));
        save(climate);

        List<Listing> results = listingRepository.findAll(buildSpec(PropertySearchCriteria.builder()
                .ventilationSystems(List.of(VentilationSystem.AIR_CONDITIONER))));

        assertThat(results).hasSize(1)
                .allMatch(l -> l.getVentilationSystems().contains(VentilationSystem.AIR_CONDITIONER));
    }
}
