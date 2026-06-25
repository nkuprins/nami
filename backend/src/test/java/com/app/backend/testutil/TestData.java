package com.app.backend.testutil;

import com.app.backend.dto.CoordsDto;
import com.app.backend.dto.CreatePropertyRequest;
import com.app.backend.dto.UpdatePropertyRequest;
import com.app.backend.dto.auth.LoginRequest;
import com.app.backend.dto.auth.RegisterRequest;
import com.app.backend.entity.*;
import com.app.backend.enums.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class TestData {

    public static final String RAW_PASSWORD = "TestPassword12345";
    public static final String BCRYPT_HASH = "$2a$04$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ012";

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

    public static Property property(User owner) {
        Property p = new Property();
        p.setId(UUID.randomUUID());
        p.setOwner(owner);
        p.setListingType(ListingType.BUY);
        p.setPropertyCategory(PropertyCategory.APARTMENT);
        p.setStatus(PropertyStatus.ACTIVE);
        p.setTitle("Test Property");
        p.setDescription("A test property description");
        p.setPrice(new BigDecimal("150000.00"));
        p.setRooms((short) 3);
        p.setM2(new BigDecimal("75.00"));
        p.setFloor((short) 3);
        p.setTotalFloors((short) 5);
        p.setYearBuilt((short) 2010);
        p.setDistrictSlug("centre");
        p.setCitySlug("riga");
        p.setAddress("Test Street 1");
        p.setLat(56.9496);
        p.setLng(24.1052);
        p.setPostedAt(OffsetDateTime.now());
        p.setUpdatedAt(OffsetDateTime.now());
        p.setFeatures(new HashSet<>(Set.of(PropertyFeature.BALCONY, PropertyFeature.PARKING)));
        p.setPhotos(new ArrayList<>());
        p.setPhones(new ArrayList<>());
        return p;
    }

    public static Property propertyWithPhotos(User owner) {
        Property p = property(owner);
        PropertyPhoto photo1 = new PropertyPhoto();
        photo1.setId(UUID.randomUUID());
        photo1.setProperty(p);
        photo1.setUrl("https://cdn.test.local/uploads/photo1.jpg");
        photo1.setPosition((short) 0);

        PropertyPhoto photo2 = new PropertyPhoto();
        photo2.setId(UUID.randomUUID());
        photo2.setProperty(p);
        photo2.setUrl("https://cdn.test.local/uploads/photo2.jpg");
        photo2.setPosition((short) 1);

        p.getPhotos().add(photo1);
        p.getPhotos().add(photo2);
        return p;
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
        return new CreatePropertyRequest(
                "buy", "apartment", "New Apartment", "Nice apartment in the center",
                new BigDecimal("200000.00"), (short) 2, new BigDecimal("65.00"),
                null, (short) 4, (short) 9, (short) 2020,
                List.of("balcony", "parking"),
                "centre", "riga", "Main Street 10",
                new CoordsDto(56.9496, 24.1052),
                List.of("https://cdn.test.local/uploads/p1.jpg"),
                List.of("+37120000000"),
                null, null
        );
    }

    public static UpdatePropertyRequest updatePropertyRequest() {
        return new UpdatePropertyRequest(
                "buy", "apartment", "Updated Apartment", "Updated description",
                new BigDecimal("210000.00"), (short) 3, new BigDecimal("70.00"),
                null, (short) 5, (short) 9, (short) 2021,
                List.of("elevator", "furnished"),
                List.of("+37120000001"),
                null, null
        );
    }
}
