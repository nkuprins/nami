package com.app.backend.service;

import com.app.backend.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Composes transactional email content (subject + HTML body) and hands it to
 * {@link ResendClient} for delivery. The two concerns are split so email copy can
 * change independently of the Resend transport.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final AppProperties props;
    private final ResendClient resendClient;

    public void sendVerificationEmail(String toEmail, String name, String verificationLink) {
        String html = "<p>Hi " + escapeHtml(name) + ",</p>"
                + "<p>Please verify your email address by clicking the link below:</p>"
                + "<p><a href=\"" + escapeHtml(verificationLink) + "\">Verify email</a></p>"
                + "<p>This link expires in 24 hours.</p>";
        resendClient.send(toEmail, "Verify your Baltnami email", html);
    }

    public void sendPasswordResetEmail(String toEmail, String name, String resetLink) {
        String html = "<p>Hi " + escapeHtml(name) + ",</p>"
                + "<p>You requested a password reset. Click the link below to set a new password:</p>"
                + "<p><a href=\"" + escapeHtml(resetLink) + "\">Reset password</a></p>"
                + "<p>This link expires in 1 hour. If you didn't request this, you can ignore this email.</p>";
        resendClient.send(toEmail, "Reset your Baltnami password", html);
    }

    public void sendInactivityWarningEmail(String toEmail, String name) {
        String loginUrl = props.frontendUrl();
        String html = "<p>Hi " + escapeHtml(name) + ",</p>"
                + "<p>Your Baltnami account has been inactive for nearly 2 years. "
                + "It will be permanently deleted in approximately 30 days along with all your listings and data.</p>"
                + "<p>If you'd like to keep your account, simply <a href=\"" + escapeHtml(loginUrl) + "\">log in</a>.</p>"
                + "<p>If you no longer need your account, no action is needed.</p>";
        resendClient.send(toEmail, "Your Baltnami account will be deleted soon", html);
    }

    public void sendListingExpiryWarningEmail(String toEmail, String name, List<String> listingTitles) {
        String loginUrl = props.frontendUrl();
        String items = listingTitles.stream()
                .map(t -> "<li><strong>" + escapeHtml(t) + "</strong></li>")
                .collect(java.util.stream.Collectors.joining());
        String html = "<p>Hi " + escapeHtml(name) + ",</p>"
                + "<p>The following listing" + (listingTitles.size() > 1 ? "s" : "") + " will expire within 7 days "
                + "and will no longer be publicly visible:</p>"
                + "<ul>" + items + "</ul>"
                + "<p><a href=\"" + escapeHtml(loginUrl) + "\">Log in</a> to renew them and choose a new listing duration.</p>"
                + "<p>If you no longer need these listings, no action is needed.</p>";
        resendClient.send(toEmail, "Your Baltnami listing" + (listingTitles.size() > 1 ? "s expire" : " expires") + " soon", html);
    }

    public void sendListingExpiredEmail(String toEmail, String name, List<String> listingTitles) {
        String loginUrl = props.frontendUrl();
        String items = listingTitles.stream()
                .map(t -> "<li><strong>" + escapeHtml(t) + "</strong></li>")
                .collect(java.util.stream.Collectors.joining());
        String html = "<p>Hi " + escapeHtml(name) + ",</p>"
                + "<p>The following listing" + (listingTitles.size() > 1 ? "s have" : " has") + " expired and "
                + (listingTitles.size() > 1 ? "are" : "is") + " no longer publicly visible:</p>"
                + "<ul>" + items + "</ul>"
                + "<p><a href=\"" + escapeHtml(loginUrl) + "\">Log in</a> to renew them and make them active again.</p>";
        resendClient.send(toEmail, "Your Baltnami listing" + (listingTitles.size() > 1 ? "s have" : " has") + " expired", html);
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
