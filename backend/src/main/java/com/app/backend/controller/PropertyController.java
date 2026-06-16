package com.app.backend.controller;

import com.app.backend.dto.*;
import com.app.backend.service.PropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping
    public PropertyPageResponse list(
            @RequestParam String type,
            @RequestParam(required = false, defaultValue = "") List<String> loc,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false, defaultValue = "") List<Integer> rooms,
            @RequestParam(required = false) BigDecimal m2Min,
            @RequestParam(required = false) BigDecimal m2Max,
            @RequestParam(required = false) Integer floorMin,
            @RequestParam(required = false) Integer floorMax,
            @RequestParam(required = false) Boolean notGround,
            @RequestParam(required = false) Boolean notTop,
            @RequestParam(required = false) Integer yearMin,
            @RequestParam(required = false) Integer yearMax,
            @RequestParam(required = false, defaultValue = "") List<String> features,
            @RequestParam(required = false) String completion,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "1") int page
    ) {
        return propertyService.list(type, loc, priceMin, priceMax, rooms,
                m2Min, m2Max, floorMin, floorMax, notGround, notTop,
                yearMin, yearMax, features, completion, sort, page);
    }

    @GetMapping("/{id}")
    public PropertyItemDto getById(@PathVariable UUID id) {
        return propertyService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyItemDto create(
            @RequestHeader("X-User-Id") UUID ownerId,
            @RequestBody CreatePropertyRequest request
    ) {
        return propertyService.create(request, ownerId);
    }
}
