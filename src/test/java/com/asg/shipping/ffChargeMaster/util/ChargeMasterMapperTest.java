package com.asg.shipping.ffChargeMaster.util;

import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeCreateDTO;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeDto;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeUpdateDTO;
import com.asg.shipping.shippingFFChargeMaster.entity.ShipChargeMaster;
import com.asg.shipping.shippingFFChargeMaster.util.ChargeMasterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
public class ChargeMasterMapperTest {

    @InjectMocks
    private ChargeMasterMapper mapper;

    private ShipChargeMaster entity;
    private ChargeCreateDTO createDto;
    private ChargeUpdateDTO updateDto;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        
        entity = ShipChargeMaster.builder()
                .chargePoid(1L)
                .groupPoid(100L)
                .chargeCode("TEST001")
                .chargeName("Test Charge")
                .chargeName2("Test Charge 2")
                .chargeRevenueType("REVENUE")
                .chargeType("FIXED")
                .chargeApplicableType("APPLICABLE")
                .divisionCode("DIV001")
                .chargeGlRevenue(1001L)
                .chargeGlCost(1002L)
                .chargeGlWip(1003L)
                .chargePayableGl(1004L)
                .fdaGlRevenue(2001L)
                .fdaGlCost(2002L)
                .directRevenueGl(3001L)
                .directCostOfSaleGl(3002L)
                .directPayableGl(3003L)
                .taxPoid(10L)
                .inputTaxPoid(11L)
                .chargeGroupPoid(20L)
                .shFfChargeMap(30L)
                .shFfChargeGlPoid(40L)
                .shFfChargeGlPoidRev(41L)
                .visibleInFf("Y")
                .oldChargeGlRevenue(4001L)
                .oldChargeGlCost(4002L)
                .active("Y")
                .seqno(1)
                .deleted("N")
                .build();

        createDto = ChargeCreateDTO.builder()
                .chargeCode("CREATE001")
                .chargeName("Create Charge")
                .chargeRevenueType("REVENUE")
                .chargeType("VARIABLE")
                .chargeApplicableType("APPLICABLE")
                .divisionCode("DIV002")
                .chargeGlRevenue(5001L)
                .chargeGlCost(5002L)
                .chargeGlWip(5003L)
                .chargePayableGl(5004L)
                .fdaGlRevenue(6001L)
                .fdaGlCost(6002L)
                .directRevenueGl(7001L)
                .directCostOfSaleGl(7002L)
                .directPayableGl(7003L)
                .taxPoid(15L)
                .inputTaxPoid(16L)
                .chargeGroupPoid(25L)
                .shFfChargeMap(35L)
                .shFfChargeGlPoid(45L)
                .active("Y")
                .seqno(2)
                .build();

