package com.app.backend.category;

import com.app.backend.enums.PropertyCategory;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Structural guards for the category → conditional-field registry. The registry
 * drives friendly validation; the DB CHECK constraints (db/schema.sql) are the
 * matching last line of defence — the assertions here encode the same intent so
 * the two can't silently drift.
 */
class CategoryProfileTest {

    @Test
    void everyCategoryHasAProfile() {
        for (PropertyCategory category : PropertyCategory.values()) {
            assertThat(CategoryProfile.of(category)).isNotNull();
        }
    }

    @Test
    void requiredAndForbiddenNeverOverlap() {
        for (PropertyCategory category : PropertyCategory.values()) {
            CategoryProfile profile = CategoryProfile.of(category);
            Set<CategoryField> overlap = EnumSet.copyOf(profile.required());
            overlap.retainAll(profile.forbidden());
            assertThat(overlap)
                    .as("required/forbidden overlap for %s", category)
                    .isEmpty();
        }
    }

    @Test
    void newProjectRequiresItsKind() {
        assertThat(CategoryProfile.of(PropertyCategory.NEW_PROJECT).requires(CategoryField.NEW_PROJECT_KIND)).isTrue();
        // Every other category forbids it (mirrors chk_new_project_kind).
        for (PropertyCategory category : otherThan(PropertyCategory.NEW_PROJECT)) {
            assertThat(CategoryProfile.of(category).forbids(CategoryField.NEW_PROJECT_KIND))
                    .as("%s should forbid new_project_kind", category)
                    .isTrue();
        }
    }

    @Test
    void commercialRequiresItsSubtype() {
        assertThat(CategoryProfile.of(PropertyCategory.COMMERCIAL).requires(CategoryField.COMMERCIAL_SUBTYPE)).isTrue();
        for (PropertyCategory category : otherThan(PropertyCategory.COMMERCIAL)) {
            assertThat(CategoryProfile.of(category).forbids(CategoryField.COMMERCIAL_SUBTYPE))
                    .as("%s should forbid commercial_subtype", category)
                    .isTrue();
        }
    }

    @Test
    void landAndGarageHaveNoRooms() {
        assertThat(CategoryProfile.of(PropertyCategory.LAND).forbids(CategoryField.ROOMS)).isTrue();
        assertThat(CategoryProfile.of(PropertyCategory.GARAGE).forbids(CategoryField.ROOMS)).isTrue();
    }

    @Test
    void plotAreaOnlyOnHouseAndLand() {
        // Mirrors chk_land_m2_scope: everything except house & land forbids land_m2.
        for (PropertyCategory category : PropertyCategory.values()) {
            boolean plotAllowed = category == PropertyCategory.HOUSE || category == PropertyCategory.LAND;
            assertThat(CategoryProfile.of(category).forbids(CategoryField.LAND_M2))
                    .as("land_m2 scope for %s", category)
                    .isEqualTo(!plotAllowed);
        }
    }

    @Test
    void landUseScopedToLandAndCommercial() {
        // Mirrors chk_land_use_scope: allowed (not forbidden) only for land & commercial.
        for (PropertyCategory category : PropertyCategory.values()) {
            boolean allowed = category == PropertyCategory.LAND || category == PropertyCategory.COMMERCIAL;
            assertThat(CategoryProfile.of(category).forbids(CategoryField.LAND_USE))
                    .as("land_use scope for %s", category)
                    .isEqualTo(!allowed);
        }
    }

    @Test
    void completionScopedToNewProject() {
        // Mirrors chk_completion_new_project_only.
        for (PropertyCategory category : PropertyCategory.values()) {
            boolean forbidden = category != PropertyCategory.NEW_PROJECT;
            assertThat(CategoryProfile.of(category).forbids(CategoryField.COMPLETION))
                    .as("completion scope for %s", category)
                    .isEqualTo(forbidden);
        }
    }

    private static Set<PropertyCategory> otherThan(PropertyCategory category) {
        Set<PropertyCategory> set = EnumSet.allOf(PropertyCategory.class);
        set.remove(category);
        return Collections.unmodifiableSet(set);
    }
}
