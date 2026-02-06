package com.asg.shipping.importmanifestbl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerTypeDTO {
    private BigDecimal groupPoid;
    private BigDecimal containerTypePoid;
    private String containerTypeCode;
    private String containerTypeName;
    private BigDecimal containerGrpPoid;
    private String containerTypeSize;
    private String containerTypeIsoName;
    private BigDecimal containerCargoWeight;
    private BigDecimal containerTareWeight;
    private BigDecimal containerTeuFactor;
    private String containerTypeCategory;
    private String active;
    private Long seqno;
    private String containerApmtTypeCode;
}
