package com.app.backend.dto.property.response;

import java.util.List;

public record PropertyPageResponse(List<PropertyListItemDto> items, long total) {}
