package com.asg.shipping.importmanifestbl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MafiDetailsDto {
    private Long detRowId;
    private String mafiReferenceNumber;
    private Long mafiSize;
    private Long mafiFreeDays;
    private String remarks;
    private String actionType;
}
