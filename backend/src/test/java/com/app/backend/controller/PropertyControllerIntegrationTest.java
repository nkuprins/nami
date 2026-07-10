package com.app.backend.controller;

import com.app.backend.IntegrationTestBase;
import com.app.backend.dto.property.model.Media;
import com.app.backend.dto.property.model.PropertyDetails;
import com.app.backend.entity.Listing;
import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.enums.BathroomLayout;
import com.app.backend.enums.ListingType;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.security.JwtService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.app.backend.testutil.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PropertyControllerIntegrationTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private EntityManager entityManager;

    private User owner;
    private Cookie ownerCookie;

    @BeforeEach
    void setUpUser() {
        User user = new User();
        user.setName("Owner");
        user.setEmail("owner@test.com");
        user.setPasswordHash(passwordEncoder.encode("TestPassword12345"));
        user.setEmailVerified(true);
        owner = userRepository.save(user);
        ownerCookie = authTestHelper.accessTokenCookie(owner.getId());
    }

    private Listing saveListing() {
        return persist(listing(owner));
    }

    private Listing saveListingWithPhotos() {
        return persist(listingWithPhotos(owner));
    }

    private Listing persist(Listing l) {
        l.setId(null);
        l.getProperty().setId(null);
        l.getProperty().setUpdatedAt(null);
        Property savedProperty = propertyRepository.save(l.getProperty());
        l.setProperty(savedProperty);
        l.setPostedAt(null);
        l.setUpdatedAt(null);
        return listingRepository.save(l);
    }

    @Test
    void list_returns200_withPaginatedResults() throws Exception {
        saveListing();

        mockMvc.perform(get("/api/properties").param("type", "buy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    void list_filtersByType() throws Exception {
        saveListing();

        mockMvc.perform(get("/api/properties").param("type", "rent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void list_returns400_whenTypeMissing() throws Exception {
        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns400_whenTypeInvalid() throws Exception {
        mockMvc.perform(get("/api/properties").param("type", "not_a_type"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returns400_whenLocMalformed() throws Exception {
        mockMvc.perform(get("/api/properties").param("type", "buy").param("loc", "riga"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_returns200_forActiveProperty() throws Exception {
        Listing saved = saveListing();

        mockMvc.perform(get("/api/properties/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()));
    }

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/properties/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_withoutLocale_returnsAllLocales() throws Exception {
        Listing saved = saveListing(); // lv + en

        mockMvc.perform(get("/api/properties/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translations.lv.title").value("Testa īpašums"))
                .andExpect(jsonPath("$.translations.en.title").value("Test Property"))
                .andExpect(jsonPath("$.availableLocales",
                        org.hamcrest.Matchers.contains("lv", "en")));
    }

    @Test
    void getById_withLocale_returnsOnlyThatLocale() throws Exception {
        Listing saved = saveListing(); // lv + en

        mockMvc.perform(get("/api/properties/{id}", saved.getId()).param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translations.en.title").value("Test Property"))
                .andExpect(jsonPath("$.translations.en.description").isNotEmpty())
                .andExpect(jsonPath("$.translations.lv").doesNotExist())
                .andExpect(jsonPath("$.availableLocales",
                        org.hamcrest.Matchers.contains("lv", "en")));
    }

    @Test
    void getTranslation_returnsRequestedLocale() throws Exception {
        Listing saved = saveListing();

        mockMvc.perform(get("/api/properties/{id}/translations/{locale}", saved.getId(), "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Property"))
                .andExpect(jsonPath("$.description").isNotEmpty());
    }

    @Test
    void getTranslation_fallsBack_whenLocaleMissing() throws Exception {
        Listing saved = saveListing(); // no ru → falls back to lv

        mockMvc.perform(get("/api/properties/{id}/translations/{locale}", saved.getId(), "ru"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Testa īpašums"));
    }

    @Test
    void getTranslation_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/properties/{id}/translations/{locale}", UUID.randomUUID(), "lv"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returns201_whenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/properties")
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPropertyRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.translations.lv.title").value("Testa dzīvoklis"));
    }

    @Test
    void create_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPropertyRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_returns200_whenOwner() throws Exception {
        Listing saved = saveListing();

        mockMvc.perform(put("/api/properties/{id}", saved.getId())
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateListingRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translations.en.title").value("Updated Apartment"))
                .andExpect(jsonPath("$.details.m2").value(70.00));
    }

    @Test
    void update_returns403_whenNotOwner() throws Exception {
        Listing saved = saveListing();

        Cookie otherCookie = authTestHelper.accessTokenCookie(saveOther("other@test.com").getId());

        mockMvc.perform(put("/api/properties/{id}", saved.getId())
                        .cookie(otherCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateListingRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProperty_returns200_whenOwner() throws Exception {
        Listing saved = saveListing();

        mockMvc.perform(get("/api/properties/{propertyId}/property", saved.getProperty().getId())
                        .cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getProperty().getId().toString()))
                .andExpect(jsonPath("$.location.address").value("Test Street 1"));
    }

    @Test
    void getProperty_returns401_whenNotAuthenticated() throws Exception {
        Listing saved = saveListing();

        mockMvc.perform(get("/api/properties/{propertyId}/property", saved.getProperty().getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProperty_returns403_whenNotOwner() throws Exception {
        Listing saved = saveListing();

        Cookie otherCookie = authTestHelper.accessTokenCookie(saveOther("other-get@test.com").getId());

        mockMvc.perform(get("/api/properties/{propertyId}/property", saved.getProperty().getId())
                        .cookie(otherCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProperty_returns200_whenOwner() throws Exception {
        Listing saved = saveListing();

        mockMvc.perform(put("/api/properties/{propertyId}/property", saved.getProperty().getId())
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePropertyRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.address").value("Main Street 10"));
    }

    @Test
    void updateProperty_returns403_whenNotOwner() throws Exception {
        Listing saved = saveListing();

        Cookie otherCookie = authTestHelper.accessTokenCookie(saveOther("other-update@test.com").getId());

        mockMvc.perform(put("/api/properties/{propertyId}/property", saved.getProperty().getId())
                        .cookie(otherCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePropertyRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_returns204_whenOwner() throws Exception {
        Listing saved = saveListing();

        mockMvc.perform(delete("/api/properties/{id}", saved.getId())
                        .cookie(ownerCookie))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_removesEmptyProperty_whenLastListingDeleted() throws Exception {
        Listing saved = saveListing();
        UUID propertyId = saved.getProperty().getId();

        mockMvc.perform(delete("/api/properties/{id}", saved.getId())
                        .cookie(ownerCookie))
                .andExpect(status().isNoContent());

        // The delete joins this test's transaction, so flush it to the DB (triggering the
        // ON DELETE CASCADE) before clearing the 1st-level cache and re-reading.
        entityManager.flush();
        entityManager.clear();
        assertThat(propertyRepository.findById(propertyId)).isEmpty();
        assertThat(listingRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void delete_keepsProperty_whenOtherListingsRemain() throws Exception {
        Listing saved = saveListing(); // BUY
        UUID propertyId = saved.getProperty().getId();

        // A second (rent) listing at the same address.
        mockMvc.perform(post("/api/properties/{propertyId}/listings", propertyId)
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addListingRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/properties/{id}", saved.getId())
                        .cookie(ownerCookie))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();
        assertThat(propertyRepository.findById(propertyId)).isPresent();
        assertThat(listingRepository.findByPropertyId(propertyId)).hasSize(1);
    }

    @Test
    void delete_returns403_whenNotOwner() throws Exception {
        Listing saved = saveListing();

        mockMvc.perform(delete("/api/properties/{id}", saved.getId())
                        .cookie(authTestHelper.accessTokenCookie(saveOther("other2@test.com").getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void addListing_returns201_andCreatesSecondListingForSameProperty() throws Exception {
        Listing saved = saveListing(); // BUY listing
        UUID propertyId = saved.getProperty().getId();

        mockMvc.perform(post("/api/properties/{propertyId}/listings", propertyId)
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addListingRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.propertyId").value(propertyId.toString()))
                .andExpect(jsonPath("$.type").value("rent"));

        assertThat(listingRepository.findByOwner(owner)).hasSize(2);
    }

    @Test
    void addListing_allowsSameListingTypeAtOneAddress() throws Exception {
        Listing saved = saveListing(); // BUY listing

        mockMvc.perform(post("/api/properties/{propertyId}/listings", saved.getProperty().getId())
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addListingRequest().toBuilder()
                                .type(ListingType.BUY)
                                .build())))
                .andExpect(status().isCreated());

        assertThat(listingRepository.findByOwner(owner)).hasSize(2);
    }

    @Test
    void addListing_returns403_whenNotOwner() throws Exception {
        Listing saved = saveListing();

        mockMvc.perform(post("/api/properties/{propertyId}/listings", saved.getProperty().getId())
                        .cookie(authTestHelper.accessTokenCookie(saveOther("other3@test.com").getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addListingRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void addListing_returns404_whenPropertyNotFound() throws Exception {
        mockMvc.perform(post("/api/properties/{propertyId}/listings", UUID.randomUUID())
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addListingRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void addListing_createsListingWithItsOwnScopeAndPhotos() throws Exception {
        Listing saved = saveListingWithPhotos(); // BUY: floor 3/5, m2 75
        UUID propertyId = saved.getProperty().getId();

        var req = addListingRequest().toBuilder()
                .details(PropertyDetails.builder()
                        .rooms((short) 3)
                        .bedrooms((short) 1)
                        .bathrooms((short) 1)
                        .bathroomLayout(BathroomLayout.COMBINED)
                        .m2(new BigDecimal("40.00"))
                        .floor((short) 2)
                        .totalFloors((short) 5)
                        .build())
                .media(Media.builder().photos(List.of("https://cdn.test.local/uploads/photo2.jpg")).build())
                .build();

        mockMvc.perform(post("/api/properties/{propertyId}/listings", propertyId)
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.details.floor").value(2))
                .andExpect(jsonPath("$.details.m2").value(40.00))
                .andExpect(jsonPath("$.details.rooms").value(3))
                .andExpect(jsonPath("$.media.photos.length()").value(1))
                .andExpect(jsonPath("$.media.photos[0]").value("https://cdn.test.local/uploads/photo2.jpg"));
    }

    @Test
    void addListing_returns400_whenFloorExceedsTotalFloors() throws Exception {
        Listing saved = saveListing();
        UUID propertyId = saved.getProperty().getId();

        var req = addListingRequest().toBuilder()
                .details(PropertyDetails.builder()
                        .rooms((short) 3)
                        .m2(new BigDecimal("50.00"))
                        .floor((short) 10)
                        .totalFloors((short) 5)
                        .build())
                .build();

        mockMvc.perform(post("/api/properties/{propertyId}/listings", propertyId)
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addListing_returns400_whenMediaUrlNotOnCdn() throws Exception {
        Listing saved = saveListing();
        UUID propertyId = saved.getProperty().getId();

        var req = addListingRequest().toBuilder()
                .media(Media.builder().photos(List.of("https://evil.example.com/x.jpg")).build())
                .build();

        mockMvc.perform(post("/api/properties/{propertyId}/listings", propertyId)
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteProperty_returns204_andCascadesAllListings() throws Exception {
        Listing saved = saveListing();
        UUID propertyId = saved.getProperty().getId();

        mockMvc.perform(delete("/api/properties/{propertyId}/listings", propertyId)
                        .cookie(ownerCookie))
                .andExpect(status().isNoContent());

        // The listing is removed via the DB's ON DELETE CASCADE, not a JPA-level cascade, so the
        // persistence context doesn't know about it. The delete joins this test's transaction, so
        // flush it to the DB (triggering the cascade) before clearing the 1st-level cache.
        entityManager.flush();
        entityManager.clear();
        assertThat(propertyRepository.findById(propertyId)).isEmpty();
        assertThat(listingRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void deleteProperty_returns403_whenNotOwner() throws Exception {
        Listing saved = saveListing();

        mockMvc.perform(delete("/api/properties/{propertyId}/listings", saved.getProperty().getId())
                        .cookie(authTestHelper.accessTokenCookie(saveOther("other4@test.com").getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void mine_returns200_withOwnerProperties() throws Exception {
        saveListing();

        mockMvc.perform(get("/api/properties/mine")
                        .cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ownerId").value(owner.getId().toString()));
    }

    @Test
    void mine_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/properties/mine"))
                .andExpect(status().isUnauthorized());
    }

    private User saveOther(String email) {
        User other = new User();
        other.setName("Other");
        other.setEmail(email);
        other.setPasswordHash(passwordEncoder.encode("TestPassword12345"));
        other.setEmailVerified(true);
        return userRepository.save(other);
    }
}
