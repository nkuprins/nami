package com.app.backend.controller;

import com.app.backend.IntegrationTestBase;
import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.security.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static com.app.backend.testutil.TestData.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PropertyControllerIntegrationTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

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

    private Property saveProperty() {
        Property p = property(owner);
        p.setId(null);
        p.setPostedAt(null);
        p.setUpdatedAt(null);
        return propertyRepository.save(p);
    }

    @Test
    void list_returns200_withPaginatedResults() throws Exception {
        saveProperty();

        mockMvc.perform(get("/api/properties").param("type", "buy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    void list_filtersByType() throws Exception {
        saveProperty();

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
    void getById_returns200_forActiveProperty() throws Exception {
        Property saved = saveProperty();

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
                .andExpect(jsonPath("$.titleLv").value("Testa dzīvoklis"));
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
        Property saved = saveProperty();

        mockMvc.perform(put("/api/properties/{id}", saved.getId())
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePropertyRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titleEn").value("Updated Apartment"));
    }

    @Test
    void update_returns403_whenNotOwner() throws Exception {
        Property saved = saveProperty();

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
        Property saved = saveProperty();

        mockMvc.perform(delete("/api/properties/{id}", saved.getId())
                        .cookie(ownerCookie))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns403_whenNotOwner() throws Exception {
        Property saved = saveProperty();

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
    void mine_returns200_withOwnerProperties() throws Exception {
        saveProperty();

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
