package com.app.backend.messaging;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MediaVariantsTest {

    @Test
    void derive_insertsSuffixBeforeExtension() {
        assertThat(MediaVariants.derive("https://cdn/uploads/x/foo.jpg", "card"))
                .isEqualTo("https://cdn/uploads/x/foo_card.jpg");
    }

    @Test
    void derive_appendsWhenNoExtension() {
        assertThat(MediaVariants.derive("https://cdn/uploads/x/foo", "thumb"))
                .isEqualTo("https://cdn/uploads/x/foo_thumb");
    }

    @Test
    void derive_ignoresDotsInEarlierPathSegments() {
        assertThat(MediaVariants.derive("https://cdn/up.loads/foo", "card"))
                .isEqualTo("https://cdn/up.loads/foo_card");
    }

    @Test
    void withVariants_expandsEachUrlToOriginalPlusVariants() {
        assertThat(MediaVariants.withVariants(List.of("a.jpg")))
                .containsExactly("a.jpg", "a_thumb.jpg", "a_card.jpg");
    }
}
