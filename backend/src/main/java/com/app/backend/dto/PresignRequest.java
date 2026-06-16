package com.app.backend.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PresignRequest(@NotEmpty List<String> filenames) {}
