package com.app.backend.dto.cadastre;

import java.math.BigDecimal;

/** Official building/apartment figures for listing-form auto-fill; fields null when unrecorded. */
public record OfficialBuilding(Short yearBuilt, BigDecimal area) {}
