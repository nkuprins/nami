package com.app.backend.service;

import com.app.backend.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class ResendClientTest {

    private static AppProperties propsWithApiKey(String apiKey) {
        return AppProperties.builder()
                .cors(new AppProperties.CorsProperties("http://localhost"))
                .s3(new AppProperties.S3Properties("bucket", "us-east-1", 5, "https://cdn.test"))
                .jwt(new AppProperties.JwtProperties("secret-32-chars-long-enough-here!", 900, 604800))
                .resend(new AppProperties.ResendProperties(apiKey, "test@test.local"))
                .frontendUrl("http://localhost:3000")
                .cookie(new AppProperties.CookieProperties(false))
                .build();
    }

    @Test
    void send_skips_whenApiKeyBlank() {
        ResendClient client = new ResendClient(propsWithApiKey(""), new ObjectMapper());

        assertThatCode(() -> client.send("to@test.com", "Subject", "<p>body</p>"))
                .doesNotThrowAnyException();
    }

    @Test
    void send_skips_whenApiKeyNull() {
        ResendClient client = new ResendClient(propsWithApiKey(null), new ObjectMapper());

        assertThatCode(() -> client.send("to@test.com", "Subject", "<p>body</p>"))
                .doesNotThrowAnyException();
    }
}
