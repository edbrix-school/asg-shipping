package com.asg.shipping.importmanifestbl.util;

import com.asg.shipping.importmanifestbl.dto.CommodityDTO;
import com.asg.shipping.importmanifestbl.dto.ContainerTypeDTO;

import java.math.BigDecimal;
public class ImportManifestDropdownMapper {

    public static ContainerTypeDTO mapContainer(Object[] row) {
        return ContainerTypeDTO.builder()
                .groupPoid((BigDecimal) row[0])
                .containerTypePoid((BigDecimal) row[1])
                .containerTypeCode((String) row[2])
                .containerTypeName((String) row[3])
                .containerGrpPoid((BigDecimal) row[4])
                .containerTypeSize((String) row[5])
                .containerTypeIsoName((String) row[6])
                .containerCargoWeight((BigDecimal) row[7])
                .containerTareWeight((BigDecimal) row[8])
                .containerTeuFactor((BigDecimal) row[9])
                .containerTypeCategory((String) row[10])
                .active(row[10] != null ? row[10].toString() : null)
                .seqno(row[12] != null ? ((BigDecimal) row[12]).longValue() : null)
                .containerApmtTypeCode((String) row[13])
                .build();
    }




    public static CommodityDTO mapCommodity(Object[] row) {
        return CommodityDTO.builder()
                .groupPoid((BigDecimal) row[0])
                .commodityPoid((BigDecimal) row[1])
                .commodityCode((String) row[2])
                .commodityName((String) row[3])
                .commodityName2((String) row[4])
                .active(row[5] != null ? row[5].toString() : null)
                .seqNo((BigDecimal) row[6])
                .createdBy((String) row[7])
                .createdDate(row[8] != null ? (java.time.LocalDateTime) row[8] : null)
                .lastModifiedBy((String) row[9])
                .lastModifiedDate(row[10] != null ? (java.time.LocalDateTime) row[10] : null)
                .deleted(row[11] != null ? row[11].toString() : null)
                .build();
    }

}
