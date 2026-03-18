package com.asg.shipping.importmanifestbl.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImportManifestBlResponseDto {

    private String status;
    private Long transactionPoid;


}
