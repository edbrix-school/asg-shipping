package com.asg.shipping.portstoragetariffsmaster.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Port Storage Tariff (header with nested details)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortStorageTariffDto {

    private Long transactionPoid;
    private Long groupPoid;
    private Long portPoid;
    private LovGetListDto portDet; // LOV data for port
    private String description;
    private String tariffType;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private LocalDate transactionDate;
    private String docRef;
    private Long companyPoid;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String deleted;
    private List<TariffDetailDto> tariffDetails; // Nested detail records
}
