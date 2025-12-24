package com.asg.shipping.linetariffs.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for tariff detail records (used for all four detail tables)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TariffDetailDto {

    private Long detRowId;
    private Long containerTypePoid;
    private LovGetListDto containerTypeDet;// LOV data for container type
    private Integer freeDays;
    private Integer slab1Tilldays;
    private BigDecimal slab1Rate;
    private Integer slab2Tilldays;
    private BigDecimal slab2Rate;
    private Integer slab3Tilldays;
    private BigDecimal slab3Rate;
    private Integer slab4Tilldays;
    private BigDecimal slab4Rate;
    private Integer slab5Tilldays;
    private BigDecimal slab5Rate;
    private Integer slab6Tilldays;
    private BigDecimal slab6Rate;
    private Integer slab7Tilldays;
    private BigDecimal slab7Rate;
}

