package com.app.backend.dto.cadastre;

/** Row counts loaded by one cadastre-mirror ingest run. */
public record CadastreIngestStats(int buildings, int premises, int parcels) {}
