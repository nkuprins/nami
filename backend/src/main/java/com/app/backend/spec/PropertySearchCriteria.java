package com.app.backend.spec;

import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyFeature;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PropertySearchCriteria(
        ListingType listingType,
        Map<String, List<String>> locByCity,
        BigDecimal priceMin,
        BigDecimal priceMax,
        List<Integer> rooms,
        BigDecimal m2Min,
        BigDecimal m2Max,
        Short floorMin,
        Short floorMax,
        Boolean notGround,
        Boolean notTop,
        Short yearMin,
        Short yearMax,
        List<PropertyFeature> features,
        PropertyCompletion completion
) {}
