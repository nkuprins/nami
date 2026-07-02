package com.app.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Price(
        @NotNull @DecimalMin("0.01") @DecimalMax("999999999999.99") @Digits(integer = 12, fraction = 2) BigDecimal amount,
        Boolean vatIncluded
) {}
