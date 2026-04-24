package com.asg.shipping.receipts.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptBlAutoPopulateDto {

    private Long blPoid;
    private LovGetListDto blDet;
    private Long companyPoid;
    private LovGetListDto companyDet;
    private String blReleaseType;
    private LovGetListDto blReleaseTypeDet;
    private String originalBlReleaseType;
    private LovGetListDto originalBlReleaseTypeDet;
    private BigDecimal printCustomerPoid;
    private LovGetListDto printCustomerDet;
    private BigDecimal chequeCompanyPoid;
    private LovGetListDto chequeCompanyDet;
    private String remarks;
}
