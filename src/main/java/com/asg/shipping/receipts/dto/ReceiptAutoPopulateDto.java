package com.asg.shipping.receipts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiptAutoPopulateDto {

    private ReceiptBlAutoPopulateDto blDetails;
    private List<ReceiptAutoPopulateContainerDto> container;
    private List<ReceiptAutoPopulateChargeDto> charges;

}
