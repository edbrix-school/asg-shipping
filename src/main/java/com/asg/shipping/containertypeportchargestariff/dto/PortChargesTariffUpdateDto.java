package com.asg.shipping.containertypeportchargestariff.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortChargesTariffUpdateDto {

    @NotNull(message = "Port POID is required")
    private Long portPoid;

    @NotBlank(message = "Description is required")
    @Size(max = 100, message = "Description must not exceed 100 characters")
    private String description;

    @NotNull(message = "Period from date is required")
    private LocalDate periodFrom;

    @NotNull(message = "Period to date is required")
    private LocalDate periodTo;

    private Integer seqNo;
    @NotNull(message = "Charge line POID is required")
    private Long chargeLinePoid;

    @NotBlank(message = "Charge division is required")
    @Size(max = 10, message = "Charge division must not exceed 10 characters")
    private String chargeDivision;

    @Valid
    private List<PortChargesDetailUpdateDto> details;
}