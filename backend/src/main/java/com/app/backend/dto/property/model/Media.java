package com.app.backend.dto.property.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Media(
        @Size(max = 30) List<@NotBlank String> photos,
        @Size(max = 3) List<@NotBlank String> plans,
        String videoUrl
) {}
