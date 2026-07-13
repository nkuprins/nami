package com.app.backend.testutil;

import com.app.backend.dto.property.request.AddListingRequest;
import com.app.backend.dto.property.model.CoordsDto;
import com.app.backend.dto.property.request.CreatePropertyRequest;
import com.app.backend.dto.property.model.LocalizedText;
import com.app.backend.dto.property.model.Location;
import com.app.backend.dto.property.model.Media;
import com.app.backend.dto.property.model.PhoneContact;
import com.app.backend.dto.property.model.Price;
import com.app.backend.dto.property.model.PropertyDetails;
import com.app.backend.dto.property.request.UpdateListingRequest;
import com.app.backend.dto.property.request.UpdatePropertyRequest;
import com.app.backend.dto.auth.LoginRequest;
import com.app.backend.dto.auth.RegisterRequest;
import com.app.backend.entity.*;
import com.app.backend.enums.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TestData {

    public static final String RAW_PASSWORD = "TestPassword12345";
    public static final String BCRYPT_HASH = "$2a$04$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ01234";

    private TestData() {}

    public static User user() {
        return user("test@example.com");
    }

    public static User user(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test User");
        user.setEmail(email);
        user.setPasswordHash(BCRYPT_HASH);
        user.setEmailVerified(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        return user;
    }

    public static User unverifiedUser(String email) {
        User user = user(email);
        user.setEmailVerified(false);
        return user;
    }

    public static Listing listing(User owner) {
        Property p = new Property();
        p.setId(UUID.randomUUID());
        p.setOwner(owner);
        p.setDistrictSlug("centre");
        p.setCitySlug("riga");
        p.setAddress("Test Street 1");
        p.setLat(56.9496);
        p.setLng(24.1052);
        p.setUpdatedAt(OffsetDateTime.now());

        Listing l = new Listing();
        l.setId(UUID.randomUUID());
        l.setProperty(p);
        l.setOwner(owner);
        l.setListingType(ListingType.BUY);
        l.setStatus(PropertyStatus.ACTIVE);
        l.setPrice(new BigDecimal("150000.00"));
        l.setVatIncluded(false);
        l.setPropertyCategory(PropertyCategory.APARTMENT);
        l.setRooms((short) 3);
        l.setM2(new BigDecimal("75.00"));
        l.setFloor((short) 3);
        l.setTotalFloors((short) 5);
        l.setYearBuilt((short) 2010);
        l.setFeatures(new HashSet<>(Set.of(PropertyFeature.BALCONY, PropertyFeature.PARKING)));
        l.setPhotos(new ArrayList<>());
        l.setPlans(new ArrayList<>());
        l.setPostedAt(OffsetDateTime.now());
        l.setUpdatedAt(OffsetDateTime.now());
        l.setExpiresAt(OffsetDateTime.now().plusMonths(3));

        Map<String, ListingTranslation> translations = new HashMap<>();
        for (String[] t : new String[][]{
                {"lv", "Testa īpašums", "Apraksts latviski"},
                {"en", "Test Property", "Description in English"}
        }) {
            ListingTranslation lt = new ListingTranslation();
            lt.setListing(l);
            lt.setLocale(t[0]);
            lt.setTitle(t[1]);
            lt.setDescription(t[2]);
            translations.put(t[0], lt);
        }
        l.setTranslations(translations);
        l.setPhones(new ArrayList<>());
        return l;
    }

    public static Listing listingWithPhotos(User owner) {
        Listing l = listing(owner);
        l.getPhotos().add("https://cdn.test.local/uploads/photo1.jpg");
        l.getPhotos().add("https://cdn.test.local/uploads/photo2.jpg");
        return l;
    }

    public static RefreshToken refreshToken(User user) {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setTokenHash("hashed-token-value");
        token.setExpiresAt(OffsetDateTime.now().plusDays(7));
        token.setRevoked(false);
        return token;
    }

    public static EmailVerificationToken emailVerificationToken(User user) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setTokenHash("hashed-verification-token");
        token.setExpiresAt(OffsetDateTime.now().plusHours(24));
        token.setUsed(false);
        return token;
    }

    public static PasswordResetToken passwordResetToken(User user) {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setTokenHash("hashed-reset-token");
        token.setExpiresAt(OffsetDateTime.now().plusHours(1));
        token.setUsed(false);
        return token;
    }

    public static RegisterRequest registerRequest() {
        return registerRequest("newuser@example.com");
    }

    public static RegisterRequest registerRequest(String email) {
        return new RegisterRequest("New User", email, RAW_PASSWORD);
    }

    public static LoginRequest loginRequest(String email) {
        return new LoginRequest(email, RAW_PASSWORD);
    }

    public static CreatePropertyRequest createPropertyRequest() {
        return CreatePropertyRequest.builder()
                .type(ListingType.BUY)
                .propertyKind(PropertyCategory.APARTMENT)
                .price(new Price(new BigDecimal("200000.00"), null))
                .details(PropertyDetails.builder()
                        .rooms((short) 2)
                        .bedrooms((short) 2)
                        .bathrooms((short) 1)
                        .bathroomLayout(BathroomLayout.COMBINED)
                        .m2(new BigDecimal("65.00"))
                        .floor((short) 4)
                        .totalFloors((short) 9)
                        .yearBuilt((short) 2020)
                        .heating(HeatingType.GAS)
                        .energyClass(EnergyClass.B)
                        .maintenanceCost(new BigDecimal("120.00"))
                        .build())
                .translations(Map.of(
                        "lv", new LocalizedText("Testa dzīvoklis", "Apraksts latviski"),
                        "en", new LocalizedText("Test Apartment", "Description in English")))
                .location(new Location("centre", "riga", "Main Street 10", null, null,
                        new CoordsDto(56.9496, 24.1052)))
                .features(List.of(PropertyFeature.BALCONY, PropertyFeature.PARKING))
                .media(Media.builder().photos(List.of("https://cdn.test.local/uploads/p1.jpg")).build())
                .phones(List.of(new PhoneContact("+37120000000", "Test User", "test@example.com")))
                .durationMonths(3)
                .build();
    }

    public static UpdateListingRequest updateListingRequest() {
        return UpdateListingRequest.builder()
                .type(ListingType.BUY)
                .propertyKind(PropertyCategory.APARTMENT)
                .price(new Price(new BigDecimal("210000.00"), null))
                .details(PropertyDetails.builder()
                        .rooms((short) 3)
                        .bedrooms((short) 2)
                        .bathrooms((short) 1)
                        .bathroomLayout(BathroomLayout.SEPARATE)
                        .m2(new BigDecimal("70.00"))
                        .floor((short) 5)
                        .totalFloors((short) 9)
                        .yearBuilt((short) 2021)
                        .heating(HeatingType.CENTRAL)
                        .energyClass(EnergyClass.C)
                        .maintenanceCost(new BigDecimal("95.00"))
                        .build())
                .translations(Map.of(
                        "lv", new LocalizedText("Atjaunots dzīvoklis", "Atjaunots apraksts"),
                        "en", new LocalizedText("Updated Apartment", "Updated description")))
                .features(List.of(PropertyFeature.ELEVATOR, PropertyFeature.FURNISHED))
                .media(Media.builder().photos(List.of("https://cdn.test.local/uploads/p1.jpg")).build())
                .phones(List.of(new PhoneContact("+37120000001", "Test User", "test@example.com")))
                .build();
    }

    public static UpdatePropertyRequest updatePropertyRequest() {
        return UpdatePropertyRequest.builder()
                .location(new Location("centre", "riga", "Main Street 10", null, null,
                        new CoordsDto(56.9496, 24.1052)))
                .build();
    }

    public static AddListingRequest addListingRequest() {
        return AddListingRequest.builder()
                .type(ListingType.RENT)
                .propertyKind(PropertyCategory.APARTMENT)
                .price(new Price(new BigDecimal("650.00"), null))
                .details(PropertyDetails.builder()
                        .rooms((short) 3)
                        .bedrooms((short) 2)
                        .bathrooms((short) 1)
                        .bathroomLayout(BathroomLayout.COMBINED)
                        .m2(new BigDecimal("65.00"))
                        .floor((short) 4)
                        .totalFloors((short) 9)
                        .yearBuilt((short) 2020)
                        .heating(HeatingType.GAS)
                        .energyClass(EnergyClass.B)
                        .maintenanceCost(new BigDecimal("120.00"))
                        .build())
                .translations(Map.of(
                        "lv", new LocalizedText("Testa dzīvoklis īrei", "Apraksts latviski"),
                        "en", new LocalizedText("Test Apartment for rent", "Description in English")))
                .features(List.of(PropertyFeature.BALCONY))
                .media(Media.builder().photos(List.of("https://cdn.test.local/uploads/p1.jpg")).build())
                .phones(List.of(new PhoneContact("+37120000002", "Test User", "test@example.com")))
                .durationMonths(3)
                .build();
    }
}
