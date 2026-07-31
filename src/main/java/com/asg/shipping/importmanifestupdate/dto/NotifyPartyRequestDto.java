package com.asg.shipping.importmanifestupdate.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyPartyRequestDto {
    private Long detRowId;
    private BigDecimal addressPoid;
    private String fax;
    private String email1;
    private String email2;
    private String sendEmailFax;
    private String sendYesNo;
    private String addressType;
    private String faxLog;
    private String emailLog;
    private String actionType;
    private Boolean fromMaster;
}
