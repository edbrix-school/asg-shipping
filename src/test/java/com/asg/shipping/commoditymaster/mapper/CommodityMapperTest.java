package com.asg.shipping.commoditymaster.mapper;

import com.asg.shipping.commoditymaster.dto.request.CommodityMasterRequest;
import com.asg.shipping.commoditymaster.dto.response.CommodityMasterResponse;
import com.asg.shipping.commoditymaster.entity.CommodityMaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CommodityMapperTest {

    private CommodityMapper commodityMapper;
    private CommodityMaster testEntity;
    private CommodityMasterRequest testRequest;

    @BeforeEach
    void setUp() {
        commodityMapper = new CommodityMapper();

        testEntity = new CommodityMaster();
        testEntity.setCommodityPoid(1L);
        testEntity.setGroupPoid(1L);
        testEntity.setCommodityCode("TEST001");
        testEntity.setCommodityName("Test Commodity");
        testEntity.setCommodityName2("Test Commodity 2");
        testEntity.setActive("Y");
        testEntity.setSeqno(1L);
        testEntity.setDeleted("N");

        testRequest = CommodityMasterRequest.builder()
                .commodityName("Test Commodity")
                .commodityName2("Test Commodity 2")
                .active("Y")
                .seqno(1L)
                .build();
    }

    @Test
    void mapToDto_Success() {
        CommodityMasterResponse result = commodityMapper.mapToDto(testEntity);

        assertNotNull(result);
        assertEquals(testEntity.getCommodityPoid(), result.getCommodityPoid());
        assertEquals(testEntity.getGroupPoid(), result.getGroupPoid());
        assertEquals(testEntity.getCommodityCode(), result.getCommodityCode());
        assertEquals(testEntity.getCommodityName(), result.getCommodityName());
        assertEquals(testEntity.getCommodityName2(), result.getCommodityName2());
        assertEquals(testEntity.getActive(), result.getActive());
        assertEquals(testEntity.getSeqno(), result.getSeqno());
        assertEquals(testEntity.getCreatedBy(), result.getCreatedBy());
        assertEquals(testEntity.getCreatedDate(), result.getCreatedDate());
        assertEquals(testEntity.getDeleted(), result.getDeleted());
    }

    @Test
    void mapToDto_NullInput() {
        CommodityMasterResponse result = commodityMapper.mapToDto(null);

        assertNull(result);
    }

    @Test
    void mapCreateDTOToEntity_Success() {
        CommodityMaster entity = new CommodityMaster();
        Long groupPoid = 1L;
        String userPoid = "testuser";

        commodityMapper.mapCreateDTOToEntity(testRequest, entity, groupPoid, userPoid);

        assertEquals(testRequest.getCommodityName(), entity.getCommodityName());
        assertEquals(testRequest.getCommodityName2(), entity.getCommodityName2());
        assertEquals(testRequest.getActive(), entity.getActive());
        assertEquals(testRequest.getSeqno(), entity.getSeqno());
        assertEquals(groupPoid, entity.getGroupPoid());
        assertEquals("N", entity.getDeleted());
    }

    @Test
    void mapCreateDTOToEntity_NullActive() {
        CommodityMaster entity = new CommodityMaster();
        CommodityMasterRequest requestWithNullActive = CommodityMasterRequest.builder()
                .commodityName("Test Commodity")
                .active(null)
                .build();

        commodityMapper.mapCreateDTOToEntity(requestWithNullActive, entity, 1L, "testuser");

        assertEquals("Y", entity.getActive()); // Should default to "Y"
    }

    @Test
    void mapUpdateDTOToEntity_Success() {
        CommodityMaster existingEntity = new CommodityMaster();
        existingEntity.setCommodityPoid(1L);
        existingEntity.setGroupPoid(1L);
        existingEntity.setCommodityCode("TEST001");
        existingEntity.setCreatedBy("originaluser");
        existingEntity.setCreatedDate(LocalDateTime.now());

        String userPoid = "updateuser";

        commodityMapper.mapUpdateDTOToEntity(testRequest, existingEntity, userPoid);

        assertEquals(testRequest.getCommodityName(), existingEntity.getCommodityName());
        assertEquals(testRequest.getCommodityName2(), existingEntity.getCommodityName2());
        assertEquals(testRequest.getActive(), existingEntity.getActive());
        assertEquals(testRequest.getSeqno(), existingEntity.getSeqno());

        // These should remain unchanged
        assertEquals(1L, existingEntity.getCommodityPoid());
        assertEquals(1L, existingEntity.getGroupPoid());
        assertEquals("TEST001", existingEntity.getCommodityCode());
        assertEquals("originaluser", existingEntity.getCreatedBy());
    }
}