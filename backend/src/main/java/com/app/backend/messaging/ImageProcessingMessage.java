package com.app.backend.messaging;

import java.util.UUID;

public record ImageProcessingMessage(UUID propertyId, String cdnUrl) {}
