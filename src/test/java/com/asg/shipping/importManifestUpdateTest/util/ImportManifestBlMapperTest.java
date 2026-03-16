package com.asg.shipping.importManifestUpdateTest.util;

import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.importManifestUpdate.dto.*;
import com.asg.shipping.importManifestUpdate.entity.*;
import com.asg.shipping.importManifestUpdate.util.ImportManifestBlMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class ImportManifestBlMapperTest {

    @InjectMocks
    private ImportManifestBlMapper mapper;

    @Test
    void mapToDto_Success() {
        ShipBlManifestHdr entity = new ShipBlManifestHdr();
        entity.setTransactionPoid(1L);
        entity.setBlNumber("BL123");
        entity.setAgentReference("AG001");

        ImportManifestBlRequestDto dto = mapper.mapToDto(entity);

        assertNotNull(dto);
        assertEquals(1L, dto.getTransactionPoid());
        assertEquals("BL123", dto.getBlNumber());
    }

    @Test
    void mapToDto_Null() {
        assertNull(mapper.mapToDto(null));
    }

    @Test
    void mapToEntity_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(200L);

            ImportManifestBlCreateDto dto = ImportManifestBlCreateDto.builder()
                    .blNumber("BL123")
                    .agentReference("AG001")
                    .build();

            ShipBlManifestHdr entity = mapper.mapToEntity(dto);

            assertNotNull(entity);
            assertEquals("BL123", entity.getBlNumber());
            assertEquals("N", entity.getDeleted());
        }
    }

    @Test
    void mapUpdateDTOToEntity_Success() {
        ShipBlManifestHdr entity = new ShipBlManifestHdr();
        ImportManifestBlUpdateDTO dto = ImportManifestBlUpdateDTO.builder()
                .blNumber("BL456")
                .agentReference("AG002")
                .build();

        ShipBlManifestHdr result = mapper.mapUpdateDTOToEntity(dto, entity);

        assertNotNull(result);
        assertEquals("BL456", result.getBlNumber());
    }

    @Test
    void mapGeneralDtlToDto_Success() {
        ShipBlManifestGeneralDtl entity = new ShipBlManifestGeneralDtl();
        entity.setId(new ShipBlManifestDtlId(1L, 1L));
        entity.setCargoDescription("Test Cargo");

        GeneralCargoRequestDto dto = mapper.mapGeneralDtlToDto(entity);

        assertNotNull(dto);
        assertEquals(1L, dto.getDetRowId());
    }

    @Test
    void mapCargoDtlFromDto_Success() {
        CargoDescriptionRequestDto dto = CargoDescriptionRequestDto.builder()
                .detRowId(1L)
                .cargoDescription("Test")
                .descriptionType("TYPE1")
                .build();

        ShipBlManifestCargoDtl entity = mapper.mapCargoDtlFromDto(dto, 1L);

        assertNotNull(entity);
        assertEquals("Test", entity.getCargoDescription());
    }

    @Test
    void mapContainerDtlToDto_Success() {
        ShipBlManifestContainerDtl entity = new ShipBlManifestContainerDtl();
        entity.setId(new ShipBlManifestDtlId(1L, 1L));
        entity.setContainerNo("CONT123");

        ContainerRequestDto dto = mapper.mapContainerDtlToDto(entity);

        assertNotNull(dto);
        assertEquals("CONT123", dto.getContainerNo());
    }

    @Test
    void updateContainerFromDto_Success() {
        ShipBlManifestContainerDtl entity = new ShipBlManifestContainerDtl();
        ContainerRequestDto dto = ContainerRequestDto.builder()
                .containerNo("CONT456")
                .equipmentIsoType("ISO1")
                .build();

        mapper.updateContainerFromDto(dto, entity);

        assertEquals("CONT456", entity.getContainerNo());
    }

    @Test
    void mapChargesDtlListToDto_Success() {
        ShipBlManifestChargesDtl entity = new ShipBlManifestChargesDtl();
        entity.setId(new ShipBlManifestDtlId(1L, 1L));
        entity.setChargePoid(100L);

        List<ChargeRequestDto> dtos = mapper.mapChargesDtlListToDto(List.of(entity));

        assertNotNull(dtos);
        assertEquals(1, dtos.size());
    }

    @Test
    void mapEmailFaxDtlFromDto_Success() {
        NotifyPartyRequestDto dto = NotifyPartyRequestDto.builder()
                .detRowId(1L)
                .addressType("CAN")
                .email1("test@test.com")
                .build();

        ShipBlManifestEmailFaxDtl entity = mapper.mapEmailFaxDtlFromDto(dto, 1L);

        assertNotNull(entity);
        assertEquals("test@test.com", entity.getEmail1());
    }

    @Test
    void mapMafiDtlToDto_Success() {
        ShipBlManifestMafiDtl entity = new ShipBlManifestMafiDtl();
        entity.setId(new ShipBlManifestDtlId(1L, 1L));
        entity.setMafiRef("MAFI001");

        MafiRequestDto dto = mapper.mapMafiDtlToDto(entity);

        assertNotNull(dto);
        assertEquals("MAFI001", dto.getMafiRef());
    }
}
