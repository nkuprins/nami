package com.app.backend.dto;

import java.util.List;

public record PropertyPageResponse(List<PropertyListItemDto> items, long total) {}
