package com.app.backend.entity;

import java.io.Serializable;
import java.util.UUID;

public record ListingTranslationId(UUID listing, String locale) implements Serializable {}
