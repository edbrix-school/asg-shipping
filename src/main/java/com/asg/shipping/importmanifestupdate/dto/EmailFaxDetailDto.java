package com.asg.shipping.importmanifestupdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailFaxDetailDto {
    private Long addressPoid;
    private String email1;
    private String email2;
    private String fax;
    private String addressType;
}
