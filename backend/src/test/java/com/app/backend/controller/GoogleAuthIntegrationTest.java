package com.app.backend.controller;

import com.app.backend.IntegrationTestBase;
import com.app.backend.dto.auth.GoogleLoginRequest;
import com.app.backend.entity.User;
import com.app.backend.repository.UserRepository;
import com.app.backend.security.GoogleTokenVerifier;
import com.app.backend.security.GoogleUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class GoogleAuthIntegrationTest extends IntegrationTestBase {

    @MockitoBean private GoogleTokenVerifier googleTokenVerifier;

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private MvcResult postGoogle(String credential) throws Exception {
        return mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleLoginRequest(credential))))
                .andReturn();
    }

    @Test
    void google_createsVerifiedUser_andSetsCookies() throws Exception {
        when(googleTokenVerifier.verify("valid-token"))
                .thenReturn(new GoogleUser("google-sub-1", "newby@test.com", "New Body"));

        MvcResult result = postGoogle("valid-token");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getCookies()).extracting("name")
                .contains("access_token", "refresh_token");

        User created = userRepository.findByEmailIgnoreCase("newby@test.com").orElseThrow();
        assertThat(created.getGoogleSub()).isEqualTo("google-sub-1");
        assertThat(created.isEmailVerified()).isTrue();
        assertThat(created.getPasswordHash()).isNull();
        assertThat(created.getName()).isEqualTo("New Body");
    }

    @Test
    void google_linksExistingLocalAccount_bySharedEmail() throws Exception {
        User local = new User();
        local.setName("Local User");
        local.setEmail("shared@test.com");
        local.setPasswordHash(passwordEncoder.encode("TestPassword12345"));
        local.setEmailVerified(false);
        userRepository.save(local);

        when(googleTokenVerifier.verify("valid-token"))
                .thenReturn(new GoogleUser("google-sub-2", "shared@test.com", "Shared User"));

        postGoogle("valid-token");

        User linked = userRepository.findById(local.getId()).orElseThrow();
        assertThat(linked.getGoogleSub()).isEqualTo("google-sub-2");
        assertThat(linked.isEmailVerified()).isTrue();
        assertThat(linked.getPasswordHash()).isNotNull();
    }

    @Test
    void google_returnsExistingUser_byGoogleSub_withoutDuplicating() throws Exception {
        when(googleTokenVerifier.verify("valid-token"))
                .thenReturn(new GoogleUser("google-sub-3", "repeat@test.com", "Repeat User"));

        postGoogle("valid-token");
        postGoogle("valid-token");

        assertThat(userRepository.findByGoogleSub("google-sub-3")).isPresent();
        assertThat(userRepository.findAll().stream()
                .filter(u -> "repeat@test.com".equalsIgnoreCase(u.getEmail()))
                .count()).isEqualTo(1);
    }
}
