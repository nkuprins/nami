package com.app.backend.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record PropertyFilter(
        @NotBlank String type,
        List<String> loc,
        BigDecimal priceMin,
        BigDecimal priceMax,
        List<Integer> rooms,
        BigDecimal m2Min,
        BigDecimal m2Max,
        Short floorMin,
        Short floorMax,
        Boolean notGround,
        Boolean notTop,
        Short yearMin,
        Short yearMax,
        List<String> features,
        String completion
) {}
