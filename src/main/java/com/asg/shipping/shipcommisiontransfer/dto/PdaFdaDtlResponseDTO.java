package com.asg.shipping.shipcommisiontransfer.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PdaFdaDtlResponseDTO {

    private Long detRowId;
    private Long charge;
    private String currencyCode;
    private BigDecimal currencyRate;
    private String remarks;
    private BigDecimal fdaAmount;
    private String chargeName;
}
