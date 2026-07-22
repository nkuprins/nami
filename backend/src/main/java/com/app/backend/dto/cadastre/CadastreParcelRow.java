package com.app.backend.dto.cadastre;

import com.app.backend.enums.LandUse;

import java.math.BigDecimal;

/** A cadastre land-parcel record as mirrored into {@code cadastre_parcels}. */
public record CadastreParcelRow(String cadastreNr, BigDecimal areaM2, LandUse landUse) {}
