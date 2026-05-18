package com.asg.shipping.receipts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptSaveResponseDto {
    private String docRef;
    private Long transactionPoid;
    private String message;
}
