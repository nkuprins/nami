package com.app.backend.controller;

import com.app.backend.IntegrationTestBase;
import com.app.backend.dto.upload.PresignRequest;
import com.app.backend.entity.User;
import com.app.backend.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UploadControllerIntegrationTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Cookie userCookie;

    @BeforeEach
    void setUpUser() throws Exception {
        User u = new User();
        u.setName("Uploader");
        u.setEmail("uploader@test.com");
        u.setPasswordHash(passwordEncoder.encode("TestPassword12345"));
        u.setEmailVerified(true);
        User saved = userRepository.save(u);
        userCookie = authTestHelper.accessTokenCookie(saved.getId());

        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://s3.amazonaws.com/presigned").toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);
    }

    @Test
    void presign_returns200_withPresignedUrls() throws Exception {
        PresignRequest req = new PresignRequest(List.of("photo.jpg"));

        mockMvc.perform(post("/api/uploads/presign")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uploadUrl").value("https://s3.amazonaws.com/presigned"))
                .andExpect(jsonPath("$[0].fileUrl").isNotEmpty());
    }

    @Test
    void presign_returns401_whenNotAuthenticated() throws Exception {
        PresignRequest req = new PresignRequest(List.of("photo.jpg"));

        mockMvc.perform(post("/api/uploads/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void presign_returns400_whenFilenamesEmpty() throws Exception {
        PresignRequest req = new PresignRequest(List.of());

        mockMvc.perform(post("/api/uploads/presign")
                        .cookie(userCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
