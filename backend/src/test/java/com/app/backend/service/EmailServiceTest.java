package com.app.backend.service;

import com.app.backend.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Test
    void sendVerificationEmail_skips_whenApiKeyBlank() {
        AppProperties props = new AppProperties(
                new AppProperties.CorsProperties("http://localhost"),
                new AppProperties.S3Properties("bucket", "us-east-1", 5, "https://cdn.test"),
                new AppProperties.JwtProperties("secret-32-chars-long-enough-here!", 900, 604800),
                new AppProperties.ResendProperties("", "test@test.local"),
                "http://localhost:3000",
                new AppProperties.CookieProperties(false),
                null
        );
        EmailService service = new EmailService(props, new ObjectMapper());

        assertThatCode(() -> service.sendVerificationEmail("to@test.com", "Name", "http://link"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendPasswordResetEmail_skips_whenApiKeyNull() {
        AppProperties props = new AppProperties(
                new AppProperties.CorsProperties("http://localhost"),
                new AppProperties.S3Properties("bucket", "us-east-1", 5, "https://cdn.test"),
                new AppProperties.JwtProperties("secret-32-chars-long-enough-here!", 900, 604800),
                new AppProperties.ResendProperties(null, "test@test.local"),
                "http://localhost:3000",
                new AppProperties.CookieProperties(false),
                null
        );
        EmailService service = new EmailService(props, new ObjectMapper());

        assertThatCode(() -> service.sendPasswordResetEmail("to@test.com", "Name", "http://link"))
                .doesNotThrowAnyException();
    }
}
