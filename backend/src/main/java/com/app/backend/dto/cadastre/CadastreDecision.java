package com.app.backend.dto.cadastre;

import com.app.backend.enums.PropertyStatus;

/**
 * Outcome of the create/edit-time cadastre cross-check: the moderation
 * {@code status} to store, plus whether the listing positively matched the
 * official registry ({@code verified} — at least one figure was comparable and
 * all comparable figures agreed). A fail-open pass with nothing to compare is
 * {@code ACTIVE} but not verified.
 */
public record CadastreDecision(PropertyStatus status, boolean verified) {}
