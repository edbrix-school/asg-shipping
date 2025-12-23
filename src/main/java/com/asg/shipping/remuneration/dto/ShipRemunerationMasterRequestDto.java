package com.asg.shipping.remuneration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipRemunerationMasterRequestDto {
    private String remunCode;
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