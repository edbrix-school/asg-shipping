package com.asg.shipping.receipts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiptCalculateDemurrageResponseDto {

    private BigDecimal demurrageAmount;
    private BigDecimal demurrageTaxAmount;
    private Long demurrageTaxPoid;
    private BigDecimal demurrageTaxPercentage;
    private List<ChargeDetail> lateCollectionCharges;
    private List<ChargeDetail> revalidationCharges;
    private BigDecimal totalAmount;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ChargeDetail {
        private String chargeType;
        private Long chargePoid;
        private BigDecimal amount;
        private Long taxPoid;
        private BigDecimal taxPercentage;
        private BigDecimal taxAmount;
    }
}
