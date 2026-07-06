package com.app.backend.dto.property.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocalizedText(String title, String description) {}
