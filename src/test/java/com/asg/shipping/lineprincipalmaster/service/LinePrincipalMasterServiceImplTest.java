package com.asg.shipping.lineprincipalmaster.service;

import com.asg.common.lib.dto.DiffObject;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.DiffUtil;
import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.common.entity.ShipLineMasterType;
import com.asg.shipping.common.repository.ShipLineMasterTypeRepository;
import com.asg.shipping.common.service.LovService;
import com.asg.shipping.lineprincipalmaster.dto.ChargeDetailDto;
import com.asg.shipping.lineprincipalmaster.dto.LinePrincipalMasterDto;
import com.asg.shipping.lineprincipalmaster.dto.LinePrincipalMasterUpdateDTO;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMaster;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterChargeDtl;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterPicDtl;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterUserRoleDtl;
import com.asg.shipping.lineprincipalmaster.repository.ShipLineMasterChargeDtlRepository;
import com.asg.shipping.lineprincipalmaster.repository.ShipLineMasterPicDtlRepository;
import com.asg.shipping.lineprincipalmaster.repository.ShipLineMasterRepository;
import com.asg.shipping.lineprincipalmaster.repository.ShipLineMasterUserRoleDtlRepository;
import com.asg.shipping.lineprincipalmaster.util.LinePrincipalMasterMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinePrincipalMasterServiceImplTest {

        @Test
        void updateLine_UsesExtendedTransactionTimeout() throws NoSuchMethodException {
                Transactional transactional = LinePrincipalMasterServiceImpl.class
                                .getMethod("updateLine", Long.class, LinePrincipalMasterUpdateDTO.class)
                                .getAnnotation(Transactional.class);

                assertNotNull(transactional);
                assertEquals(180, transactional.timeout());
        }

    @Test
    void diffUtil_IgnoresRecursiveMasterChargeRelationship() {
        ShipLineMaster oldLine = ShipLineMaster.builder()
                .linePoid(1L)
                .lineCode("LINE001")
                .lineName("Old Name")
                .build();
        ShipLineMaster newLine = ShipLineMaster.builder()
                .linePoid(1L)
                .lineCode("LINE001")
                .lineName("New Name")
                .build();

        ShipLineMasterChargeDtl oldCharge = ShipLineMasterChargeDtl.builder()
                .linePoid(1L)
                .detRowId(1L)
                .chargePoid(101L)
                .build();
        oldCharge.setLineMaster(oldLine);
        oldLine.setCharges(List.of(oldCharge));

        ShipLineMasterChargeDtl newCharge = ShipLineMasterChargeDtl.builder()
                .linePoid(1L)
                .detRowId(1L)
                .chargePoid(101L)
                .build();
        newCharge.setLineMaster(newLine);
        newLine.setCharges(List.of(newCharge));

        List<DiffObject> diffs = assertDoesNotThrow(() -> DiffUtil.createDiffList(oldLine, newLine, ShipLineMaster.class));

        assertNotNull(diffs);
    }

    @Test
    void diffUtil_IgnoresRecursiveChargeMasterRelationship() {
        ShipLineMaster oldLine = ShipLineMaster.builder().linePoid(1L).lineCode("LINE001").lineName("Line One").build();
        ShipLineMaster newLine = ShipLineMaster.builder().linePoid(1L).lineCode("LINE001").lineName("Line One").build();

        ShipLineMasterChargeDtl oldCharge = ShipLineMasterChargeDtl.builder()
                .linePoid(1L)
                .detRowId(1L)
                .chargePoid(101L)
                .lineChargeCode("OLD")
                .build();
        oldCharge.setLineMaster(oldLine);
        oldLine.setCharges(List.of(oldCharge));

        ShipLineMasterChargeDtl newCharge = ShipLineMasterChargeDtl.builder()
                .linePoid(1L)
                .detRowId(1L)
                .chargePoid(101L)
                .lineChargeCode("NEW")
                .build();
        newCharge.setLineMaster(newLine);
        newLine.setCharges(List.of(newCharge));

        List<DiffObject> diffs = assertDoesNotThrow(() -> DiffUtil.createDiffList(oldCharge, newCharge, ShipLineMasterChargeDtl.class));

        assertNotNull(diffs);
    }

        @Test
        void diffUtil_SupportsContainerTypeChildRows() {
                ShipLineMasterType oldContainerType = new ShipLineMasterType();
                oldContainerType.setLinePoid(1L);
                oldContainerType.setDetRowId(1L);
                oldContainerType.setContainerTypePoid(101L);

                ShipLineMasterType newContainerType = new ShipLineMasterType();
                newContainerType.setLinePoid(1L);
                newContainerType.setDetRowId(1L);
                newContainerType.setContainerTypePoid(202L);

                List<DiffObject> diffs = assertDoesNotThrow(() -> DiffUtil.createDiffList(oldContainerType, newContainerType, ShipLineMasterType.class));

                assertNotNull(diffs);
        }

        @Test
        void diffUtil_SupportsUserRoleChildRows() {
                ShipLineMasterUserRoleDtl oldUserRole = ShipLineMasterUserRoleDtl.builder()
                                .linePoid(1L)
                                .detRowId(1L)
                                .userRolePoid(101L)
                                .build();
                ShipLineMasterUserRoleDtl newUserRole = ShipLineMasterUserRoleDtl.builder()
                                .linePoid(1L)
                                .detRowId(1L)
                                .userRolePoid(202L)
                                .build();

                List<DiffObject> diffs = assertDoesNotThrow(() -> DiffUtil.createDiffList(oldUserRole, newUserRole, ShipLineMasterUserRoleDtl.class));

                assertNotNull(diffs);
        }

        @Test
        void diffUtil_SupportsPicChildRows() {
                ShipLineMasterPicDtl oldPic = ShipLineMasterPicDtl.builder()
                                .linePoid(1L)
                                .detRowId(1L)
                                .departmentPoid(101L)
                                .handledUserPoid(201L)
                                .remarks("old")
                                .build();
                ShipLineMasterPicDtl newPic = ShipLineMasterPicDtl.builder()
                                .linePoid(1L)
                                .detRowId(1L)
                                .departmentPoid(102L)
                                .handledUserPoid(202L)
                                .remarks("new")
                                .build();

                List<DiffObject> diffs = assertDoesNotThrow(() -> DiffUtil.createDiffList(oldPic, newPic, ShipLineMasterPicDtl.class));

                assertNotNull(diffs);
        }

    @Mock
    private ShipLineMasterRepository lineRepository;

    @Mock
    private ShipLineMasterChargeDtlRepository chargeDtlRepository;

    @Mock
    private ShipLineMasterTypeRepository containerTypeRepository;

    @Mock
    private ShipLineMasterUserRoleDtlRepository userRoleDtlRepository;

    @Mock
    private ShipLineMasterPicDtlRepository picDtlRepository;

    @Mock
    private DocumentSearchService documentSearchService;

    @Mock
    private DocumentDeleteService documentDeleteService;

    @Mock
    private LovService lovService;

    @Mock
    private LinePrincipalMasterMapper mapper;

    @Mock
    private LoggingService loggingService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private LinePrincipalMasterServiceImpl service;

    @Test
    void getLine_EnrichesBillToByCode() {
        ShipLineMaster line = ShipLineMaster.builder()
                .linePoid(1L)
                .groupPoid(10L)
                .companyPoid(20L)
                .billTo("PR001")
                .build();
        LinePrincipalMasterDto dto = LinePrincipalMasterDto.builder()
                .linePoid(1L)
                .charges(new ArrayList<>())
                .containerTypes(new ArrayList<>())
                .userRoles(new ArrayList<>())
                .picDetails(new ArrayList<>())
                .build();

        when(lineRepository.findByLinePoidAndGroupPoid(1L, 10L)).thenReturn(Optional.of(line));
        when(mapper.mapToDto(line)).thenReturn(dto);
        when(chargeDtlRepository.findByLinePoidOrderByDetRowId(1L)).thenReturn(List.of());
        when(containerTypeRepository.findByLinePoidOrderByDetRowId(1L)).thenReturn(List.of());
        when(userRoleDtlRepository.findByLinePoidOrderByDetRowId(1L)).thenReturn(List.of());
        when(picDtlRepository.findByLinePoidOrderByDetRowId(1L)).thenReturn(List.of());
        when(mapper.mapChargeDetailsToDto(any())).thenReturn(new ArrayList<>());
        when(mapper.mapContainerTypeDetailsToDto(any())).thenReturn(new ArrayList<>());
        when(mapper.mapUserRoleDetailsToDto(any())).thenReturn(new ArrayList<>());
        when(mapper.mapPicDetailsToDto(any())).thenReturn(new ArrayList<>());
        when(lovService.getLovItemByCode("PR001", "PRINCIPAL_MASTER_FOR_PDA", 10L, 20L, 30L))
                .thenReturn(new LovItem());

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(20L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(30L);

            LinePrincipalMasterDto result = service.getLine(1L);

            assertNotNull(result);
            verify(lovService).getLovItemByCode("PR001", "PRINCIPAL_MASTER_FOR_PDA", 10L, 20L, 30L);
        }
    }

    @Test
    void deleteLine_UsesDocumentDeleteService() {
        ShipLineMaster line = ShipLineMaster.builder()
                .linePoid(1L)
                .groupPoid(10L)
                .active("Y")
                .deleted("N")
                .build();
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("cleanup");

        when(lineRepository.findByLinePoidAndGroupPoid(1L, 10L)).thenReturn(Optional.of(line));
        when(lineRepository.save(any(ShipLineMaster.class))).thenReturn(line);
        when(documentDeleteService.deleteDocument(1L, "SHIP_LINE_MASTER", "LINE_POID", deleteReasonDto, null))
                .thenReturn("SUCCESS");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<com.asg.common.lib.utility.ASGHelperUtils> mockedHelper = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-007");
            mockedHelper.when(() -> getCurrentUser()).thenReturn("tester");

            service.deleteLine(1L, deleteReasonDto);

            verify(documentDeleteService).deleteDocument(1L, "SHIP_LINE_MASTER", "LINE_POID", deleteReasonDto, null);
            verify(loggingService).createLogDetailsEntry("100-007", "1", "Deleted", "N", "Y", "KeyId = LINE_POID:1", "SHIP_LINE_MASTER");
        }
    }

    @Test
    void updateLine_OmittedChargeIsNotDeleted() {
        ShipLineMaster line = ShipLineMaster.builder()
                .linePoid(1L)
                .groupPoid(10L)
                .lineCode("LINE001")
                .lineName("Line One")
                .build();
        ShipLineMasterChargeDtl existingOne = ShipLineMasterChargeDtl.builder().linePoid(1L).detRowId(1L).chargePoid(101L).build();
        ShipLineMasterChargeDtl existingTwo = ShipLineMasterChargeDtl.builder().linePoid(1L).detRowId(2L).chargePoid(102L).build();
        ChargeDetailDto updateCharge = ChargeDetailDto.builder().detRowId(1L).chargePoid(101L).build();
        LinePrincipalMasterUpdateDTO dto = LinePrincipalMasterUpdateDTO.builder()
                .lineCode("LINE001")
                .lineName("Line One")
                .charges(List.of(updateCharge))
                .build();
        LinePrincipalMasterDto response = LinePrincipalMasterDto.builder().linePoid(1L).build();

        when(lineRepository.findByLinePoidAndGroupPoid(1L, 10L)).thenReturn(Optional.of(line));
        when(lineRepository.existsByLineCodeAndGroupPoidExcluding("LINE001", 10L, 1L)).thenReturn(false);
        when(lineRepository.existsByLineNameAndGroupPoidExcluding("Line One", 10L, 1L)).thenReturn(false);
        when(lineRepository.save(line)).thenReturn(line);
        when(chargeDtlRepository.findByLinePoidOrderByDetRowId(1L)).thenReturn(List.of(existingOne, existingTwo));
        when(chargeDtlRepository.existsByLinePoidAndChargePoidExcluding(1L, 101L, 1L)).thenReturn(false);
        when(mapper.mapToDto(line)).thenReturn(response);
        when(mapper.mapChargeDetailsToDto(any())).thenReturn(new ArrayList<>());
        when(mapper.mapContainerTypeDetailsToDto(any())).thenReturn(new ArrayList<>());
        when(mapper.mapUserRoleDetailsToDto(any())).thenReturn(new ArrayList<>());
        when(mapper.mapPicDetailsToDto(any())).thenReturn(new ArrayList<>());
        when(containerTypeRepository.findByLinePoidOrderByDetRowId(1L)).thenReturn(List.of());
        when(userRoleDtlRepository.findByLinePoidOrderByDetRowId(1L)).thenReturn(List.of());
        when(picDtlRepository.findByLinePoidOrderByDetRowId(1L)).thenReturn(List.of());

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<com.asg.common.lib.utility.ASGHelperUtils> mockedHelper = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(30L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(20L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-007");
            mockedHelper.when(() -> getCurrentUser()).thenReturn("tester");

            LinePrincipalMasterDto result = service.updateLine(1L, dto);

            assertNotNull(result);
            verify(chargeDtlRepository, never()).deleteById(eq(new com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterChargeDtlId(1L, 2L)));
        }
    }

    @Test
    void updateLine_ChargeDeleteActionDeletesTargetRow() {
        ShipLineMaster line = ShipLineMaster.builder()
                .linePoid(1L)
                .groupPoid(10L)
                .lineCode("LINE001")
                .lineName("Line One")
                .build();
        ShipLineMasterChargeDtl existingOne = ShipLineMasterChargeDtl.builder().linePoid(1L).detRowId(1L).chargePoid(101L).build();
        ChargeDetailDto deleteCharge = ChargeDetailDto.builder().detRowId(1L).actionType("ISDELETED").build();
        LinePrincipalMasterUpdateDTO dto = LinePrincipalMasterUpdateDTO.builder()
                .lineCode("LINE001")
                .lineName("Line One")
                .charges(List.of(deleteCharge))
                .build();
        LinePrincipalMasterDto response = LinePrincipalMasterDto.builder().linePoid(1L).build();

        when(lineRepository.findByLinePoidAndGroupPoid(1L, 10L)).thenReturn(Optional.of(line));
        when(lineRepository.existsByLineCodeAndGroupPoidExcluding("LINE001", 10L, 1L)).thenReturn(false);
        when(lineRepository.existsByLineNameAndGroupPoidExcluding("Line One", 10L, 1L)).thenReturn(false);
        when(lineRepository.save(line)).thenReturn(line);
        when(chargeDtlRepository.findByLinePoidOrderByDetRowId(1L)).thenReturn(List.of(existingOne));
        when(mapper.mapToDto(line)).thenReturn(response);
        when(mapper.mapChargeDetailsToDto(any())).thenReturn(new ArrayList<>());
        when(mapper.mapContainerTypeDetailsToDto(any())).thenReturn(new ArrayList<>());
        when(mapper.mapUserRoleDetailsToDto(any())).thenReturn(new ArrayList<>());
        when(mapper.mapPicDetailsToDto(any())).thenReturn(new ArrayList<>());
        when(containerTypeRepository.findByLinePoidOrderByDetRowId(1L)).thenReturn(List.of());
        when(userRoleDtlRepository.findByLinePoidOrderByDetRowId(1L)).thenReturn(List.of());
        when(picDtlRepository.findByLinePoidOrderByDetRowId(1L)).thenReturn(List.of());

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<com.asg.common.lib.utility.ASGHelperUtils> mockedHelper = mockStatic(com.asg.common.lib.utility.ASGHelperUtils.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(30L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(20L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-007");
            mockedHelper.when(() -> getCurrentUser()).thenReturn("tester");

            LinePrincipalMasterDto result = service.updateLine(1L, dto);

            assertNotNull(result);
            verify(chargeDtlRepository).deleteById(eq(new com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterChargeDtlId(1L, 1L)));
        }
    }
}