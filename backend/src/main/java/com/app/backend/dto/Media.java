package com.app.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Media(
        List<@NotBlank String> photos,
        List<@NotBlank String> plans,
        String videoUrl
) {}
