package com.app.backend.category;

import com.app.backend.enums.PropertyCategory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static com.app.backend.category.CategoryField.COMMERCIAL_SUBTYPE;
import static com.app.backend.category.CategoryField.COMPLETION;
import static com.app.backend.category.CategoryField.LAND_M2;
import static com.app.backend.category.CategoryField.LAND_USE;
import static com.app.backend.category.CategoryField.M2;
import static com.app.backend.category.CategoryField.NEW_PROJECT_KIND;
import static com.app.backend.category.CategoryField.ROOMS;

/**
 * The single source of truth for which conditional fields ({@link CategoryField})
 * each {@link PropertyCategory} requires or forbids. Fields listed in neither set
 * are optional. Consumed by {@code PropertyRequestValidator} for friendly 400s;
 * the DB CHECK constraints (see {@code db/schema.sql}) are the matching last line
 * of defence — the two must agree (asserted by {@code CategoryProfileTest}).
 *
 * <p>Mirror on the frontend: {@code frontend/src/types/categoryRegistry.ts}.
 */
public record CategoryProfile(
        PropertyCategory category,
        Set<CategoryField> required,
        Set<CategoryField> forbidden
) {
    private static final Map<PropertyCategory, CategoryProfile> BY_CATEGORY =
            new EnumMap<>(PropertyCategory.class);

    private static void register(PropertyCategory category,
                                 Set<CategoryField> required,
                                 Set<CategoryField> forbidden) {
        BY_CATEGORY.put(category, new CategoryProfile(category, required, forbidden));
    }

    static {
        register(PropertyCategory.APARTMENT,
                Set.of(ROOMS, M2),
                Set.of(LAND_M2, LAND_USE, NEW_PROJECT_KIND, COMMERCIAL_SUBTYPE, COMPLETION));

        register(PropertyCategory.HOUSE,
                Set.of(ROOMS, M2),
                Set.of(LAND_USE, NEW_PROJECT_KIND, COMMERCIAL_SUBTYPE, COMPLETION));

        // new_project follows its apartment|house kind: rooms + building area apply.
        register(PropertyCategory.NEW_PROJECT,
                Set.of(NEW_PROJECT_KIND, ROOMS, M2),
                Set.of(LAND_M2, LAND_USE, COMMERCIAL_SUBTYPE));

        register(PropertyCategory.COMMERCIAL,
                Set.of(COMMERCIAL_SUBTYPE, M2),
                Set.of(LAND_M2, NEW_PROJECT_KIND, COMPLETION));

        register(PropertyCategory.LAND,
                Set.of(LAND_USE, LAND_M2),
                Set.of(ROOMS, M2, NEW_PROJECT_KIND, COMMERCIAL_SUBTYPE, COMPLETION));

        register(PropertyCategory.GARAGE,
                Set.of(M2),
                Set.of(ROOMS, LAND_M2, LAND_USE, NEW_PROJECT_KIND, COMMERCIAL_SUBTYPE, COMPLETION));
    }

    public static CategoryProfile of(PropertyCategory category) {
        CategoryProfile profile = BY_CATEGORY.get(category);
        if (profile == null) {
            throw new IllegalStateException("No CategoryProfile for " + category);
        }
        return profile;
    }

    public boolean requires(CategoryField field) {
        return required.contains(field);
    }

    public boolean forbids(CategoryField field) {
        return forbidden.contains(field);
    }
}
