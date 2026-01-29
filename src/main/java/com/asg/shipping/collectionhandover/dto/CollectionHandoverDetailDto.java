package com.asg.shipping.collectionhandover.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for collection handover detail records
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionHandoverDetailDto {

    private Long detRowId;
    private BigDecimal currencyAmount;
    private String currencyType;
    private LovGetListDto currencyTypeDet; // LOV data for currency
    private Integer noOfTran;
    private BigDecimal cashAmount;
}

