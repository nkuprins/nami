package com.app.backend.service;

import com.app.backend.dto.CreatePropertyRequest;
import com.app.backend.dto.PropertyFilter;
import com.app.backend.dto.PropertyItemDto;
import com.app.backend.dto.PropertyListItemDto;
import com.app.backend.dto.PropertyPageResponse;
import com.app.backend.dto.UpdatePropertyRequest;
import com.app.backend.entity.Property;
import com.app.backend.entity.PropertyPhone;
import com.app.backend.entity.PropertyPhoto;
import com.app.backend.entity.User;
import com.app.backend.enums.*;
import com.app.backend.mapper.PropertyMapper;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.spec.PropertySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
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

        PropertyCompletion completionEnum = (filter.completion() != null && !filter.completion().isBlank())
                ? PropertyCompletion.fromDbValue(filter.completion())
                : null;

        Specification<Property> spec = PropertySpec.build(
                listingType, filter.loc(), filter.priceMin(), filter.priceMax(), filter.rooms(),
                filter.m2Min(), filter.m2Max(), filter.floorMin(), filter.floorMax(),
                filter.notGround(), filter.notTop(), filter.yearMin(), filter.yearMax(),
                featureEnums, completionEnum
        );

        if ("price-per-m2-asc".equals(sort)) {
            List<Property> all = propertyRepository.findAll(spec);
            all.sort(Comparator.comparing(p -> p.getPrice().divide(p.getM2(), 10, RoundingMode.HALF_UP)));
            int start = (page - 1) * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, all.size());
            List<PropertyListItemDto> items = (start >= all.size())
                    ? List.of()
                    : all.subList(start, end).stream().map(propertyMapper::toListDto).toList();
            return new PropertyPageResponse(items, all.size());
        }

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

        if (isBlank(req.titleLv()) && isBlank(req.titleEn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one title language is required");
        }
        if (isBlank(req.descriptionLv()) && isBlank(req.descriptionEn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one description language is required");
        }

        property.setTitleLv(blankToNull(req.titleLv()));
        property.setTitleEn(blankToNull(req.titleEn()));
        property.setDescriptionLv(blankToNull(req.descriptionLv()));
        property.setDescriptionEn(blankToNull(req.descriptionEn()));
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

        property.setVideoUrl(req.videoUrl());

        if (req.phones() != null) {
            for (int i = 0; i < req.phones().size(); i++) {
                PropertyPhone phone = new PropertyPhone();
                phone.setProperty(property);
                phone.setPhone(req.phones().get(i));
                phone.setPosition((short) i);
                property.getPhones().add(phone);
            }
        }

        return propertyMapper.toDto(propertyRepository.save(property));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s.trim();
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

        if (isBlank(req.titleLv()) && isBlank(req.titleEn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one title language is required");
        }
        if (isBlank(req.descriptionLv()) && isBlank(req.descriptionEn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one description language is required");
        }

        property.setTitleLv(blankToNull(req.titleLv()));
        property.setTitleEn(blankToNull(req.titleEn()));
        property.setDescriptionLv(blankToNull(req.descriptionLv()));
        property.setDescriptionEn(blankToNull(req.descriptionEn()));
        property.setPrice(req.price());
        property.setRooms(req.rooms());
        property.setM2(req.m2());
        property.setLandM2(req.landM2());
        property.setFloor(req.floor());
        property.setTotalFloors(req.totalFloors());
        property.setYearBuilt(req.yearBuilt());
        property.setVideoUrl(req.videoUrl());

        if (req.completion() != null && !req.completion().isBlank()) {
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
        if (req.phones() != null) {
            for (int i = 0; i < req.phones().size(); i++) {
                PropertyPhone phone = new PropertyPhone();
                phone.setProperty(property);
                phone.setPhone(req.phones().get(i));
                phone.setPosition((short) i);
                property.getPhones().add(phone);
            }
        }

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

        propertyRepository.delete(property);

        if (!photoUrls.isEmpty()) {
            try {
                uploadService.deleteObjects(photoUrls);
            } catch (Exception e) {
                log.warn("Failed to delete S3 objects for property {}: {}", propertyId, e.getMessage());
            }
        }
    }

    private Sort buildSort(String sort) {
        return switch (sort) {
            case "price-asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "m2-desc" -> Sort.by(Sort.Direction.DESC, "m2");
            default -> Sort.by(Sort.Direction.DESC, "postedAt");
        };
    }
}
