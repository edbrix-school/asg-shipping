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
public class DescriptionAndMarksDto {

    private Long detRowId;
    private String cargoDescription;
    private String marksDescription;
    private String descriptionType;
    private String actionType;

}
