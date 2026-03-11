package com.asg.shipping.contractsandagreements.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContractsAgreementPicDtlDto {

    private Long detRowId;

    @NotNull(message = "Department  is required")
    private Long departmentPoid;
    @NotNull(message = "Handled By is required")
    private Long handledUserPoid;
    @NotNull(message = "Period From is required")
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private String remarks;
    private String actionType;

}
