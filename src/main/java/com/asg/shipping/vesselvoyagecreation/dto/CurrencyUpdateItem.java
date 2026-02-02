package com.asg.shipping.vesselvoyagecreation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyUpdateItem {
    @NotBlank
    private String currencyCode;

    @NotNull
    private Double newCurrencyExchange;
}










