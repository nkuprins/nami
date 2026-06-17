package com.app.backend.service;

import com.app.backend.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final AppProperties props;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendVerificationEmail(String toEmail, String name, String verificationLink) {
        String html = "<p>Hi " + name + ",</p>"
                + "<p>Please verify your email address by clicking the link below:</p>"
                + "<p><a href=\"" + verificationLink + "\">Verify email</a></p>"
                + "<p>This link expires in 24 hours.</p>";
        send(toEmail, "Verify your Baltnami email", html);
    }

    public void sendPasswordResetEmail(String toEmail, String name, String resetLink) {
        String html = "<p>Hi " + name + ",</p>"
                + "<p>You requested a password reset. Click the link below to set a new password:</p>"
                + "<p><a href=\"" + resetLink + "\">Reset password</a></p>"
                + "<p>This link expires in 1 hour. If you didn't request this, you can ignore this email.</p>";
        send(toEmail, "Reset your Baltnami password", html);
    }

    private void send(String to, String subject, String html) {
        String apiKey = props.resend().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.info("RESEND_API_KEY not set - skipping email to {}: {}", to, subject);
            return;
        }
        try {
            String escaped = html.replace("\\", "\\\\").replace("\"", "\\\"");
            String json = "{\"from\":\"" + props.resend().from() + "\","
                    + "\"to\":[\"" + to + "\"],"
                    + "\"subject\":\"" + subject + "\","
                    + "\"html\":\"" + escaped + "\"}";

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
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
