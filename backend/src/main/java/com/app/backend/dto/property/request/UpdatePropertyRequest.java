package com.app.backend.dto.property.request;

import com.app.backend.dto.property.model.Location;
import com.app.backend.dto.property.model.Media;
import com.app.backend.dto.property.model.PropertyDetails;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyFeature;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

/**
 * Updates a property's own fields (physical attributes, media, features,
 * location). The listings on it (price, translations, phones, completion)
 * are not touched — edit them via the separate listing endpoint instead.
 */
@Builder(toBuilder = true)
public record UpdatePropertyRequest(
        @NotNull PropertyCategory propertyKind,
        @NotNull @Valid PropertyDetails details,
        List<PropertyFeature> features,
        @Valid Media media,
        @NotNull @Valid Location location
) {

    @AssertTrue(message = "land_m2 is not valid for an apartment")
    public boolean isLandM2Valid() {
        return details == null || details.landM2() == null || propertyKind != PropertyCategory.APARTMENT;
    }
}
