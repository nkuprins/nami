package com.app.backend.controller;

import com.app.backend.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uses a MockMvc without the base class's default {@code csrf()} token so the
 * real {@code CookieCsrfTokenRepository} runs and actually writes the
 * {@code XSRF-TOKEN} cookie this endpoint exists to plant.
 *
 * <p>The base class's {@code .with(csrf())} default request goes further than
 * bypassing the cookie: Spring Security Test's post-processor reflectively swaps
 * the shared {@link CsrfFilter}'s token repository for a cookie-less
 * {@code TestCsrfTokenRepository}, and that mutation persists on the singleton
 * filter across the shared application context. So whether this test saw a real
 * repository used to depend on whether a base-MockMvc test ran first — an
 * order-dependent flake. We restore a real repository before each run.
 */
class CsrfControllerIntegrationTest extends IntegrationTestBase {

    @Autowired private WebApplicationContext context;
    @Autowired private SecurityFilterChain securityFilterChain;

    private MockMvc noCsrf;

    @BeforeEach
    void buildMockMvc() {
        noCsrf = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        restoreRealCsrfRepository();
    }

    /** Undo any prior test's swap of the shared CsrfFilter's cookie-less test repository. */
    private void restoreRealCsrfRepository() {
        securityFilterChain.getFilters().stream()
                .filter(CsrfFilter.class::isInstance)
                .forEach(filter -> ReflectionTestUtils.setField(
                        filter, "tokenRepository", CookieCsrfTokenRepository.withHttpOnlyFalse()));
    }

    @Test
    void csrf_isPublic_returnsNoContent_andPlantsToken() throws Exception {
        noCsrf.perform(get("/api/csrf"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }
}
