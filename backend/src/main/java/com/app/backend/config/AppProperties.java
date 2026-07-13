package com.app.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(
        CorsProperties cors,
        S3Properties s3,
        JwtProperties jwt,
        ResendProperties resend,
        String frontendUrl,
        CookieProperties cookie,
        TurnstileProperties turnstile,
        AddressRegisterProperties addressRegister
) {
    public record JwtProperties(String secret, long accessTokenTtlSeconds, long refreshTokenTtlSeconds) {}
    public record ResendProperties(String apiKey, String from) {}
    public record CookieProperties(boolean secure) {}
    public record TurnstileProperties(String secretKey) {}
    public record CorsProperties(String allowedOrigins) {}
    public record S3Properties(String bucket, String region, long presignTtlMinutes, String cdnUrl) {}
    public record AddressRegisterProperties(boolean autoIngest, AddressRegisterUrls urls) {}
    public record AddressRegisterUrls(String novads, String pagasts, String pilseta,
                                      String ciems, String iela, String eka) {}
}
