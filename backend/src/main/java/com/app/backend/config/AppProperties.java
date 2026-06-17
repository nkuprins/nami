package com.app.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(
        CorsProperties cors,
        S3Properties s3,
        JwtProperties jwt,
        ResendProperties resend,
        String frontendUrl,
        CookieProperties cookie
) {
    public record JwtProperties(String secret, long accessTokenTtlSeconds, long refreshTokenTtlSeconds) {}
    public record ResendProperties(String apiKey, String from) {}
    public record CookieProperties(boolean secure) {}
    public record CorsProperties(String allowedOrigins) {}
    public record S3Properties(String bucket, String region, long presignTtlMinutes, String cdnUrl) {}
}
