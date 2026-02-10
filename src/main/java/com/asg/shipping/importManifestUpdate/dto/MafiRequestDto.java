package com.asg.shipping.importManifestUpdate.dto;

import lombok.*;
import java.time.LocalDateTime;

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
    private LocalDateTime mafiEmptyDate;
    private LocalDateTime backLoadDate;
    private String actionType;
}
