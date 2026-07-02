package com.app.backend.mapper;

import com.app.backend.dto.CoordsDto;
import com.app.backend.dto.LocalizedText;
import com.app.backend.dto.Location;
import com.app.backend.dto.Media;
import com.app.backend.dto.Price;
import com.app.backend.dto.PropertyDetails;
import com.app.backend.dto.PropertyDto;
import com.app.backend.dto.PropertyItemDto;
import com.app.backend.dto.PropertyListItemDto;
import com.app.backend.dto.PropertyRequest;
import com.app.backend.dto.UpdateListingRequest;
import com.app.backend.dto.UpdatePropertyRequest;
import com.app.backend.entity.Listing;
import com.app.backend.entity.ListingTranslation;
import com.app.backend.entity.Property;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyFeature;
import com.app.backend.enums.SupportedLocale;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PropertyMapper {

    public PropertyItemDto toDto(Listing l) {
        Property p = l.getProperty();
        List<PropertyFeature> features = p.getFeatures().isEmpty() ? null
                : p.getFeatures().stream().sorted().toList();
        List<String> phones = l.getPhones();
        return PropertyItemDto.builder()
                .id(l.getId())
                .propertyId(p.getId())
                .ownerId(l.getOwner().getId())
                .type(l.getListingType())
                .propertyKind(p.getPropertyCategory())
                .price(new Price(l.getPrice(), l.isVatIncluded() ? true : null))
                .details(toFullDetailsDto(p))
                .translations(translations(l.getTranslations(), true))
                .location(toLocation(p))
                .features(features)
                .media(toMediaDto(p))
                .phones(phones.isEmpty() ? null : phones)
                .postedAt(l.getPostedAt())
                .completion(l.getCompletion())
                .expiresAt(l.getExpiresAt())
                .build();
    }

    public PropertyDto toPropertyDto(Property p) {
        List<PropertyFeature> features = p.getFeatures().isEmpty() ? null
                : p.getFeatures().stream().sorted().toList();
        return PropertyDto.builder()
                .id(p.getId())
                .ownerId(p.getOwner().getId())
                .propertyKind(p.getPropertyCategory())
                .details(toFullDetailsDto(p))
                .media(toMediaDto(p))
                .features(features)
                .location(toLocation(p))
                .build();
    }

    public PropertyListItemDto toListDto(Listing l) {
        Property p = l.getProperty();
        List<PropertyFeature> features = p.getFeatures().isEmpty() ? null
                : p.getFeatures().stream().sorted().toList();
        String photo = p.getPhotos().isEmpty() ? null : p.getPhotos().getFirst();
        PropertyDetails details = PropertyDetails.builder()
                .rooms(p.getRooms())
                .bedrooms(p.getBedrooms())
                .bathrooms(p.getBathrooms())
                .m2(p.getM2())
                .landM2(p.getLandM2())
                .floor(p.getFloor())
                .totalFloors(p.getTotalFloors())
                .yearBuilt(p.getYearBuilt())
                .build();
        return PropertyListItemDto.builder()
                .id(l.getId())
                .propertyId(p.getId())
                .ownerId(l.getOwner().getId())
                .type(l.getListingType())
                .propertyKind(p.getPropertyCategory())
                .price(new Price(l.getPrice(), l.isVatIncluded() ? true : null))
                .details(details)
                .translations(translations(l.getTranslations(), false))
                .location(new Location(p.getDistrictSlug(), p.getCitySlug(), p.getAddress(), null))
                .features(features)
                .photo(photo)
                .postedAt(l.getPostedAt())
                .completion(l.getCompletion())
                .expiresAt(l.getExpiresAt())
                .build();
    }

    /**
     * Builds the locale-keyed translation map in {@code lv → en → ru} order,
     * including only locales the listing actually has. When
     * {@code withDescription} is false (list cards) the description is dropped.
     */
    private static Map<String, LocalizedText> translations(Map<String, ListingTranslation> tr,
                                                            boolean withDescription) {
        Map<String, LocalizedText> result = new LinkedHashMap<>();
        for (SupportedLocale locale : SupportedLocale.values()) {
            ListingTranslation t = tr.get(locale.code);
            if (t != null) {
                result.put(locale.code,
                        new LocalizedText(t.getTitle(), withDescription ? t.getDescription() : null));
            }
        }
        return result;
    }

    /** Populates the fields shared by create and update; create-only fields (owner, location, status, expiry) are set by the caller. */
    public void applyCommon(Listing listing, Property property, PropertyRequest req) {
        applyPropertyFields(property, req.propertyKind(), req.details(), req.media(), req.features());
        applyListingFields(listing, req.type(), req.price(), req.completion(), req.translations(), req.phones());
    }

    /** Updates only a listing's own fields (price, translations, phones, completion) — the property is untouched. */
    public void applyListingFields(Listing listing, UpdateListingRequest req) {
        applyListingFields(listing, req.type(), req.price(), req.completion(), req.translations(), req.phones());
    }

    /** Updates only a property's own fields (details, media, features, location) — its listings are untouched. */
    public void applyPropertyFields(Property property, UpdatePropertyRequest req) {
        applyPropertyFields(property, req.propertyKind(), req.details(), req.media(), req.features());
        Location loc = req.location();
        property.setDistrictSlug(loc.district());
        property.setCitySlug(loc.city());
        property.setAddress(loc.address());
        property.setLat(loc.coords().lat());
        property.setLng(loc.coords().lng());
    }

    private static void applyPropertyFields(Property property, PropertyCategory kind, PropertyDetails details,
                                             @Nullable Media media, @Nullable List<PropertyFeature> features) {
        property.setPropertyCategory(kind);
        applyDetails(property, details);
        applyMedia(property, media);
        property.getFeatures().clear();
        if (features != null) {
            property.getFeatures().addAll(features);
        }
    }

    private void applyListingFields(Listing listing, ListingType type, Price price, PropertyCompletion completion,
                                     @Nullable Map<String, LocalizedText> translations, @Nullable List<String> phones) {
        listing.setListingType(type);
        listing.setPrice(price.amount());
        listing.setVatIncluded(Boolean.TRUE.equals(price.vatIncluded()));
        listing.setCompletion(completion);
        applyTranslations(listing, translations);
        listing.setPhones(nullToEmpty(phones));
    }

    public void applyTranslations(Listing listing, @Nullable Map<String, LocalizedText> translations) {
        Map<String, LocalizedText> src = translations != null ? translations : Map.of();
        record Candidate(String locale, String title, String desc) {}
        List<Candidate> candidates = new ArrayList<>();
        for (SupportedLocale locale : SupportedLocale.values()) {
            LocalizedText t = src.get(locale.code);
            candidates.add(new Candidate(locale.code,
                    t != null ? t.title() : null, t != null ? t.description() : null));
        }
        // Presence of at least one complete translation is enforced by @ValidPropertyRequest.
        listing.getTranslations().clear();
        for (Candidate c : candidates) {
            if (StringUtils.hasText(c.title()) && StringUtils.hasText(c.desc())) {
                ListingTranslation lt = new ListingTranslation();
                lt.setListing(listing);
                lt.setLocale(c.locale());
                lt.setTitle(c.title().trim());
                lt.setDescription(c.desc().trim());
                listing.getTranslations().put(c.locale(), lt);
            }
        }
    }

    private static PropertyDetails toFullDetailsDto(Property p) {
        return PropertyDetails.builder()
                .rooms(p.getRooms())
                .bedrooms(p.getBedrooms())
                .bathrooms(p.getBathrooms())
                .bathroomLayout(p.getBathroomLayout())
                .m2(p.getM2())
                .landM2(p.getLandM2())
                .floor(p.getFloor())
                .totalFloors(p.getTotalFloors())
                .yearBuilt(p.getYearBuilt())
                .heating(p.getHeating())
                .energyClass(p.getEnergyClass())
                .maintenanceCost(p.getMaintenanceCost())
                .build();
    }

    private static Media toMediaDto(Property p) {
        List<String> plans = p.getPlans();
        return Media.builder()
                .photos(p.getPhotos().isEmpty() ? null : p.getPhotos())
                .plans(plans.isEmpty() ? null : plans)
                .videoUrl(p.getVideoUrl())
                .build();
    }

    private static Location toLocation(Property p) {
        return new Location(p.getDistrictSlug(), p.getCitySlug(), p.getAddress(),
                new CoordsDto(p.getLat(), p.getLng()));
    }

    private static void applyDetails(Property property, PropertyDetails details) {
        property.setRooms(details.rooms());
        property.setBedrooms(details.bedrooms());
        property.setBathrooms(details.bathrooms());
        property.setBathroomLayout(details.bathroomLayout());
        property.setM2(details.m2());
        property.setLandM2(details.landM2());
        property.setFloor(details.floor());
        property.setTotalFloors(details.totalFloors());
        property.setYearBuilt(details.yearBuilt());
        property.setHeating(details.heating());
        property.setEnergyClass(details.energyClass());
        property.setMaintenanceCost(details.maintenanceCost());
    }

    private static void applyMedia(Property property, @Nullable Media media) {
        property.setPhotos(nullToEmpty(media != null ? media.photos() : null));
        property.setPlans(nullToEmpty(media != null ? media.plans() : null));
        property.setVideoUrl(media != null ? media.videoUrl() : null);
    }

    private static List<String> nullToEmpty(List<String> values) {
        return values != null ? new ArrayList<>(values) : new ArrayList<>();
    }
}
