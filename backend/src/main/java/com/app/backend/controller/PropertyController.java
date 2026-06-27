package com.app.backend.controller;

import com.app.backend.dto.*;
import com.app.backend.service.PropertyService;
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

    @GetMapping
    public PropertyPageResponse list(
            @Valid PropertyFilter filter,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "1") int page
    ) {
        return propertyService.list(filter, sort, page);
    }

    @GetMapping("/mine")
    public List<PropertyListItemDto> mine(@AuthenticationPrincipal UUID userId) {
        return propertyService.listByOwner(userId);
    }

    @GetMapping("/{id}")
    public PropertyItemDto getById(@PathVariable UUID id) {
        return propertyService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyItemDto create(
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid CreatePropertyRequest request
    ) {
        return propertyService.create(request, userId);
    }

    @PutMapping("/{id}")
    public PropertyItemDto update(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @RequestBody @Valid UpdatePropertyRequest request
    ) {
        return propertyService.update(id, request, userId);
    }

    @PostMapping("/{id}/renew")
    public PropertyItemDto renew(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @RequestBody @Valid RenewPropertyRequest request
    ) {
        return propertyService.renew(id, request, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id
    ) {
        propertyService.delete(id, userId);
    }
}
