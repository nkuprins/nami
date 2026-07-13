package com.app.backend.dto.property.request;

import com.app.backend.dto.property.model.LocalizedText;
import com.app.backend.dto.property.model.Media;
import com.app.backend.dto.property.model.Price;
import com.app.backend.dto.property.model.PropertyDetails;
import com.app.backend.enums.Communication;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.ParkingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyExtra;
import com.app.backend.enums.PropertyFeature;
import com.app.backend.enums.SecurityFeature;
import com.app.backend.enums.StoveType;
import com.app.backend.enums.VentilationSystem;

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

    List<VentilationSystem> ventilationSystems();

    List<Communication> communications();

    List<StoveType> stove();

    List<SecurityFeature> security();

    List<PropertyExtra> extras();

    List<ParkingType> parking();

    Media media();

    List<String> phones();

    PropertyCompletion completion();
}
