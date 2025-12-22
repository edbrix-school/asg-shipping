package com.asg.shipping.commoditymaster.mapper;

import com.asg.shipping.commoditymaster.dto.request.CommodityMasterRequest;
import com.asg.shipping.commoditymaster.dto.response.CommodityMasterResponse;
import com.asg.shipping.commoditymaster.entity.CommodityMaster;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

@Component
public class CommodityMapper {

    public CommodityMasterResponse mapToDto(CommodityMaster commodity) {
        if (commodity == null) {
            return null;
        }
        
        return CommodityMasterResponse.builder()
                .commodityPoid(commodity.getCommodityPoid())
                .groupPoid(commodity.getGroupPoid())
                .commodityCode(commodity.getCommodityCode())
                .commodityName(commodity.getCommodityName())
                .commodityName2(commodity.getCommodityName2())
                .active(commodity.getActive())
                .seqno(commodity.getSeqno())
                .createdBy(commodity.getCreatedBy())
                .createdDate(commodity.getCreatedDate())
                .lastmodifiedBy(commodity.getLastmodifiedBy())
                .lastmodifiedDate(commodity.getLastmodifiedDate())
                .deleted(commodity.getDeleted())
                .build();
    }

    public void mapCreateDTOToEntity(CommodityMasterRequest dto, CommodityMaster commodity, Long groupPoid, String userPoid) {
        // commodityPoid will be set by database trigger
        commodity.setCommodityName(dto.getCommodityName());
        commodity.setCommodityName2(dto.getCommodityName2());
        commodity.setActive(dto.getActive() != null ? dto.getActive() : "Y");
        commodity.setSeqno(dto.getSeqno());
        commodity.setGroupPoid(groupPoid);
        commodity.setCreatedBy(userPoid);
        commodity.setCreatedDate(Timestamp.from(Instant.now()));
        commodity.setLastmodifiedBy(userPoid);
        commodity.setLastmodifiedDate(Timestamp.from(Instant.now()));
        commodity.setDeleted("N");
        // commodityCode will be set by database trigger
    }

    public void mapUpdateDTOToEntity(CommodityMasterRequest dto, CommodityMaster commodity, String userPoid) {
        commodity.setCommodityName(dto.getCommodityName());
        commodity.setCommodityName2(dto.getCommodityName2());
        commodity.setActive(dto.getActive());
        commodity.setSeqno(dto.getSeqno());
        commodity.setLastmodifiedBy(userPoid);
        commodity.setLastmodifiedDate(Timestamp.from(Instant.now()));
    }
}