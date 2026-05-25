package com.asg.shipping.exportManifestUpdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportManifestAddressDto {
    private String addressPoid;
    private Long addressMasterPoid;
    private String addressType;
    private String offTel1;
    private String contactPerson;
    private String mobile;
    private String fax;
    private String email;
    private String poBox;
}
