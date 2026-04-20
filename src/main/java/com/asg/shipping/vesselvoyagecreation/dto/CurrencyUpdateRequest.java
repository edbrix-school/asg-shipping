package com.asg.shipping.vesselvoyagecreation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyUpdateRequest {
    @Valid
    @NotEmpty
    private List<CurrencyUpdateItem> items;
}










