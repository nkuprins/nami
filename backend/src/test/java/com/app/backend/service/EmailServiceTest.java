package com.app.backend.service;

import com.app.backend.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private ResendClient resendClient;
    @Captor private ArgumentCaptor<String> toCaptor;
    @Captor private ArgumentCaptor<String> subjectCaptor;
    @Captor private ArgumentCaptor<String> htmlCaptor;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(
                new AppProperties.CorsProperties("http://localhost"),
                new AppProperties.S3Properties("bucket", "us-east-1", 5, "https://cdn.test"),
                new AppProperties.JwtProperties("secret-32-chars-long-enough-here!", 900, 604800),
                new AppProperties.ResendProperties("re_key", "test@test.local"),
                "http://localhost:3000",
                new AppProperties.CookieProperties(false),
                null, null, null
        );
        emailService = new EmailService(props, resendClient);
    }

    private void captureSend() {
        verify(resendClient).send(toCaptor.capture(), subjectCaptor.capture(), htmlCaptor.capture());
    }

    @Test
    void verificationEmail_delegates_withLinkAndEscapedName() {
        emailService.sendVerificationEmail("to@test.com", "A<b>", "http://link/verify");

        captureSend();
        assertThat(toCaptor.getValue()).isEqualTo("to@test.com");
        assertThat(subjectCaptor.getValue()).isEqualTo("Verify your Baltnami email");
        assertThat(htmlCaptor.getValue())
                .contains("A&lt;b&gt;")
                .doesNotContain("A<b>")
                .contains("http://link/verify");
    }

    @Test
    void passwordResetEmail_delegates_withResetLink() {
        emailService.sendPasswordResetEmail("to@test.com", "Name", "http://link/reset");

        captureSend();
        assertThat(subjectCaptor.getValue()).isEqualTo("Reset your Baltnami password");
        assertThat(htmlCaptor.getValue()).contains("http://link/reset");
    }

    @Test
    void inactivityWarningEmail_delegates_withFrontendLoginUrl() {
        emailService.sendInactivityWarningEmail("to@test.com", "Name");

        captureSend();
        assertThat(subjectCaptor.getValue()).isEqualTo("Your Baltnami account will be deleted soon");
        assertThat(htmlCaptor.getValue()).contains("http://localhost:3000");
    }

    @Test
    void listingExpiryWarning_usesSingularSubject_forOneListing() {
        emailService.sendListingExpiryWarningEmail("to@test.com", "Name", List.of("Cosy flat"));

        captureSend();
        assertThat(subjectCaptor.getValue()).isEqualTo("Your Baltnami listing expires soon");
        assertThat(htmlCaptor.getValue()).contains("<li><strong>Cosy flat</strong></li>");
    }

    @Test
    void listingExpiryWarning_usesPluralSubject_forMultipleListings() {
        emailService.sendListingExpiryWarningEmail("to@test.com", "Name", List.of("Flat", "House"));

        captureSend();
        assertThat(subjectCaptor.getValue()).isEqualTo("Your Baltnami listings expire soon");
    }

    @Test
    void listingExpired_usesSingularSubject_forOneListing() {
        emailService.sendListingExpiredEmail("to@test.com", "Name", List.of("Cosy flat"));

        captureSend();
        assertThat(subjectCaptor.getValue()).isEqualTo("Your Baltnami listing has expired");
    }

    @Test
    void listingExpired_usesPluralSubject_forMultipleListings() {
        emailService.sendListingExpiredEmail("to@test.com", "Name", List.of("Flat", "House"));

        captureSend();
        assertThat(subjectCaptor.getValue()).isEqualTo("Your Baltnami listings have expired");
    }
}
