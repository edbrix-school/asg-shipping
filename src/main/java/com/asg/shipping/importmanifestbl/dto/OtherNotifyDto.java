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
public class OtherNotifyDto {

    private String notify2EdiName;
    private String notify2EdiAddress;
    private Long notify2Poid;
    private String notify3EdiName;
    private String notify3EdiAddress;
    private Long notify3Poid;
    private String actionType;
}