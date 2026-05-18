package com.asg.shipping.receipts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiptCalculateDemurrageRequestDto {
    private Long blPoid;
    private Long transactionPoid;
    private List<ContainerRequest> containers;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ContainerRequest {
        private String containerNo;
        private String equipmentIsoType;
        private LocalDate fromDate;
        private LocalDate toDate;
        private Long extraFreeDays;
    }
}
