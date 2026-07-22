package com.app.backend.spec;

import com.app.backend.dto.property.request.PropertyFilter;
import com.app.backend.enums.BathroomLayout;
import com.app.backend.enums.CommercialType;
import com.app.backend.enums.Communication;
import com.app.backend.enums.EnergyClass;
import com.app.backend.enums.HeatingType;
import com.app.backend.enums.LandUse;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.ParkingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyExtra;
import com.app.backend.enums.PropertyFeature;
import com.app.backend.enums.RoofType;
import com.app.backend.enums.SecurityFeature;
import com.app.backend.enums.SewageType;
import com.app.backend.enums.StoveType;
import com.app.backend.enums.VentilationSystem;
import com.app.backend.enums.VentilationType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
public record PropertySearchCriteria(
        ListingType listingType,
        PropertyCategory kind,
        CommercialType commercialSubtype,
        LandUse landUse,
        Map<String, List<String>> locByCity,
        List<Long> streetCodes,
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
        List<RoofType> roof,
        List<PropertyFeature> features,
        List<VentilationSystem> ventilationSystems,
        List<Communication> communications,
        List<StoveType> stove,
        List<SecurityFeature> security,
        List<PropertyExtra> extras,
        List<ParkingType> parking,
        PropertyCompletion completion
) {

    /** Maps a web-layer filter into search criteria; {@code locByCity} is the pre-parsed {@code loc} filter. */
    public static PropertySearchCriteria from(PropertyFilter filter, Map<String, List<String>> locByCity) {
        return PropertySearchCriteria.builder()
                .listingType(filter.type())
                .kind(filter.kind())
                .commercialSubtype(filter.commercialSubtype())
                .landUse(filter.landUse())
                .locByCity(locByCity)
                .streetCodes(filter.street() != null ? filter.street() : List.of())
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
                .roof(filter.roof() != null ? filter.roof() : List.of())
                .features(filter.features() != null ? filter.features() : List.of())
                .ventilationSystems(filter.ventilationSystems() != null ? filter.ventilationSystems() : List.of())
                .communications(filter.communications() != null ? filter.communications() : List.of())
                .stove(filter.stove() != null ? filter.stove() : List.of())
                .security(filter.security() != null ? filter.security() : List.of())
                .extras(filter.extras() != null ? filter.extras() : List.of())
                .parking(filter.parking() != null ? filter.parking() : List.of())
                .completion(filter.completion())
                .build();
    }
}
