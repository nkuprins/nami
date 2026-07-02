package com.app.backend.controller;

import com.app.backend.IntegrationTestBase;
import com.app.backend.entity.Listing;
import com.app.backend.entity.Property;
import com.app.backend.entity.User;
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
        Listing l = listing(owner);
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
                        .content(objectMapper.writeValueAsString(updatePropertyRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translations.en.title").value("Updated Apartment"));
    }

    @Test
    void update_returns403_whenNotOwner() throws Exception {
        Listing saved = saveListing();

        User other = new User();
        other.setName("Other");
        other.setEmail("other@test.com");
        other.setPasswordHash(passwordEncoder.encode("TestPassword12345"));
        other.setEmailVerified(true);
        other = userRepository.save(other);

        Cookie otherCookie = authTestHelper.accessTokenCookie(other.getId());

        mockMvc.perform(put("/api/properties/{id}", saved.getId())
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
    void delete_keepsProperty_forLaterRelisting() throws Exception {
        Listing saved = saveListing();
        UUID propertyId = saved.getProperty().getId();

        mockMvc.perform(delete("/api/properties/{id}", saved.getId())
                        .cookie(ownerCookie))
                .andExpect(status().isNoContent());

        assertThat(propertyRepository.findById(propertyId)).isPresent();
        assertThat(listingRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void delete_returns403_whenNotOwner() throws Exception {
        Listing saved = saveListing();

        User other = new User();
        other.setName("Other2");
        other.setEmail("other2@test.com");
        other.setPasswordHash(passwordEncoder.encode("TestPassword12345"));
        other.setEmailVerified(true);
        other = userRepository.save(other);

        mockMvc.perform(delete("/api/properties/{id}", saved.getId())
                        .cookie(authTestHelper.accessTokenCookie(other.getId())))
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
    void addListing_returns409_whenSameListingTypeAlreadyExists() throws Exception {
        Listing saved = saveListing(); // BUY listing

        mockMvc.perform(post("/api/properties/{propertyId}/listings", saved.getProperty().getId())
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addListingRequest().toBuilder()
                                .type(ListingType.BUY)
                                .build())))
                .andExpect(status().isConflict());
    }

    @Test
    void addListing_returns403_whenNotOwner() throws Exception {
        Listing saved = saveListing();

        User other = new User();
        other.setName("Other3");
        other.setEmail("other3@test.com");
        other.setPasswordHash(passwordEncoder.encode("TestPassword12345"));
        other.setEmailVerified(true);
        other = userRepository.save(other);

        mockMvc.perform(post("/api/properties/{propertyId}/listings", saved.getProperty().getId())
                        .cookie(authTestHelper.accessTokenCookie(other.getId()))
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
    void deleteProperty_returns204_andCascadesAllListings() throws Exception {
        Listing saved = saveListing();
        UUID propertyId = saved.getProperty().getId();

        mockMvc.perform(delete("/api/properties/{propertyId}/listings", propertyId)
                        .cookie(ownerCookie))
                .andExpect(status().isNoContent());

        // The listing is removed via the DB's ON DELETE CASCADE, not a JPA-level cascade,
        // so the persistence context doesn't know about it — clear the 1st-level cache first.
        entityManager.clear();
        assertThat(propertyRepository.findById(propertyId)).isEmpty();
        assertThat(listingRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void deleteProperty_returns403_whenNotOwner() throws Exception {
        Listing saved = saveListing();

        User other = new User();
        other.setName("Other4");
        other.setEmail("other4@test.com");
        other.setPasswordHash(passwordEncoder.encode("TestPassword12345"));
        other.setEmailVerified(true);
        other = userRepository.save(other);

        mockMvc.perform(delete("/api/properties/{propertyId}/listings", saved.getProperty().getId())
                        .cookie(authTestHelper.accessTokenCookie(other.getId())))
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
}
