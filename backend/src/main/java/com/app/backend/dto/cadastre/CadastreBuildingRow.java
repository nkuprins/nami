package com.app.backend.dto.cadastre;

/** A cadastre building record as mirrored into {@code cadastre_buildings}. */
public record CadastreBuildingRow(String cadastreNr, Long arBuildingCode, Short yearBuilt) {}
