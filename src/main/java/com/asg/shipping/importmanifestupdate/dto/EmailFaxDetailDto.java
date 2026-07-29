package com.asg.shipping.importmanifestupdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailFaxDetailDto {
    private String actionType;
    private BigDecimal addressPoid;
    private String addressType;
    private String email1;
    private String email2;
    private String sendYesNo;
    private String sendEmailFax;
}
