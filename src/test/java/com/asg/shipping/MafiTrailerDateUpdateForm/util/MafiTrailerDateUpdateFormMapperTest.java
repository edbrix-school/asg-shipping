package com.asg.shipping.mafitrailerdateupdateform.util;

import com.asg.shipping.mafitrailerdateupdateform.dto.MafiDetailDto;
import com.asg.shipping.mafitrailerdateupdateform.dto.MafiTrailerDateUpdateFormRequest;
import com.asg.shipping.mafitrailerdateupdateform.dto.MafiTrailerDateUpdateFormResponse;
import com.asg.shipping.mafitrailerdateupdateform.dto.MafitrailerHeaderDTO;
import com.asg.shipping.mafitrailerdateupdateform.dto.VoyageProjection;
import com.asg.shipping.mafitrailerdateupdateform.entity.ShipBlMafiDtl;
import com.asg.shipping.mafitrailerdateupdateform.entity.ShipBlMafiDtlId;
import com.asg.shipping.mafitrailerdateupdateform.entity.ShipBlMafiHdr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MafiTrailerDateUpdateFormMapperTest {

    @InjectMocks
    private MafiTrailerDateUpdateFormMapper mapper;

    @Test
    void toMafiTrailerResponse_NullHeaderOrDetails() {
        assertNull(mapper.toMafiTrailerResponse(null, mock(VoyageProjection.class), Collections.emptyList()));
        assertNull(mapper.toMafiTrailerResponse(new ShipBlMafiHdr(), mock(VoyageProjection.class), null));
        assertNull(mapper.toMafiTrailerResponse(null, mock(VoyageProjection.class), null));
    }
 
    @Test
    void toMafiTrailerResponse_NullVoyage() {
        assertNull(mapper.toMafiTrailerResponse(new ShipBlMafiHdr(), null, Collections.emptyList()));
    }

    @Test
    void toMafiTrailerResponse_Success() {
        // Arrange
        ShipBlMafiHdr header = new ShipBlMafiHdr();
        header.setAgentReference("AgentRef123");
        header.setRemarks("HdrRemarks");

        VoyageProjection voyage = mock(VoyageProjection.class);
        when(voyage.getVoyageNo()).thenReturn("V123");
        when(voyage.getJobNo()).thenReturn("J123");
        when(voyage.getLinePoid()).thenReturn(1L);
        when(voyage.getLineCode()).thenReturn("LC");
        when(voyage.getLineName()).thenReturn("LineName");
        when(voyage.getVesselPoid()).thenReturn(2L);
        when(voyage.getVesselCode()).thenReturn("VC");
        when(voyage.getVesselName()).thenReturn("VesselName");

        ShipBlMafiDtl detail = new ShipBlMafiDtl();
        ShipBlMafiDtlId id = new ShipBlMafiDtlId();
        id.setTransactionPoid(10L);
        id.setDetRowId(1L);
        detail.setId(id);
        detail.setBlPoid(20L);
        detail.setMafiRef("MafiRef");
        detail.setMafiSize(java.math.BigDecimal.valueOf(40));
        detail.setMafiFreeDays(java.math.BigDecimal.valueOf(5));
        detail.setRemarks("DtlRemarks");
        java.time.LocalDate date1 = java.time.LocalDate.now();
        java.time.LocalDate date2 = java.time.LocalDate.now();
        detail.setMafiEmptyDate(date1);
        detail.setBackLoadDate(date2);

        List<ShipBlMafiDtl> details = List.of(detail);

        // Act
        MafiTrailerDateUpdateFormResponse response = mapper.toMafiTrailerResponse(header, voyage, details);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getMafiHeader());
        assertEquals("V123", response.getMafiHeader().getVoyageNo());
        assertEquals("J123", response.getMafiHeader().getJobNo());
        assertEquals(1L, response.getMafiHeader().getLinePoid());
        assertEquals("AgentRef123", response.getMafiHeader().getAgentReference());
        assertEquals("HdrRemarks", response.getMafiHeader().getRemarks());
        assertEquals(2L, response.getMafiHeader().getVesselPoid());
        
        // Assert Maps
        assertEquals(1L, response.getMafiHeader().getLineDetail().get("poid"));
        assertEquals("LC", response.getMafiHeader().getLineDetail().get("code"));
        assertEquals("LineName", response.getMafiHeader().getLineDetail().get("description"));
        
        assertEquals(2L, response.getMafiHeader().getVesselDetail().get("poid"));
        assertEquals("VC", response.getMafiHeader().getVesselDetail().get("code"));
        assertEquals("VesselName", response.getMafiHeader().getVesselDetail().get("description"));

        assertNotNull(response.getMafiDetails());
        assertEquals(1, response.getMafiDetails().size());
        MafiDetailDto pDto = response.getMafiDetails().get(0);
        assertEquals(10L, pDto.getTransactionPoid());
        assertEquals(1L, (Long) pDto.getDetRowId());
        assertEquals(20L, pDto.getBlPoid());
        assertEquals("MafiRef", pDto.getMafiRef());
        assertEquals(java.math.BigDecimal.valueOf(40), pDto.getMafiSize());
        assertEquals(java.math.BigDecimal.valueOf(5), pDto.getMafiFreeDays());
        assertEquals("DtlRemarks", pDto.getRemarks());
        assertEquals(date1, pDto.getMafiEmptyDate());
        assertEquals(date2, pDto.getBackLoadDate());
    }

    @Test
    void updateShipBlMafiHdr_NullValues() {
        ShipBlMafiHdr entity = new ShipBlMafiHdr();
        entity.setAgentReference("OldRef");

        // When request is null, entity should not be changed
        mapper.updateShipBlMafiHdr(entity, null, "User1");
        assertEquals("OldRef", entity.getAgentReference());

        // When entity is null, no exception should be thrown
        assertDoesNotThrow(() -> mapper.updateShipBlMafiHdr(null, new MafiTrailerDateUpdateFormRequest(), "User1"));

        // When MafiHeader is null
        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(null);
        mapper.updateShipBlMafiHdr(entity, request, "User1");
        assertEquals("OldRef", entity.getAgentReference());
    }

    @Test
    void updateShipBlMafiHdr_Success() {
        // Arrange
        ShipBlMafiHdr entity = new ShipBlMafiHdr();
        entity.setAgentReference("OldRef");
        entity.setRemarks("OldRemarks");

        MafitrailerHeaderDTO headerDTO = new MafitrailerHeaderDTO();
        headerDTO.setAgentReference("NewRef");
        headerDTO.setRemarks("NewRemarks");

        MafiTrailerDateUpdateFormRequest request = new MafiTrailerDateUpdateFormRequest();
        request.setMafiHeader(headerDTO);

        // Act
        mapper.updateShipBlMafiHdr(entity, request, "User1");

        // Assert
        assertEquals("NewRef", entity.getAgentReference());
        assertEquals("NewRemarks", entity.getRemarks());
    }
}
