package com.asg.shipping.containerinventorymovementupdate.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCimuRequest {
    @NotNull(message = "transactionPoid is required")
    private Long transactionPoid;

    @Size(max = 50, message = "containerNo must be <= 50 chars")
    private String containerNo;

    private Boolean applyToAllContainers;

    private Long extraFreeDays;
    private Long extraFreeDaysPrnpls;

    @Size(max = 25, message = "blIssueType must be <= 25 chars")
    private String blIssueType;

    private Boolean holdReturnForm;

    private String withConsigneeFull;
    private String emptyIn;

    // Optional: datetime; legacy had rights-based control
    private String actualDischargeDate;

    // Optional safety: if frontend passes BL number, backend validates it matches transactionPoid
    private String blNumber;
}


