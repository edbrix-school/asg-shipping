package com.asg.shipping.importmanifestbl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MafiDetailsDto {
    private Long detRowId;
    private String mafiReferenceNumber;
    private Long mafiSize;
    private Long mafiFreeDays;
    private String remarks;
    private String actionType;
}
