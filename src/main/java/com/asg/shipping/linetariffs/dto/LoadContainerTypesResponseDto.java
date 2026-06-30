package com.asg.shipping.linetariffs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadContainerTypesResponseDto {

    private Long transactionPoid;
    /** IMP = Import Demurrage Collectable, EXP = Export Detention Collectable */
    private String type;
    private List<TariffDetailDto> containerTypes;
}
