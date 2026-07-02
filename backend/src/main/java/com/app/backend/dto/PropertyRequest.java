package com.app.backend.dto;

import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyFeature;

import java.util.List;
import java.util.Map;

/**
 * Common shape of the create/update property requests: the fields both share.
 * Lets cross-field validation ({@code @ValidPropertyRequest}) be declared once
 * and lets the service populate the shared fields via a single code path.
 * Create-only fields (location, duration) stay off the interface.
 */
public interface PropertyRequest {
    ListingType type();

    PropertyCategory propertyKind();

    Price price();

    PropertyDetails details();

    Map<String, LocalizedText> translations();

    List<PropertyFeature> features();

    Media media();

    List<String> phones();

    PropertyCompletion completion();
}
