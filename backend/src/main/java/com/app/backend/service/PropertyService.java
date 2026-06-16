package com.app.backend.service;

import com.app.backend.dto.*;
import com.app.backend.entity.*;
import com.app.backend.enums.*;
import com.app.backend.repository.*;
import com.app.backend.spec.PropertySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private static final int PAGE_SIZE = 12;

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PropertyPageResponse list(
            String type,
            List<String> loc,
            BigDecimal priceMin, BigDecimal priceMax,
            List<Integer> rooms,
            BigDecimal m2Min, BigDecimal m2Max,
            Integer floorMin, Integer floorMax,
            Boolean notGround, Boolean notTop,
            Integer yearMin, Integer yearMax,
            List<String> features,
            String completion,
            String sort,
            int page
    ) {
        ListingType listingType = ListingType.fromDbValue(type);

        List<PropertyFeature> featureEnums = (features == null || features.isEmpty())
                ? List.of()
                : features.stream().map(PropertyFeature::fromDbValue).toList();

        PropertyCompletion completionEnum = (completion != null && !completion.isBlank())
                ? PropertyCompletion.fromDbValue(completion)
                : null;

        Specification<Property> spec = PropertySpec.build(
                listingType, loc, priceMin, priceMax, rooms,
                m2Min, m2Max, floorMin, floorMax, notGround, notTop,
                yearMin, yearMax, featureEnums, completionEnum
        );

        if ("price-per-m2-asc".equals(sort)) {
            List<Property> all = propertyRepository.findAll(spec);
            all.sort(Comparator.comparing(p -> p.getPrice().divide(p.getM2(), 10, RoundingMode.HALF_UP)));
            int start = (page - 1) * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, all.size());
            List<PropertyItemDto> items = (start >= all.size())
                    ? List.of()
                    : all.subList(start, end).stream().map(this::toDto).toList();
            return new PropertyPageResponse(items, all.size());
        }

        PageRequest pageRequest = PageRequest.of(page - 1, PAGE_SIZE, buildSort(sort));
        Page<Property> result = propertyRepository.findAll(spec, pageRequest);
        List<PropertyItemDto> items = result.getContent().stream().map(this::toDto).toList();
        return new PropertyPageResponse(items, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PropertyItemDto getById(UUID id) {
        return propertyRepository.findById(id)
                .filter(p -> p.getStatus() == PropertyStatus.ACTIVE)
                .map(this::toDto)
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
        property.setTitle(req.title());
        property.setDescription(req.description());
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

        if (req.completion() != null && !req.completion().isBlank()) {
            property.setCompletion(PropertyCompletion.fromDbValue(req.completion()));
        }

        if (req.features() != null) {
            Set<PropertyFeature> featureSet = req.features().stream()
                    .map(PropertyFeature::fromDbValue)
                    .collect(Collectors.toSet());
            property.setFeatures(featureSet);
        }

        if (req.photos() != null) {
            for (int i = 0; i < req.photos().size(); i++) {
                PropertyPhoto photo = new PropertyPhoto();
                photo.setProperty(property);
                photo.setUrl(req.photos().get(i));
                photo.setPosition((short) i);
                property.getPhotos().add(photo);
            }
        }

        return toDto(propertyRepository.save(property));
    }

    private Sort buildSort(String sort) {
        return switch (sort) {
            case "price-asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "m2-desc" -> Sort.by(Sort.Direction.DESC, "m2");
            default -> Sort.by(Sort.Direction.DESC, "postedAt");
        };
    }

    private PropertyItemDto toDto(Property p) {
        List<String> photos = p.getPhotos().stream()
                .map(PropertyPhoto::getUrl)
                .toList();
        List<String> features = p.getFeatures().stream()
                .map(PropertyFeature::getDbValue)
                .toList();
        return new PropertyItemDto(
                p.getId().toString(),
                p.getListingType().getDbValue(),
                p.getPropertyCategory().getDbValue(),
                p.getTitle(),
                p.getDescription(),
                p.getPrice(),
                p.getRooms(),
                p.getM2(),
                p.getLandM2(),
                p.getFloor(),
                p.getTotalFloors(),
                p.getYearBuilt(),
                features,
                p.getDistrictSlug(),
                p.getCitySlug(),
                p.getAddress(),
                new CoordsDto(p.getLat(), p.getLng()),
                photos,
                p.getPostedAt().toString(),
                p.getCompletion() != null ? p.getCompletion().getDbValue() : null
        );
    }
}
