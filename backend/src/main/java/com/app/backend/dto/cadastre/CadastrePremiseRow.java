package com.app.backend.dto.cadastre;

import java.math.BigDecimal;

/** A cadastre premise-group (apartment) record as mirrored into {@code cadastre_premises}. */
public record CadastrePremiseRow(String cadastreNr, Long arCode, BigDecimal areaM2) {}
