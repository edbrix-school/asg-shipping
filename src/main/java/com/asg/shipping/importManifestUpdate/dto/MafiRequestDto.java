package com.asg.shipping.importManifestUpdate.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
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
    private String actionType;
}
