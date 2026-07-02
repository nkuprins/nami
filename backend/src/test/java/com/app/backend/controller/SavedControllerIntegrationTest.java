package com.app.backend.controller;

import com.app.backend.IntegrationTestBase;
import com.app.backend.entity.Listing;
import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static com.app.backend.testutil.TestData.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SavedControllerIntegrationTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User user;
    private Cookie userCookie;

    @BeforeEach
    void setUpUser() {
        User u = new User();
        u.setName("Saver");
        u.setEmail("saver@test.com");
        u.setPasswordHash(passwordEncoder.encode("TestPassword12345"));
        u.setEmailVerified(true);
        user = userRepository.save(u);
        userCookie = authTestHelper.accessTokenCookie(user.getId());
    }

    private Listing saveListing() {
        Listing l = listing(user);
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
    void getSaved_returns200_withEmptyList_whenNoneSaved() throws Exception {
        mockMvc.perform(get("/api/saved").cookie(userCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getSaved_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/saved"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void save_returns204_whenListingExists() throws Exception {
        Listing listing = saveListing();

        mockMvc.perform(post("/api/saved/{listingId}", listing.getId())
                        .cookie(userCookie))
                .andExpect(status().isNoContent());
    }

    @Test
    void save_returns404_whenListingNotFound() throws Exception {
        mockMvc.perform(post("/api/saved/{listingId}", UUID.randomUUID())
                        .cookie(userCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void unsave_returns204() throws Exception {
        Listing listing = saveListing();

        mockMvc.perform(post("/api/saved/{listingId}", listing.getId())
                        .cookie(userCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/saved/{listingId}", listing.getId())
                        .cookie(userCookie))
                .andExpect(status().isNoContent());
    }
}
