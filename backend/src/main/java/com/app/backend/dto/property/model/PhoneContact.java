package com.app.backend.dto.property.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * A contact phone number with the name/email to show alongside it. When
 * {@code name}/{@code email} are blank on write, the owner's account name/email
 * are used instead (see PropertyMapper#applyListingContent).
 */
@Builder(toBuilder = true)
public record PhoneContact(
        @NotBlank String phone,
        String name,
        @Email String email
) {}
