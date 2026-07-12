package com.app.backend.spec;

import com.app.backend.dto.property.request.PropertyFilter;
import com.app.backend.enums.BathroomLayout;
import com.app.backend.enums.EnergyClass;
import com.app.backend.enums.HeatingType;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyFeature;
import com.app.backend.enums.SewageType;
import com.app.backend.enums.VentilationType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
public record PropertySearchCriteria(
        ListingType listingType,
        PropertyCategory kind,
        Map<String, List<String>> locByCity,
        BigDecimal priceMin,
        BigDecimal priceMax,
        List<Integer> rooms,
        List<Integer> bedrooms,
        List<Integer> bathrooms,
        BigDecimal m2Min,
        BigDecimal m2Max,
        BigDecimal landM2Min,
        BigDecimal landM2Max,
        Short floorMin,
        Short floorMax,
        Boolean notGround,
        Boolean notTop,
        Short yearMin,
        Short yearMax,
        BigDecimal maintenanceCostMax,
        BathroomLayout bathroomLayout,
        Boolean vatIncluded,
        List<HeatingType> heating,
        List<EnergyClass> energyClass,
        List<SewageType> sewage,
        List<VentilationType> ventilation,
        List<PropertyFeature> features,
        PropertyCompletion completion
) {

    /** Maps a web-layer filter into search criteria; {@code locByCity} is the pre-parsed {@code loc} filter. */
    public static PropertySearchCriteria from(PropertyFilter filter, Map<String, List<String>> locByCity) {
        return PropertySearchCriteria.builder()
                .listingType(filter.type())
                .kind(filter.kind())
                .locByCity(locByCity)
                .priceMin(filter.priceMin()).priceMax(filter.priceMax())
                .rooms(filter.rooms())
                .bedrooms(filter.bedrooms()).bathrooms(filter.bathrooms())
                .m2Min(filter.m2Min()).m2Max(filter.m2Max())
                .landM2Min(filter.landM2Min()).landM2Max(filter.landM2Max())
                .floorMin(filter.floorMin()).floorMax(filter.floorMax())
                .notGround(filter.notGround()).notTop(filter.notTop())
                .yearMin(filter.yearMin()).yearMax(filter.yearMax())
                .maintenanceCostMax(filter.maintenanceCostMax())
                .bathroomLayout(filter.bathroomLayout())
                .vatIncluded(filter.vatIncluded())
                .heating(filter.heating() != null ? filter.heating() : List.of())
                .energyClass(filter.energyClass() != null ? filter.energyClass() : List.of())
                .sewage(filter.sewage() != null ? filter.sewage() : List.of())
                .ventilation(filter.ventilation() != null ? filter.ventilation() : List.of())
                .features(filter.features() != null ? filter.features() : List.of())
                .completion(filter.completion())
                .build();
    }
}
