package com.app.backend.controller;

import com.app.backend.dto.address.BuildingOptionDto;
import com.app.backend.dto.address.StreetOptionDto;
import com.app.backend.service.AddressRegistryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public typeahead over the State Address Register mirror (strict address selection in listing forms). */
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressRegistryQueryService queryService;

    @GetMapping("/streets")
    public List<StreetOptionDto> streets(
            @RequestParam String city,
            @RequestParam String district,
            @RequestParam(defaultValue = "") String q
    ) {
        return queryService.searchStreets(city, district, q);
    }

    @GetMapping("/buildings")
    public List<BuildingOptionDto> buildings(
            @RequestParam long streetCode,
            @RequestParam(defaultValue = "") String q
    ) {
        return queryService.searchBuildings(streetCode, q);
    }
}
