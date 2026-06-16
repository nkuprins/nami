package com.app.backend.dto;

import java.util.List;

public record PropertyPageResponse(List<PropertyItemDto> items, long total) {}
