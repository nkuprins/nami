package com.app.backend.controller;

import com.app.backend.dto.cadastre.OfficialBuilding;
import com.app.backend.dto.cadastre.OfficialParcel;
import com.app.backend.service.CadastreQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Official VZD-cadastre figures for a selected address, used to auto-fill the listing form. */
@RestController
@RequestMapping("/api/cadastre")
@RequiredArgsConstructor
public class CadastreController {

    private final CadastreQueryService cadastreQueryService;

    @GetMapping("/building")
    public OfficialBuilding building(
            @RequestParam long buildingCode,
            @RequestParam(defaultValue = "") String apartment
    ) {
        return cadastreQueryService.lookupBuilding(buildingCode, apartment);
    }

    @GetMapping("/parcel")
    public OfficialParcel parcel(@RequestParam String parcelNr) {
        return cadastreQueryService.lookupParcel(parcelNr);
    }
}
