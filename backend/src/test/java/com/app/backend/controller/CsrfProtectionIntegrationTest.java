package com.app.backend.controller;

import com.app.backend.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies CSRF protection is actually enforced. Uses a MockMvc built without
 * the base class's default token so each request controls its own CSRF state —
 * the shared {@code mockMvc} attaches a valid token to everything.
 */
class CsrfProtectionIntegrationTest extends IntegrationTestBase {

    @Autowired
    private WebApplicationContext context;

    private MockMvc noCsrf;

    @BeforeEach
    void buildMockMvc() {
        noCsrf = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void protectedMutation_withoutCsrfToken_isForbidden() throws Exception {
        noCsrf.perform(post("/api/saved/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedMutation_withCsrfToken_passesCsrfAndReachesAuth() throws Exception {
        // Same request, now with a valid token: CSRF passes, so the unauthenticated
        // request is rejected at authentication (401) rather than by CSRF (403).
        noCsrf.perform(post("/api/saved/{id}", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exemptAuthEndpoint_withoutCsrfToken_reachesService() throws Exception {
        // Login is CSRF-exempt: a tokenless call must reach AuthService (401 for
        // unknown credentials) instead of being blocked by CSRF (a bodyless 403).
        noCsrf.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@test.com\",\"password\":\"WrongPassword12345\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}
