package com.asg.shipping.receipts.dto;

import com.asg.common.lib.dto.LovGetListDto;
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

    private List<ContainerResult> containerResults;
    private List<ChargeDetail> charges;
    private BigDecimal totalAmount;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ContainerResult {
        private String containerNo;
        private BigDecimal demurrageAmount;
        private Long demurrageDays;
        private BigDecimal taxAmount;
        private Long taxPoid;
        private LovGetListDto taxDet;
        private BigDecimal taxPercentage;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ChargeDetail {
        private String chargeType;
        private Long chargePoid;
        private LovGetListDto chargeDet;
        private BigDecimal amount;
        private Long taxPoid;
        private LovGetListDto taxDet;
        private BigDecimal taxPercentage;
        private BigDecimal taxAmount;
    }
}
