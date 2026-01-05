package com.asg.shipping.importManifestUpdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MafiRequestDto {
    private Long detRowId;
    private String mafiRef;
    private String remarks;
    private Long mafiFreeDays;
    private Long mafiSize;
    private LocalDate mafiEmptyDate;
    private LocalDate backLoadDate;
}
