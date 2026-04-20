package com.asg.shipping.importmanifestupdate.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SaveEmailsRequestDto {

    private String emailsText;           // Text from "Emails from Manifest" textarea
    private Boolean updateConsignee;     // Consignee checkbox
    private Boolean updateNotify;        // Notify checkbox
    private String scope;
}
