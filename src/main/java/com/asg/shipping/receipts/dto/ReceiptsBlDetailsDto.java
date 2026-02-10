package com.asg.shipping.receipts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiptsBlDetailsDto {

    private String docRef;
    private LocalDateTime date;
    private Long blPoid;
    private Long companyPoid;
    private String releaseType;
    private String originalReleaseType;
    private Long printDoCustomerPoid;
    private Long chequeCompany;
    private String cpr;
    private String name;
    private String contact;
    private String paymentReference;
    private String remarks;
    private Long token;
    private Long amount;
    private List<ReceiptContainerDto> container;
    private List<ReceiptCharges> charges;
    private List<ReceiptPaymentDetailDto> paymentDetail;



}
