package com.asg.shipping.containerinventorymovementupdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerHistoryRowDto {
    private String containerNo;
    private Long extraFreeDays;

    private String dischargeFull;
    private String withConsigneeFull;
    private String emptyIn;
    private String emptyOut;
    private String exportPortFull;
    private String loadFull;
    private String loadEmpty;

    private String selected; // "Y" or "N"
}


