package com.asg.shipping.remuneration.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ShipRemunerationMasterResponseDto {
    private Long remunerationPoid;
    private String remunCode;
    private String remunDescription;
    private String impExpType;
    private String active;
    private Integer seqNo;
    private String remunBasedOn;
    private String remunChargeCodePoid;
    private Long glPoid;
    private String remunBookedByUsed;
    private String deleted;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}

