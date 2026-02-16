package com.asg.shipping.importmanifestbl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DescriptionAndMarksDto {

    private String cargoDescription;
    private String marksDescription;

}
