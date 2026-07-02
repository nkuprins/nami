package com.app.backend.dto;

import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCompletion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Updates a listing's own fields. The property it belongs to (rooms, m2,
 * media, features, location) is not touched — edit it via the separate
 * property endpoints instead.
 */
@Builder(toBuilder = true)
public record UpdateListingRequest(
        @NotNull ListingType type,
        @NotNull @Valid Price price,
        Map<String, LocalizedText> translations,
        List<@NotBlank String> phones,
        PropertyCompletion completion
) {

    @AssertTrue(message = "At least one complete translation (title + description) is required")
    public boolean isTranslationsComplete() {
        return translations != null && translations.values().stream()
                .anyMatch(UpdateListingRequest::isComplete);
    }

    private static boolean isComplete(LocalizedText t) {
        return t != null && StringUtils.hasText(t.title()) && StringUtils.hasText(t.description());
    }
}
