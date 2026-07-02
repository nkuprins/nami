package com.app.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PresignRequest(@NotEmpty @Size(max = 30) List<@NotBlank String> filenames) {}
