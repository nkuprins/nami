package com.app.backend.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddressMatcherTest {

    @Test
    void normalize_foldsDiacritics_stripsPunctuation_lowercases() {
        assertThat(AddressMatcher.normalize("Elīzabetes iela 2.")).isEqualTo("elizabetes iela 2");
    }

    @Test
    void isDuplicate_true_ignoringDiacriticsAndPunctuation() {
        assertThat(AddressMatcher.isDuplicate("Brīvības iela 12", "brivibas iela, 12")).isTrue();
    }

    @Test
    void isDuplicate_false_forDifferentStreet() {
        assertThat(AddressMatcher.isDuplicate("Brivibas iela 12", "Elizabetes iela 12")).isFalse();
    }

    @Test
    void isNearMatch_true_forTypo() {
        assertThat(AddressMatcher.isNearMatch("Elizabetes iela", "Elazebetes iela")).isTrue();
    }

    @Test
    void isNearMatch_true_forTypoWithAddedSuffix() {
        assertThat(AddressMatcher.isNearMatch("Elizabetes iela", "Elizabetes ieal 2a")).isTrue();
    }

    @Test
    void isNearMatch_false_forExactDuplicate() {
        assertThat(AddressMatcher.isNearMatch("Elizabetes iela", "elizabetes iela")).isFalse();
    }

    @Test
    void isNearMatch_false_forDifferentStreet() {
        assertThat(AddressMatcher.isNearMatch("Elizabetes iela", "Brivibas iela")).isFalse();
    }
}
