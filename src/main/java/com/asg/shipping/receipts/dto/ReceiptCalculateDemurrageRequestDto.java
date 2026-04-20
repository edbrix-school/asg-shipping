package com.asg.shipping.receipts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiptCalculateDemurrageRequestDto {

    private Long transactionPoid;
    private Long blPoid;
    private String containerNo;
    private String containerIsoType;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private Long extraFreeDays;


}
