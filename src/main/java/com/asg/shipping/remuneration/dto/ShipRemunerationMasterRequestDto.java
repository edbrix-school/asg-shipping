package com.asg.shipping.remuneration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipRemunerationMasterRequestDto {
    @Size(max = 20, message = "Remuneration Code must not exceed 20 characters")
    @NotBlank(message = "Remuneration Code should not be empty")
    private String remunCode;

    @Size(max = 100, message = "Remuneration Description must not exceed 100 characters")
    @NotBlank(message = "Remuneration Description should not be empty")
    private String remunDescription;

    private String impExpType;

    private String active;

    private Integer seqNo;

    private String remunBasedOn;

    private Long remunChargeCodePoid;

    private Long glPoid;

    private String remunBookedByUsed;
}