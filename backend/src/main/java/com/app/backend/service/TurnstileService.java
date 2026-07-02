package com.app.backend.service;

import com.app.backend.config.AppProperties;
import com.app.backend.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Verifies Cloudflare Turnstile tokens (the human-check widget rendered on the
 * add-listing form). Fails closed: a missing, spent, or unverifiable token is
 * rejected. When no secret key is configured (local dev / tests) verification
 * is skipped entirely, mirroring {@link EmailService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TurnstileService {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final AppProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void verify(String token, String clientIp) {
        String secret = props.turnstile().secretKey();
        if (secret == null || secret.isBlank()) {
            log.debug("TURNSTILE_SECRET_KEY not set - skipping Turnstile verification");
            return;
        }
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "TURNSTILE_FAILED");
        }

        try {
            String body = "secret=" + URLEncoder.encode(secret, StandardCharsets.UTF_8)
                    + "&response=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                    + "&remoteip=" + URLEncoder.encode(clientIp, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VERIFY_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (!json.path("success").asBoolean(false)) {
                log.warn("Turnstile verification failed: {}", json.path("error-codes"));
                throw new ApiException(HttpStatus.FORBIDDEN, "TURNSTILE_FAILED");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Turnstile verification error: {}", e.getMessage());
            throw new ApiException(HttpStatus.FORBIDDEN, "TURNSTILE_FAILED");
        }
    }
}
