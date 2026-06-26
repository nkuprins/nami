package com.app.backend.service;

import com.app.backend.dto.CreatePropertyRequest;
import com.app.backend.dto.PropertyFilter;
import com.app.backend.dto.PropertyItemDto;
import com.app.backend.dto.PropertyListItemDto;
import com.app.backend.dto.PropertyPageResponse;
import com.app.backend.dto.UpdatePropertyRequest;
import com.app.backend.entity.Property;
import com.app.backend.entity.Property_;
import com.app.backend.entity.PropertyPhone;
import com.app.backend.entity.PropertyPhoto;
import com.app.backend.entity.PropertyPlan;
import com.app.backend.entity.PropertyTranslation;
import com.app.backend.entity.User;
import com.app.backend.enums.*;
import com.app.backend.mapper.PropertyMapper;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.spec.PropertySearchCriteria;
import com.app.backend.spec.PropertySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyService {

    private static final int PAGE_SIZE = 12;

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertyMapper propertyMapper;
    private final UploadService uploadService;

    @Transactional(readOnly = true)
    public PropertyPageResponse list(PropertyFilter filter, String sort, int page) {
        ListingType listingType = ListingType.fromDbValue(filter.type());

        List<PropertyFeature> featureEnums = (filter.features() == null || filter.features().isEmpty())
                ? List.of()
                : filter.features().stream().map(PropertyFeature::fromDbValue).toList();

        PropertyCompletion completionEnum = StringUtils.hasText(filter.completion())
                ? PropertyCompletion.fromDbValue(filter.completion())
                : null;

        PropertySearchCriteria criteria = new PropertySearchCriteria(
                listingType,
                parseLocFilter(filter.loc()),
                filter.priceMin(), filter.priceMax(),
                filter.rooms(),
                filter.m2Min(), filter.m2Max(),
                filter.floorMin(), filter.floorMax(),
                filter.notGround(), filter.notTop(),
                filter.yearMin(), filter.yearMax(),
                featureEnums, completionEnum
        );
        Specification<Property> spec = PropertySpec.build(criteria);

        PageRequest pageRequest = PageRequest.of(page - 1, PAGE_SIZE, buildSort(sort));
        Page<Property> result = propertyRepository.findAll(spec, pageRequest);
        List<PropertyListItemDto> items = result.getContent().stream().map(propertyMapper::toListDto).toList();
        return new PropertyPageResponse(items, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<PropertyListItemDto> listByOwner(UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return propertyRepository.findByOwner(owner).stream()
                .map(propertyMapper::toListDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PropertyItemDto getById(UUID id) {
        return propertyRepository.findById(id)
                .filter(p -> p.getStatus() == PropertyStatus.ACTIVE)
                .map(propertyMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public PropertyItemDto create(CreatePropertyRequest req, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Property property = new Property();
        property.setOwner(owner);
        property.setListingType(ListingType.fromDbValue(req.type()));
        property.setPropertyCategory(PropertyCategory.fromDbValue(req.propertyKind()));

        applyTranslations(property, req.titleLv(), req.titleEn(), req.titleRu(),
                req.descriptionLv(), req.descriptionEn(), req.descriptionRu());
        property.setPrice(req.price());
        property.setRooms(req.rooms());
        property.setM2(req.m2());
        property.setLandM2(req.landM2());
        property.setFloor(req.floor());
        property.setTotalFloors(req.totalFloors());
        property.setYearBuilt(req.yearBuilt());
        property.setDistrictSlug(req.district());
        property.setCitySlug(req.city());
        property.setAddress(req.address());
        property.setLat(req.coords().lat());
        property.setLng(req.coords().lng());
        property.setStatus(PropertyStatus.ACTIVE);

        if (StringUtils.hasText(req.completion())) {
            property.setCompletion(PropertyCompletion.fromDbValue(req.completion()));
        }

        if (req.features() != null) {
            Set<PropertyFeature> featureSet = req.features().stream()
                    .map(PropertyFeature::fromDbValue)
                    .collect(Collectors.toSet());
            property.setFeatures(featureSet);
        }

        addPhotos(property, req.photos());
        addPlans(property, req.plans());
        property.setVideoUrl(req.videoUrl());
        addPhones(property, req.phones());

        Property saved = propertyRepository.save(property);
        log.info("Property created: {} by owner: {}", saved.getId(), ownerId);
        return propertyMapper.toDto(saved);
    }

    private static void applyTranslations(Property property,
                                           String titleLv, String titleEn, String titleRu,
                                           String descLv, String descEn, String descRu) {
        List<String[]> candidates = List.of(
                new String[]{SupportedLocale.LV.code, titleLv, descLv},
                new String[]{SupportedLocale.EN.code, titleEn, descEn},
                new String[]{SupportedLocale.RU.code, titleRu, descRu}
        );
        boolean anyValid = candidates.stream()
                .anyMatch(c -> StringUtils.hasText(c[1]) && StringUtils.hasText(c[2]));
        if (!anyValid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At least one complete translation (title + description) is required");
        }
        property.getTranslations().clear();
        for (String[] c : candidates) {
            if (StringUtils.hasText(c[1]) && StringUtils.hasText(c[2])) {
                PropertyTranslation pt = new PropertyTranslation();
                pt.setProperty(property);
                pt.setLocale(c[0]);
                pt.setTitle(c[1].trim());
                pt.setDescription(c[2].trim());
                property.getTranslations().put(c[0], pt);
            }
        }
    }

    private static Map<String, List<String>> parseLocFilter(List<String> loc) {
        if (loc == null || loc.isEmpty()) return Map.of();
        Map<String, List<String>> byCity = new HashMap<>();
        for (String entry : loc) {
            int colon = entry.indexOf(':');
            if (colon <= 0 || colon >= entry.length() - 1) continue;
            byCity.computeIfAbsent(entry.substring(0, colon), k -> new ArrayList<>())
                    .add(entry.substring(colon + 1));
        }
        return byCity;
    }

    @Transactional
    public PropertyItemDto update(UUID propertyId, UpdatePropertyRequest req, UUID ownerId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!property.getOwner().getId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        property.setListingType(ListingType.fromDbValue(req.type()));
        property.setPropertyCategory(PropertyCategory.fromDbValue(req.propertyKind()));

        applyTranslations(property, req.titleLv(), req.titleEn(), req.titleRu(),
                req.descriptionLv(), req.descriptionEn(), req.descriptionRu());
        property.setPrice(req.price());
        property.setRooms(req.rooms());
        property.setM2(req.m2());
        property.setLandM2(req.landM2());
        property.setFloor(req.floor());
        property.setTotalFloors(req.totalFloors());
        property.setYearBuilt(req.yearBuilt());
        property.setVideoUrl(req.videoUrl());

        if (StringUtils.hasText(req.completion())) {
            property.setCompletion(PropertyCompletion.fromDbValue(req.completion()));
        } else {
            property.setCompletion(null);
        }

        property.getFeatures().clear();
        if (req.features() != null) {
            Set<PropertyFeature> featureSet = req.features().stream()
                    .map(PropertyFeature::fromDbValue)
                    .collect(Collectors.toSet());
            property.setFeatures(featureSet);
        }

        property.getPhones().clear();
        addPhones(property, req.phones());

        return propertyMapper.toDto(propertyRepository.save(property));
    }

    @Transactional
    public void delete(UUID propertyId, UUID ownerId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!property.getOwner().getId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<String> photoUrls = property.getPhotos().stream()
                .map(PropertyPhoto::getUrl)
                .toList();
        List<String> planUrls = property.getPlans().stream()
                .map(PropertyPlan::getUrl)
                .toList();

        propertyRepository.delete(property);
        log.info("Property deleted: {} by owner: {}", propertyId, ownerId);

        List<String> allMediaUrls = new java.util.ArrayList<>(photoUrls);
        allMediaUrls.addAll(planUrls);
        if (!allMediaUrls.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        uploadService.deleteObjects(allMediaUrls);
                    } catch (Exception e) {
                        log.warn("Failed to delete S3 objects for property {}: {}", propertyId, e.getMessage());
                    }
                }
            });
        }
    }

    private static void addPhotos(Property property, List<String> urls) {
        if (urls == null) return;
        for (int i = 0; i < urls.size(); i++) {
            PropertyPhoto photo = new PropertyPhoto();
            photo.setProperty(property);
            photo.setUrl(urls.get(i));
            photo.setPosition((short) i);
            property.getPhotos().add(photo);
        }
    }

    private static void addPlans(Property property, List<String> urls) {
        if (urls == null) return;
        for (int i = 0; i < urls.size(); i++) {
            PropertyPlan plan = new PropertyPlan();
            plan.setProperty(property);
            plan.setUrl(urls.get(i));
            plan.setPosition((short) i);
            property.getPlans().add(plan);
        }
    }

    private static void addPhones(Property property, List<String> numbers) {
        if (numbers == null) return;
        for (int i = 0; i < numbers.size(); i++) {
            PropertyPhone phone = new PropertyPhone();
            phone.setProperty(property);
            phone.setPhone(numbers.get(i));
            phone.setPosition((short) i);
            property.getPhones().add(phone);
        }
    }

    private static Sort buildSort(String sort) {
        if (sort == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort parameter is required");
        return switch (sort) {
            case "newest" -> Sort.by(Sort.Direction.DESC, Property_.POSTED_AT);
            case "price-asc" -> Sort.by(Sort.Direction.ASC, Property_.PRICE);
            case "price-desc" -> Sort.by(Sort.Direction.DESC, Property_.PRICE);
            case "m2-desc" -> Sort.by(Sort.Direction.DESC, Property_.M2);
            case "price-per-m2-asc" -> Sort.by(Sort.Direction.ASC, Property_.PRICE_PER_M2);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown sort option: " + sort);
        };
    }
}
