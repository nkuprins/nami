package com.app.backend.mapper;

import com.app.backend.dto.property.model.CoordsDto;
import com.app.backend.dto.property.model.LocalizedText;
import com.app.backend.dto.property.model.Location;
import com.app.backend.dto.property.model.Media;
import com.app.backend.dto.property.model.PhoneContact;
import com.app.backend.dto.property.model.Price;
import com.app.backend.dto.property.model.PropertyDetails;
import com.app.backend.dto.property.response.PropertyDto;
import com.app.backend.dto.property.response.PropertyItemDto;
import com.app.backend.dto.property.response.PropertyListItemDto;
import com.app.backend.dto.property.request.PropertyRequest;
import com.app.backend.entity.Listing;
import com.app.backend.entity.ListingTranslation;
import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.enums.SupportedLocale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PropertyMapper {

    /** Full detail with every locale's translation — used by write responses and the edit flow. */
    public PropertyItemDto toDto(Listing l) {
        return toDto(l, null);
    }

    /**
     * Detail view. When {@code locale} is null the {@code translations} map carries every
     * locale (edit flow); otherwise it carries a single entry — the fallback-resolved content
     * for {@code locale}, keyed under {@code locale} — so display callers ship one language.
     * {@code availableLocales} always lists every locale the listing actually has.
     */
    public PropertyItemDto toDto(Listing l, String locale) {
        Property p = l.getProperty();
        List<PhoneContact> phones = l.getPhones();
        return PropertyItemDto.builder()
                .id(l.getId())
                .propertyId(p.getId())
                .ownerId(l.getOwner().getId())
                .type(l.getListingType())
                .propertyKind(l.getPropertyCategory())
                .newProjectKind(l.getNewProjectKind())
                .commercialSubtype(l.getCommercialSubtype())
                .landUse(l.getLandUse())
                .price(new Price(l.getPrice(), l.isVatIncluded() ? true : null))
                .details(toFullDetailsDto(l))
                .translations(locale == null
                        ? translations(l.getTranslations(), true)
                        : singleLocale(l.getTranslations(), locale))
                .availableLocales(availableLocales(l.getTranslations()))
                .location(toLocation(p))
                .features(sorted(l.getFeatures()))
                .ventilationSystems(sorted(l.getVentilationSystems()))
                .communications(sorted(l.getCommunications()))
                .stove(sorted(l.getStove()))
                .security(sorted(l.getSecurity()))
                .extras(sorted(l.getExtras()))
                .parking(sorted(l.getParking()))
                .media(toMediaDto(l))
                .phones(phones.isEmpty() ? null : phones)
                .postedAt(l.getPostedAt())
                .completion(l.getCompletion())
                .expiresAt(l.getExpiresAt())
                .status(l.getStatus())
                .cadastreVerified(l.isCadastreVerified() ? true : null)
                .build();
    }

    /** The owner's editable address record; every physical/media attribute lives on the listing now. */
    public PropertyDto toPropertyDto(Property p) {
        return PropertyDto.builder()
                .id(p.getId())
                .ownerId(p.getOwner().getId())
                .location(toLocation(p))
                .build();
    }

    public PropertyListItemDto toListDto(Listing l) {
        Property p = l.getProperty();
        List<String> photos = l.getPhotos();
        String photo = photos.isEmpty() ? null : photos.getFirst();
        PropertyDetails details = PropertyDetails.builder()
                .rooms(l.getRooms())
                .bedrooms(l.getBedrooms())
                .bathrooms(l.getBathrooms())
                .m2(l.getM2())
                .landM2(l.getLandM2())
                .floor(l.getFloor())
                .totalFloors(l.getTotalFloors())
                .yearBuilt(l.getYearBuilt())
                .build();
        return PropertyListItemDto.builder()
                .id(l.getId())
                .propertyId(p.getId())
                .ownerId(l.getOwner().getId())
                .type(l.getListingType())
                .propertyKind(l.getPropertyCategory())
                .newProjectKind(l.getNewProjectKind())
                .commercialSubtype(l.getCommercialSubtype())
                .landUse(l.getLandUse())
                .price(new Price(l.getPrice(), l.isVatIncluded() ? true : null))
                .details(details)
                .translations(translations(l.getTranslations(), false))
                .location(Location.builder()
                        .district(p.getDistrictSlug())
                        .city(p.getCitySlug())
                        .address(p.getAddress())
                        .arBuildingCode(p.getArBuildingCode())
                        .apartment(p.getApartment())
                        .cadastreParcelNr(p.getCadastreParcelNr())
                        .build())
                .features(sorted(l.getFeatures()))
                .photo(photo)
                .postedAt(l.getPostedAt())
                .completion(l.getCompletion())
                .expiresAt(l.getExpiresAt())
                .cadastreVerified(l.isCadastreVerified() ? true : null)
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

    /** Per-locale fallback chain, mirroring the frontend's {@code LOCALE_FALLBACK_ORDER}. */
    private static final Map<String, List<String>> FALLBACK_ORDER = Map.of(
            "lv", List.of("lv", "en", "ru"),
            "en", List.of("en", "lv", "ru"),
            "ru", List.of("ru", "lv", "en"));

    /** The single fallback-resolved translation for {@code locale}, keyed under {@code locale}. */
    private static Map<String, LocalizedText> singleLocale(Map<String, ListingTranslation> tr, String locale) {
        LocalizedText resolved = resolve(tr, locale);
        return resolved == null ? Map.of() : Map.of(locale, resolved);
    }

    /** Every locale the listing has, in {@code lv → en → ru} order; null when it has none. */
    private static List<String> availableLocales(Map<String, ListingTranslation> tr) {
        List<String> result = new ArrayList<>();
        for (SupportedLocale locale : SupportedLocale.values()) {
            if (tr.containsKey(locale.code)) {
                result.add(locale.code);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /** Title+description for the given locale, falling back through {@link #FALLBACK_ORDER}. */
    public LocalizedText toTranslation(Listing l, String locale) {
        return resolve(l.getTranslations(), locale);
    }

    private static LocalizedText resolve(Map<String, ListingTranslation> tr, String locale) {
        // Every stored translation has both title and description (enforced on write), so
        // entry-level fallback matches the frontend's field-level fallback.
        for (String code : FALLBACK_ORDER.getOrDefault(locale, List.of("lv", "en", "ru"))) {
            ListingTranslation t = tr.get(code);
            if (t != null) {
                return new LocalizedText(t.getTitle(), t.getDescription());
            }
        }
        return null;
    }

    /** Writes a listing's full content (category, physical, media, features, terms). Owner, status and expiry are set by the caller. */
    public void applyListingContent(Listing listing, PropertyRequest req) {
        listing.setListingType(req.type());
        listing.setPropertyCategory(req.propertyKind());
        listing.setNewProjectKind(req.newProjectKind());
        listing.setCommercialSubtype(req.commercialSubtype());
        listing.setLandUse(req.landUse());
        listing.setPrice(req.price().amount());
        listing.setVatIncluded(Boolean.TRUE.equals(req.price().vatIncluded()));
        listing.setCompletion(req.completion());
        applyDetails(listing, req.details());
        applyMedia(listing, req.media());
        replace(listing.getFeatures(), req.features());
        replace(listing.getVentilationSystems(), req.ventilationSystems());
        replace(listing.getCommunications(), req.communications());
        replace(listing.getStove(), req.stove());
        replace(listing.getSecurity(), req.security());
        replace(listing.getExtras(), req.extras());
        replace(listing.getParking(), req.parking());
        applyTranslations(listing, req.translations());
        listing.setPhones(applyPhoneDefaults(nullToEmpty(req.phones()), listing.getOwner()));
    }

    /** Fills a blank contact name/email in a phone entry from the listing owner's account. */
    private static List<PhoneContact> applyPhoneDefaults(List<PhoneContact> phones, User owner) {
        return phones.stream()
                .map(p -> new PhoneContact(
                        p.phone(),
                        StringUtils.hasText(p.name()) ? p.name() : owner.getName(),
                        StringUtils.hasText(p.email()) ? p.email() : owner.getEmail()))
                .toList();
    }

    /** Writes a property's location (shared by all listings at the address). */
    public void applyPropertyLocation(Property property, Location loc) {
        property.setDistrictSlug(loc.district());
        property.setCitySlug(loc.city());
        property.setAddress(loc.address());
        property.setArBuildingCode(loc.arBuildingCode());
        property.setArStreetCode(loc.arStreetCode());
        property.setApartment(loc.apartment());
        property.setCadastreParcelNr(loc.cadastreParcelNr());
        property.setLat(loc.coords().lat());
        property.setLng(loc.coords().lng());
    }

    public void applyTranslations(Listing listing, Map<String, LocalizedText> translations) {
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

    /** Sorted list for a response (null when empty, so {@code NON_NULL} drops it). */
    private static <T extends Enum<T>> List<T> sorted(Set<T> values) {
        return values.isEmpty() ? null : values.stream().sorted().toList();
    }

    /** Replaces a managed collection's contents in place (clear + addAll), tolerating a null source. */
    private static <T> void replace(Set<T> target, List<T> source) {
        target.clear();
        if (source != null) target.addAll(source);
    }

    private static PropertyDetails toFullDetailsDto(Listing l) {
        return PropertyDetails.builder()
                .rooms(l.getRooms())
                .bedrooms(l.getBedrooms())
                .bathrooms(l.getBathrooms())
                .bathroomLayout(l.getBathroomLayout())
                .m2(l.getM2())
                .landM2(l.getLandM2())
                .floor(l.getFloor())
                .totalFloors(l.getTotalFloors())
                .yearBuilt(l.getYearBuilt())
                .heating(l.getHeating())
                .energyClass(l.getEnergyClass())
                .maintenanceCost(l.getMaintenanceCost())
                .sewage(l.getSewage())
                .ventilation(l.getVentilation())
                .roof(l.getRoof())
                .build();
    }

    private static Media toMediaDto(Listing l) {
        List<String> photos = l.getPhotos();
        List<String> plans = l.getPlans();
        return Media.builder()
                .photos(photos.isEmpty() ? null : photos)
                .plans(plans.isEmpty() ? null : plans)
                .videoUrl(l.getVideoUrl())
                .websiteUrl(l.getWebsiteUrl())
                .build();
    }

    private static Location toLocation(Property p) {
        return Location.builder()
                .district(p.getDistrictSlug())
                .city(p.getCitySlug())
                .address(p.getAddress())
                .arBuildingCode(p.getArBuildingCode())
                .apartment(p.getApartment())
                .cadastreParcelNr(p.getCadastreParcelNr())
                .coords(new CoordsDto(p.getLat(), p.getLng()))
                .build();
    }

    private static void applyDetails(Listing listing, PropertyDetails details) {
        listing.setRooms(details.rooms());
        listing.setBedrooms(details.bedrooms());
        listing.setBathrooms(details.bathrooms());
        listing.setBathroomLayout(details.bathroomLayout());
        listing.setM2(details.m2());
        listing.setLandM2(details.landM2());
        listing.setFloor(details.floor());
        listing.setTotalFloors(details.totalFloors());
        listing.setYearBuilt(details.yearBuilt());
        listing.setHeating(details.heating());
        listing.setEnergyClass(details.energyClass());
        listing.setMaintenanceCost(details.maintenanceCost());
        listing.setSewage(details.sewage());
        listing.setVentilation(details.ventilation());
        listing.setRoof(details.roof());
    }

    private static void applyMedia(Listing listing, Media media) {
        listing.setPhotos(nullToEmpty(media != null ? media.photos() : null));
        listing.setPlans(nullToEmpty(media != null ? media.plans() : null));
        listing.setVideoUrl(media != null ? media.videoUrl() : null);
        listing.setWebsiteUrl(media != null ? media.websiteUrl() : null);
    }

    private static <T> List<T> nullToEmpty(List<T> values) {
        return values != null ? new ArrayList<>(values) : new ArrayList<>();
    }
}
