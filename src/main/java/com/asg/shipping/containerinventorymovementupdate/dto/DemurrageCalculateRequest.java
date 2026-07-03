package com.asg.shipping.containerinventorymovementupdate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemurrageCalculateRequest {
    @NotNull(message = "transactionPoid is required")
    private Long transactionPoid;

    @NotBlank(message = "containerNo is required")
    @Size(max = 50, message = "containerNo must be <= 50 chars")
    private String containerNo;

    @NotNull(message = "demDt is required")
    private LocalDate demDt;

    private String emptyIn;
}


