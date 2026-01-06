package com.asg.shipping.containertypeportchargestariff.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortChargesTariffCreateDto {

    @NotNull(message = "Port POID is required")
    private Long portPoid;

    @NotBlank(message = "Description is required")
    @Size(min = 5, max = 100, message = "Description must be between 5 and 100 characters")
    private String description;

    @NotNull(message = "Period from date is required")
    private LocalDate periodFrom;

    @NotNull(message = "Period to date is required")
    private LocalDate periodTo;

    private Integer seqNo;
    
    @NotNull(message = "Charge line POID is required")
    private Long chargeLinePoid;

    @NotBlank(message = "Charge division is required")
    @Size(max = 10, message = "Charge division cannot exceed 10 characters")
    private String chargeDivision;

    @Valid
    private List<PortChargesDetailCreateDto> details;
}