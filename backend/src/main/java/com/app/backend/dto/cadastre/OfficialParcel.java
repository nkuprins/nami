package com.app.backend.dto.cadastre;

import com.app.backend.enums.LandUse;

import java.math.BigDecimal;

/** Official parcel figures for listing-form auto-fill; fields null when unrecorded. */
public record OfficialParcel(BigDecimal areaM2, LandUse landUse) {}
