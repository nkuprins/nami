package com.app.backend.controller;

import com.app.backend.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uses a MockMvc without the base class's default {@code csrf()} token so the
 * real {@code CookieCsrfTokenRepository} runs — the shared token postprocessor
 * bypasses the repository and never writes the XSRF-TOKEN cookie this endpoint
 * exists to plant.
 */
class CsrfControllerIntegrationTest extends IntegrationTestBase {

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
    void csrf_isPublic_returnsNoContent_andPlantsToken() throws Exception {
        noCsrf.perform(get("/api/csrf"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }
}
