package com.app.backend.service;

import com.app.backend.config.AppProperties;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Transport for outbound transactional email via the Resend API. When no API key is
 * configured (local dev / tests) sending is skipped. Email <em>content</em> (subjects,
 * HTML bodies) is composed by {@link EmailService}; this class only delivers it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResendClient {

    private final AppProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void send(String to, String subject, String html) {
        String apiKey = props.resend().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.info("RESEND_API_KEY not set - skipping email: {}", subject);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "from", props.resend().from(),
                    "to", List.of(to),
                    "subject", subject,
                    "html", html
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Resend API error {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to send email ({}): {}", subject, e.getMessage());
        }
    }
}