        updateDto = ChargeUpdateDTO.builder()
                .chargeName("Update Charge")
                .chargeRevenueType("COST")
                .chargeType("FIXED")
                .chargeApplicableType("NOT_APPLICABLE")
                .divisionCode("DIV003")
                .chargeGlRevenue(8001L)
                .chargeGlCost(8002L)
                .chargeGlWip(8003L)
                .chargePayableGl(8004L)
                .fdaGlRevenue(9001L)
                .fdaGlCost(9002L)
                .directRevenueGl(10001L)
                .directCostOfSaleGl(10002L)
                .directPayableGl(10003L)
                .taxPoid(18L)
                .inputTaxPoid(19L)
                .chargeGroupPoid(28L)
                .shFfChargeMap(38L)
                .shFfChargeGlPoid(48L)
                .active("N")
                .seqno(3)
                .build();
    }

    @Test
    void mapToDto_Success() {
        ChargeDto result = mapper.mapToDto(entity);

        assertNotNull(result);
        assertEquals(entity.getChargePoid(), result.getChargePoid());
        assertEquals(entity.getGroupPoid(), result.getGroupPoid());
        assertEquals(entity.getChargeCode(), result.getChargeCode());
        assertEquals(entity.getChargeName(), result.getChargeName());
        assertEquals(entity.getChargeName2(), result.getChargeName2());
        assertEquals(entity.getChargeRevenueType(), result.getChargeRevenueType());
        assertEquals(entity.getChargeType(), result.getChargeType());
        assertEquals(entity.getChargeApplicableType(), result.getChargeApplicableType());
        assertEquals(entity.getDivisionCode(), result.getDivisionCode());
        assertEquals(entity.getChargeGlRevenue(), result.getChargeGlRevenue());
        assertEquals(entity.getChargeGlCost(), result.getChargeGlCost());
        assertEquals(entity.getChargeGlWip(), result.getChargeGlWip());
        assertEquals(entity.getChargePayableGl(), result.getChargePayableGl());
        assertEquals(entity.getFdaGlRevenue(), result.getFdaGlRevenue());
        assertEquals(entity.getFdaGlCost(), result.getFdaGlCost());
        assertEquals(entity.getDirectRevenueGl(), result.getDirectRevenueGl());
        assertEquals(entity.getDirectCostOfSaleGl(), result.getDirectCostOfSaleGl());
        assertEquals(entity.getDirectPayableGl(), result.getDirectPayableGl());
        assertEquals(entity.getTaxPoid(), result.getTaxPoid());
        assertEquals(entity.getInputTaxPoid(), result.getInputTaxPoid());
        assertEquals(entity.getChargeGroupPoid(), result.getChargeGroupPoid());
        assertEquals(entity.getShFfChargeMap(), result.getShFfChargeMap());
        assertEquals(entity.getShFfChargeGlPoid(), result.getShFfChargeGlPoid());
        assertEquals(entity.getShFfChargeGlPoidRev(), result.getShFfChargeGlPoidRev());
        assertEquals(entity.getVisibleInFf(), result.getVisibleInFf());
        assertEquals(entity.getOldChargeGlRevenue(), result.getOldChargeGlRevenue());
        assertEquals(entity.getOldChargeGlCost(), result.getOldChargeGlCost());
        assertEquals(entity.getActive(), result.getActive());
        assertEquals(entity.getSeqno(), result.getSeqno());
        assertEquals(entity.getCreatedBy(), result.getCreatedBy());
        assertEquals(entity.getCreatedDate(), result.getCreatedDate());
        assertEquals(entity.getLastModifiedBy(), result.getLastModifiedBy());
        assertEquals(entity.getLastModifiedDate(), result.getLastModifiedDate());
        assertEquals(entity.getDeleted(), result.getDeleted());
    }

    @Test
    void mapToDto_NullEntity() {
        ChargeDto result = mapper.mapToDto(null);
        assertNull(result);
    }

    @Test
    void mapCreateDTOToEntity_Success() {
        ShipChargeMaster targetEntity = new ShipChargeMaster();
        Long groupPoid = 200L;
        Long userPoid = 300L;

        mapper.mapCreateDTOToEntity(createDto, targetEntity, groupPoid, userPoid);

        assertEquals(groupPoid, targetEntity.getGroupPoid());
        assertEquals(createDto.getChargeCode(), targetEntity.getChargeCode());
        assertEquals(createDto.getChargeName(), targetEntity.getChargeName());
        assertEquals(createDto.getChargeRevenueType(), targetEntity.getChargeRevenueType());
        assertEquals(createDto.getChargeType(), targetEntity.getChargeType());
        assertEquals(createDto.getChargeApplicableType(), targetEntity.getChargeApplicableType());
        assertEquals(createDto.getDivisionCode(), targetEntity.getDivisionCode());
        assertEquals(createDto.getChargeGlRevenue(), targetEntity.getChargeGlRevenue());
        assertEquals(createDto.getChargeGlCost(), targetEntity.getChargeGlCost());
        assertEquals(createDto.getChargeGlWip(), targetEntity.getChargeGlWip());
        assertEquals(createDto.getChargePayableGl(), targetEntity.getChargePayableGl());
        assertEquals(createDto.getFdaGlRevenue(), targetEntity.getFdaGlRevenue());
        assertEquals(createDto.getFdaGlCost(), targetEntity.getFdaGlCost());
        assertEquals(createDto.getDirectRevenueGl(), targetEntity.getDirectRevenueGl());
        assertEquals(createDto.getDirectCostOfSaleGl(), targetEntity.getDirectCostOfSaleGl());
        assertEquals(createDto.getDirectPayableGl(), targetEntity.getDirectPayableGl());
        assertEquals(createDto.getTaxPoid(), targetEntity.getTaxPoid());
        assertEquals(createDto.getInputTaxPoid(), targetEntity.getInputTaxPoid());
        assertEquals(createDto.getChargeGroupPoid(), targetEntity.getChargeGroupPoid());
        assertEquals(createDto.getShFfChargeMap(), targetEntity.getShFfChargeMap());
        assertEquals(createDto.getShFfChargeGlPoid(), targetEntity.getShFfChargeGlPoid());
        assertEquals(createDto.getSeqno(), targetEntity.getSeqno());
        assertEquals(createDto.getActive(), targetEntity.getActive());
        assertEquals("N", targetEntity.getDeleted());
    }

    @Test
    void mapCreateDTOToEntity_DefaultActiveStatus() {
        ShipChargeMaster targetEntity = new ShipChargeMaster();
        ChargeCreateDTO dtoWithoutActive = ChargeCreateDTO.builder()
                .chargeCode("TEST")
                .chargeName("Test")
                .divisionCode("DIV")
                .active(null)
                .build();

        mapper.mapCreateDTOToEntity(dtoWithoutActive, targetEntity, 100L, 200L);

        assertEquals("Y", targetEntity.getActive());
        assertEquals("N", targetEntity.getDeleted());
    }

    @Test
    void mapCreateDTOToEntity_EmptyActiveStatus() {
        ShipChargeMaster targetEntity = new ShipChargeMaster();
        ChargeCreateDTO dtoWithEmptyActive = ChargeCreateDTO.builder()
                .chargeCode("TEST")
                .chargeName("Test")
                .divisionCode("DIV")
                .active("")
                .build();

        mapper.mapCreateDTOToEntity(dtoWithEmptyActive, targetEntity, 100L, 200L);

        assertEquals("Y", targetEntity.getActive());
        assertEquals("N", targetEntity.getDeleted());
    }

    @Test
    void mapUpdateDTOToEntity_Success() {
        ShipChargeMaster targetEntity = new ShipChargeMaster();
        Long groupPoid = 400L;
        Long userPoid = 500L;

        mapper.mapUpdateDTOToEntity(updateDto, targetEntity, groupPoid, userPoid);

        assertEquals(groupPoid, targetEntity.getGroupPoid());
        assertEquals(updateDto.getChargeName(), targetEntity.getChargeName());
        assertEquals(updateDto.getChargeRevenueType(), targetEntity.getChargeRevenueType());
        assertEquals(updateDto.getChargeType(), targetEntity.getChargeType());
        assertEquals(updateDto.getChargeApplicableType(), targetEntity.getChargeApplicableType());
        assertEquals(updateDto.getDivisionCode(), targetEntity.getDivisionCode());
        assertEquals(updateDto.getChargeGlRevenue(), targetEntity.getChargeGlRevenue());
        assertEquals(updateDto.getChargeGlCost(), targetEntity.getChargeGlCost());
        assertEquals(updateDto.getChargeGlWip(), targetEntity.getChargeGlWip());
        assertEquals(updateDto.getChargePayableGl(), targetEntity.getChargePayableGl());
        assertEquals(updateDto.getFdaGlRevenue(), targetEntity.getFdaGlRevenue());
        assertEquals(updateDto.getFdaGlCost(), targetEntity.getFdaGlCost());
        assertEquals(updateDto.getDirectRevenueGl(), targetEntity.getDirectRevenueGl());
        assertEquals(updateDto.getDirectCostOfSaleGl(), targetEntity.getDirectCostOfSaleGl());
        assertEquals(updateDto.getDirectPayableGl(), targetEntity.getDirectPayableGl());
        assertEquals(updateDto.getTaxPoid(), targetEntity.getTaxPoid());
        assertEquals(updateDto.getInputTaxPoid(), targetEntity.getInputTaxPoid());
        assertEquals(updateDto.getChargeGroupPoid(), targetEntity.getChargeGroupPoid());
        assertEquals(updateDto.getShFfChargeMap(), targetEntity.getShFfChargeMap());
        assertEquals(updateDto.getShFfChargeGlPoid(), targetEntity.getShFfChargeGlPoid());
        assertEquals(updateDto.getSeqno(), targetEntity.getSeqno());
        assertEquals(updateDto.getActive(), targetEntity.getActive());
    }

    @Test
    void mapUpdateDTOToEntity_NullActiveStatus() {
        ShipChargeMaster targetEntity = new ShipChargeMaster();
        targetEntity.setActive("Y"); // Set initial value
        
        ChargeUpdateDTO dtoWithNullActive = ChargeUpdateDTO.builder()
                .chargeName("Test")
                .divisionCode("DIV")
                .active(null)
                .build();

        mapper.mapUpdateDTOToEntity(dtoWithNullActive, targetEntity, 100L, 200L);

        assertEquals("Y", targetEntity.getActive()); // Should remain unchanged
    }

    @Test
    void mapUpdateDTOToEntity_EmptyActiveStatus() {
        ShipChargeMaster targetEntity = new ShipChargeMaster();
        targetEntity.setActive("Y"); // Set initial value
        
        ChargeUpdateDTO dtoWithEmptyActive = ChargeUpdateDTO.builder()
                .chargeName("Test")
                .divisionCode("DIV")
                .active("")
                .build();

        mapper.mapUpdateDTOToEntity(dtoWithEmptyActive, targetEntity, 100L, 200L);

        assertEquals("Y", targetEntity.getActive()); // Should remain unchanged
    }
}