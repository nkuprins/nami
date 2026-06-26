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

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final AppProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendVerificationEmail(String toEmail, String name, String verificationLink) {
        String html = "<p>Hi " + escapeHtml(name) + ",</p>"
                + "<p>Please verify your email address by clicking the link below:</p>"
                + "<p><a href=\"" + escapeHtml(verificationLink) + "\">Verify email</a></p>"
                + "<p>This link expires in 24 hours.</p>";
        send(toEmail, "Verify your Baltnami email", html);
    }

    public void sendPasswordResetEmail(String toEmail, String name, String resetLink) {
        String html = "<p>Hi " + escapeHtml(name) + ",</p>"
                + "<p>You requested a password reset. Click the link below to set a new password:</p>"
                + "<p><a href=\"" + escapeHtml(resetLink) + "\">Reset password</a></p>"
                + "<p>This link expires in 1 hour. If you didn't request this, you can ignore this email.</p>";
        send(toEmail, "Reset your Baltnami password", html);
    }

    public void sendInactivityWarningEmail(String toEmail, String name) {
        String loginUrl = props.frontendUrl();
        String html = "<p>Hi " + escapeHtml(name) + ",</p>"
                + "<p>Your Baltnami account has been inactive for nearly 2 years. "
                + "It will be permanently deleted in approximately 30 days along with all your listings and data.</p>"
                + "<p>If you'd like to keep your account, simply <a href=\"" + escapeHtml(loginUrl) + "\">log in</a>.</p>"
                + "<p>If you no longer need your account, no action is needed.</p>";
        send(toEmail, "Your Baltnami account will be deleted soon", html);
    }

    private void send(String to, String subject, String html) {
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

    private static String escapeHtml(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
