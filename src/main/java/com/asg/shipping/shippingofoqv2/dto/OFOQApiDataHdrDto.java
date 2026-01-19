package com.asg.shipping.shippingofoqv2.dto;

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
public class OFOQApiDataHdrDto {
    
    private Long transactionPoid;
    private String docRef;
    private LocalDate transactionDate;
    private String voyageNo;
    private Long vesselPoid;
    private LocalDate arrivalDate;
    private Long rotationNumber;
    private String apiProvisionalMfNo;
    private String apiProvisionalStatus;
    private String manifestNo;
    private String manifestStatus;
    private String remarks;
    private String deleted;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}