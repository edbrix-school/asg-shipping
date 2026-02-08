package com.asg.shipping.importmanifestbl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContainersDropDownDto {
    private List<ContainerTypeDTO> containerTypes;
    private List<CommodityDTO> commodities;

}
