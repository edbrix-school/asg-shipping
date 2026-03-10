package com.asg.shipping.linetariffs.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Line Tariff
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineTariffDto {

    private Long transactionPoid;
    private LocalDate transactionDate;
    private Long groupPoid;
    private Long linePoid;
    private LovGetListDto lineDet; // LOV data for line
    private String description;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private String dmgFromSameday;
    private String dmgFromNextday;
    private String dmgSkipHolidays;
    private String dmgSkipWeekends;
    private String dmgBaseslabAfterFree;
    private String dtnFromSameday;
    private String dtnFromNextday;
    private String dtnSkipHolidays;
    private String dtnSkipWeekends;
    private String dtnBaseslabAfterFree;
    private String payableCurrency;
    private LovGetListDto payableCurrencyDet; // LOV data for currency
    private String receivableCurrency;
    private LovGetListDto receivableCurrencyDet; // LOV data for currency
    private String docRef;
    private Long companyPoid;
    private Integer seqno;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String deleted;

    // Detail records
    private List<TariffDetailDto> importDemurrageCollectable;
    private List<TariffDetailDto> importDemurragePayable;
    private List<TariffDetailDto> exportDetentionCollectable;
    private List<TariffDetailDto> exportDetentionPayable;
}

