package com.app.backend.dto.address;

/** Row counts loaded by one address-register ingest run. */
public record AddressIngestStats(int territories, int streets, int buildings) {}
