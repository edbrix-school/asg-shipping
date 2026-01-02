package com.asg.shipping.demurragedetentionpayabletransfer.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for SHIP_DEM_DTN_TRANSFER_BILL_DTL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemurrageDetentionTransferBillDetailDto {

    private Long detRowId;
    private Long glPoid;
    private LovGetListDto glPoidDet; // LOV data
    private Long glCompanyPoid;
    private String billRefType;
    private String billRefno;
    private LocalDate billDueDate;
    private String description;
    private BigDecimal drAmt;
    private BigDecimal crAmt;
    private String containerNo;
    private BigDecimal billwiseBalance;
    private String checkall;
}
