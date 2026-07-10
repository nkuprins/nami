package com.app.backend.controller;

import com.app.backend.dto.property.model.LocalizedText;
import com.app.backend.dto.property.request.*;
import com.app.backend.dto.property.response.*;
import com.app.backend.enums.ListingType;
import com.app.backend.service.ListingQueryService;
import com.app.backend.service.ListingService;
import com.app.backend.service.PropertyQueryService;
import com.app.backend.service.PropertyService;
import com.app.backend.service.TurnstileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;
    private final ListingService listingService;
    private final PropertyQueryService propertyQueryService;
    private final ListingQueryService listingQueryService;
    private final TurnstileService turnstileService;

    @GetMapping
    public PropertyPageResponse list(
            @Valid PropertyFilter filter,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "1") int page
    ) {
        return listingQueryService.list(filter, sort, page);
    }

    @GetMapping("/counts")
    public PropertyCategoryCountsDto counts(@RequestParam ListingType type) {
        return listingQueryService.countsByType(type);
    }

    @GetMapping("/mine")
    public List<PropertyListItemDto> mine(@AuthenticationPrincipal UUID userId) {
        return listingQueryService.listByOwner(userId);
    }

    @GetMapping("/{id}")
    public PropertyItemDto getById(
            @PathVariable UUID id,
            @RequestParam(required = false) String locale
    ) {
        return listingQueryService.getById(id, locale);
    }

    @GetMapping("/{id}/translations/{locale}")
    public LocalizedText getTranslation(@PathVariable UUID id, @PathVariable String locale) {
        return listingQueryService.getTranslation(id, locale);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyItemDto create(
            @AuthenticationPrincipal UUID userId,
            @RequestHeader(value = "X-Turnstile-Token", required = false) String turnstileToken,
            @RequestBody @Valid CreatePropertyRequest request,
            HttpServletRequest httpRequest
    ) {
        turnstileService.verify(turnstileToken, clientIp(httpRequest));
        return propertyService.create(request, userId);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/{propertyId}/listings")
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyItemDto addListing(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID propertyId,
            @RequestBody @Valid AddListingRequest request
    ) {
        return listingService.addListing(propertyId, request, userId);
    }

    @PutMapping("/{id}")
    public PropertyItemDto updateListing(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @RequestBody @Valid UpdateListingRequest request
    ) {
        return listingService.updateListing(id, request, userId);
    }

    @GetMapping("/{propertyId}/property")
    public PropertyDto getProperty(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID propertyId
    ) {
        return propertyQueryService.getProperty(propertyId, userId);
    }

    @PutMapping("/{propertyId}/property")
    public PropertyDto updateProperty(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID propertyId,
            @RequestBody @Valid UpdatePropertyRequest request
    ) {
        return propertyService.updateProperty(propertyId, request, userId);
    }

    @PostMapping("/{id}/renew")
    public PropertyItemDto renew(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @RequestBody @Valid RenewPropertyRequest request
    ) {
        return listingService.renew(id, request, userId);
    }

    @PostMapping("/{id}/reprocess-images")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reprocessImages(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id
    ) {
        propertyService.reprocessImages(id, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id
    ) {
        listingService.deleteListing(id, userId);
    }

    @DeleteMapping("/{propertyId}/listings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProperty(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID propertyId
    ) {
        propertyService.deleteProperty(propertyId, userId);
    }
}
