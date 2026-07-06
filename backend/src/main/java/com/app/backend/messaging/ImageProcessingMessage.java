package com.app.backend.messaging;

import java.util.UUID;

/** A single image to process, carried on the queue as a reference (never the bytes). */
public record ImageProcessingMessage(UUID propertyId, String cdnUrl) {}
