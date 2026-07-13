package com.app.backend.dto.address;

/**
 * A register apartment (VZD "dzīvoklis") as mirrored into
 * {@code address_apartments}, keyed by its own VAR address code and pointing
 * at the parent building. Links a listing's free-typed apartment number to
 * an official VAR code, which the cadastre mirror's premise groups reference.
 */
public record ApartmentRow(
        long code,
        long buildingCode,
        String name,
        String normName
) {}
