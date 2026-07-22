package com.app.backend.category;

/**
 * The listing fields whose presence is conditional on {@code property_category}.
 * Each maps to where it lives on a property request so validation violations can
 * point at the right node. Fields that apply to every category (price, floor,
 * year, feature sets, …) are not modelled here.
 */
public enum CategoryField {
    ROOMS("details", "rooms"),
    M2("details", "m2"),
    LAND_M2("details", "landM2"),
    LAND_USE(null, "landUse"),
    NEW_PROJECT_KIND(null, "newProjectKind"),
    COMMERCIAL_SUBTYPE(null, "commercialSubtype"),
    COMPLETION(null, "completion");

    /** Parent node on the request DTO, or {@code null} when the field is top-level. */
    public final String parentNode;
    public final String node;

    CategoryField(String parentNode, String node) {
        this.parentNode = parentNode;
        this.node = node;
    }
}
