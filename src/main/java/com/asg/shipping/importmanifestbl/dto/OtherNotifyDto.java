package com.asg.shipping.importmanifestbl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OtherNotifyDto {

    private String notify2EdiName;
    private String notify2EdiAddress;
    private Long notify2Poid;

    private String notify3EdiName;
    private String notify3EdiAddress;
    private Long notify3Poid;


    private String actionType;
}