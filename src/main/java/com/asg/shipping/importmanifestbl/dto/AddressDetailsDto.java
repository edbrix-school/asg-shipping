package com.asg.shipping.importmanifestbl.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressDetailsDto {

    private Long detRowId;
    private Long preferredCommunicationPoid;
    private String email1;
    private String email2;

}
