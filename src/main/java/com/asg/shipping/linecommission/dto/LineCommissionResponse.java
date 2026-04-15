package com.asg.shipping.linecommission.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineCommissionResponse {
    private Long transactionPoid;
    private String docRef;
    private Long linePoid;
    private LovGetListDto lineDet;
    private String description;
    private String remarks;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private LocalDate renewalDate;
    private LocalDate transactionDate;
    private Long currencyPoid;
    private LovGetListDto currencyDet;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
    private String deleted;

    private List<ContainerRateDto> containerRates = new ArrayList<>();
    private List<OtherRemunerationDto> otherRemunerations = new ArrayList<>();
    private List<LocalShareDto> localShares = new ArrayList<>();
}


