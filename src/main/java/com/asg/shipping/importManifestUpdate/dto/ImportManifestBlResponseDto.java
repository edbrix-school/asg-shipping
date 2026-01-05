package com.asg.shipping.importManifestUpdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportManifestBlResponseDto {

    private Long transactionPoid;
    private String docRef;
    private String blNumber;
    private String blStatus;
    private LocalDate transactionDate;

    private String vesselName;
    private String voyageNo;

    private ImportManifestBlRequestDto data;

    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime lastModifiedDate;
    private String lastModifiedBy;

}
