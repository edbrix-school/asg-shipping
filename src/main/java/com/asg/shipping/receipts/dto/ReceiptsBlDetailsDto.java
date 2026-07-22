package com.asg.shipping.receipts.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiptsBlDetailsDto {

    private String docRef;
    private LocalDate date;
    private Long blPoid;
    private LovGetListDto blDet;
    private Long companyPoid;
    private LovGetListDto companyDet;
    private String releaseType;
    private String originalReleaseType;
    private Long printDoCustomerPoid;
    private LovGetListDto printDoCustomerDet;
    private Long chequeCompany;
    private LovGetListDto chequeCompanyDet;
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
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;



}
