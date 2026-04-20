package com.asg.shipping.shippingofoqv2.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OFOQApiDataHdrDto {
    
    private Long transactionPoid;
    private String docRef;
    private LocalDateTime transactionDate;
    private String voyageNo;
    private Long vesselPoid;
    private LocalDateTime arrivalDate;
    private Long rotationNumber;
    private String apiProvisionalMfNo;
    private String apiProvisionalStatus;
    private String manifestNo;
    private String manifestStatus;
    private String remarks;
    private String functionalReference;
    private String deleted;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}