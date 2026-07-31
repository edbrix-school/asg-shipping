package com.asg.shipping.importmanifestbl.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressDetailsDto {

    private Long detRowId;
    private BigDecimal addressPoid;
    private String email1;
    private String email2;
    private String addressType;
    private String sendYesNo;
    private String sendEmailFax;
    private String actionType;
    private Boolean fromMaster;

}
