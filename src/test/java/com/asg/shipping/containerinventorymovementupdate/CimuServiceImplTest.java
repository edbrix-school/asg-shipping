package com.asg.shipping.containerinventorymovementupdate;

import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.containerinventorymovementupdate.dto.*;
import com.asg.shipping.containerinventorymovementupdate.repository.jdbc.*;
import com.asg.shipping.containerinventorymovementupdate.service.impl.CimuServiceImpl;
import com.asg.shipping.containerinventorymovementupdate.util.ExcelInspectionParser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CimuServiceImplTest {

    @Mock
    private CimuLovSuggestionRepository suggestionRepository;
    @Mock
    private CimuQueryRepository queryRepository;
    @Mock
    private CimuUpdateRepository updateRepository;
    @Mock
    private CimuDemurrageRepository demurrageRepository;
    @Mock
    private CimuRightsRepository rightsRepository;

    @InjectMocks
    private CimuServiceImpl service;

    private MockedStatic<UserContext> userContextMock;

    @BeforeEach
    void setupUserContext() {
        userContextMock = Mockito.mockStatic(UserContext.class);
        userContextMock.when(UserContext::getGroupPoid).thenReturn(10L);
        userContextMock.when(UserContext::getCompanyPoid).thenReturn(20L);
        userContextMock.when(UserContext::getUserPoid).thenReturn(1L);
        userContextMock.when(UserContext::getUserId).thenReturn("admin");
    }

    @AfterEach
    void tearDown() {
        userContextMock.close();
    }

    // ---------- suggestContainers ----------

    @Test
    void suggestContainers_success() {
        when(suggestionRepository.suggestContainerNos(any(), any(), any(), any()))
                .thenReturn(List.of("CONT001", "CONT002"));

        SuggestContainerResponse response = service.suggestContainers("CONT00");

        assertEquals(2, response.getItems().size());
    }

    @Test
    void suggestContainers_invalidQuery() {
        assertThrows(ValidationException.class,
                () -> service.suggestContainers("ABC"));
    }

    // ---------- queryScreenData ----------

    @Test
    void queryScreenData_success() {
        QueryCimuRequest request = new QueryCimuRequest();
        request.setContainerNo("CONT001");

        when(queryRepository.fetchContainerInfo(any(), any()))
                .thenReturn(List.of(new ContainerInfoDto()));
        when(queryRepository.fetchHistoryByContainerNo(any()))
                .thenReturn(List.of(new ContainerHistoryRowDto()));
        when(rightsRepository.hasDocRight(any(), any()))
                .thenReturn(true);

        QueryCimuResponse response = service.queryScreenData(request);

        assertNotNull(response);
        assertTrue(response.getPermissions().isCanEditActualDischargeDate());
    }

    @Test
    void queryScreenData_missingInputs() {
        assertThrows(ValidationException.class,
                () -> service.queryScreenData(new QueryCimuRequest()));
    }

    @Test
    void queryScreenData_rightsLookupFailureFallsBackToFalse() {
        QueryCimuRequest request = new QueryCimuRequest();
        request.setContainerNo("CONT001");
        when(rightsRepository.hasDocRight(any(), any()))
                .thenThrow(new RuntimeException("rights down"));
        when(queryRepository.fetchContainerInfo(any(), any()))
                .thenReturn(List.of(new ContainerInfoDto()));
        when(queryRepository.fetchHistoryByContainerNo(any()))
                .thenReturn(List.of());

        QueryCimuResponse response = service.queryScreenData(request);

        assertNotNull(response);
        assertFalse(response.getPermissions().isCanEditActualDischargeDate());
    }

    // ---------- updateContainerData ----------

    @Test
    void updateContainerData_success() {
        UpdateCimuRequest request = new UpdateCimuRequest();
        request.setTransactionPoid(100L);
        request.setContainerNo("CONT001");

        when(queryRepository.containerExistsInBl(any(), any())).thenReturn(true);
        when(updateRepository.callProcShipCntInvtUpdate(
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("TRUE");

        UpdateCimuResponse response = service.updateContainerData(request);

        assertEquals("TRUE", response.getStatus());
    }

    @Test
    void updateContainerData_applyAll_blankStatusDefaultsTrue() {
        UpdateCimuRequest request = new UpdateCimuRequest();
        request.setTransactionPoid(100L);
        request.setApplyToAllContainers(true);

        when(updateRepository.callProcShipCntInvtUpdate(
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(" ");

        UpdateCimuResponse response = service.updateContainerData(request);

        assertEquals("TRUE", response.getStatus());
    }

    @Test
    void updateContainerData_holdReturnWithRights_success() {
        UpdateCimuRequest request = new UpdateCimuRequest();
        request.setTransactionPoid(100L);
        request.setContainerNo("CONT001");
        request.setHoldReturnForm(true);
        request.setWithConsigneeFull("2025-01-01 10:00:00");
        request.setEmptyIn("2025-01-02 10:00:00");
        request.setActualDischargeDate("2025-01-03 10:00:00");

        when(queryRepository.containerExistsInBl(100L, "CONT001")).thenReturn(true);
        when(updateRepository.callProcShipCntInvtUpdate(
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                // Procedure returns "TRUE" prefixed with audit detail messages — must be treated as SUCCESS
                .thenReturn("TRUE, Free days not updated, bl issue type not updated, Return form hold");

        UpdateCimuResponse response = service.updateContainerData(request);

        // Status starts with TRUE → success, full message returned as-is
        assertTrue(response.getStatus().toUpperCase().startsWith("TRUE"));
        assertTrue(response.getStatus().contains("Return form hold"));
    }

    @Test
    void updateContainerData_statusWithAuditMessages_treatedAsSuccess() {
        // Regression: procedure appends audit detail after "TRUE," — must NOT throw 400
        UpdateCimuRequest request = new UpdateCimuRequest();
        request.setTransactionPoid(100L);
        request.setContainerNo("CONT001");
        when(queryRepository.containerExistsInBl(100L, "CONT001")).thenReturn(true);
        when(updateRepository.callProcShipCntInvtUpdate(
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("TRUE, Free days updated, bl issue type updated");

        UpdateCimuResponse response = service.updateContainerData(request);

        assertTrue(response.getStatus().toUpperCase().startsWith("TRUE"));
    }

    @Test
    void updateContainerData_missingRequest() {
        assertThrows(ValidationException.class, () -> service.updateContainerData(null));
    }

    @Test
    void updateContainerData_requiresContainerWhenNotApplyAll() {
        UpdateCimuRequest request = new UpdateCimuRequest();
        request.setTransactionPoid(100L);
        request.setApplyToAllContainers(false);

        assertThrows(ValidationException.class, () -> service.updateContainerData(request));
    }

    @Test
    void updateContainerData_blNumberMismatch() {
        UpdateCimuRequest request = new UpdateCimuRequest();
        request.setTransactionPoid(100L);
        request.setContainerNo("CONT001");
        request.setBlNumber("BLX");

        when(queryRepository.blNumberMatchesTransaction(100L, "BLX")).thenReturn(false);

        assertThrows(ValidationException.class, () -> service.updateContainerData(request));
    }

    @Test
    void updateContainerData_containerNotInBl() {
        UpdateCimuRequest request = new UpdateCimuRequest();
        request.setTransactionPoid(100L);
        request.setContainerNo("CONT001");

        when(queryRepository.containerExistsInBl(100L, "CONT001")).thenReturn(false);

        assertThrows(ValidationException.class, () -> service.updateContainerData(request));
    }

    @Test
    void updateContainerData_missingUserContext() {
        userContextMock.when(UserContext::getUserPoid).thenReturn(null);
        UpdateCimuRequest request = new UpdateCimuRequest();
        request.setTransactionPoid(100L);
        request.setContainerNo("CONT001");
        when(queryRepository.containerExistsInBl(100L, "CONT001")).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.updateContainerData(request));
    }

    @Test
    void updateContainerData_holdReturnWithoutRights() {
        UpdateCimuRequest request = new UpdateCimuRequest();
        request.setTransactionPoid(100L);
        request.setContainerNo("CONT001");
        request.setHoldReturnForm(true);
        when(queryRepository.containerExistsInBl(100L, "CONT001")).thenReturn(true);
        when(updateRepository.callProcShipCntInvtUpdate(
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("Free days not updated, bl issue type not updated, User have no right to hold Return Form");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.updateContainerData(request));
        assertTrue(ex.getMessage().contains("User have no right to hold Return Form"));
    }

    // ---------- socUpdate ----------

    @Test
    void socUpdate_success() {
        SocUpdateRequest request = new SocUpdateRequest();
        request.setBlNumber("BL001");

        when(updateRepository.callProcShipCntSocUpdate(any(), any()))
                .thenReturn("TRUE");

        SocUpdateResponse response = service.socUpdate(request);

        assertEquals("TRUE", response.getStatus());
    }

    @Test
    void socUpdate_missingBlNumber() {
        SocUpdateRequest request = new SocUpdateRequest();
        assertThrows(ValidationException.class, () -> service.socUpdate(request));
    }

    @Test
    void socUpdate_missingUserContext() {
        userContextMock.when(UserContext::getUserPoid).thenReturn(null);
        SocUpdateRequest request = new SocUpdateRequest();
        request.setBlNumber("BL001");

        assertThrows(ValidationException.class, () -> service.socUpdate(request));
    }

    // ---------- calculateDemurrage ----------

    @Test
    void calculateDemurrage_success() {
        DemurrageCalculateRequest request = new DemurrageCalculateRequest();
        request.setTransactionPoid(100L);
        request.setContainerNo("CONT001");
        request.setDemDt(LocalDate.now().plusDays(1));

        when(demurrageRepository.calculateDemurrage(any(), any(), any()))
                .thenReturn(BigDecimal.TEN);
        when(demurrageRepository.getImportTotalMessage(any(), any(), any()))
                .thenReturn("Total amount need to collect =10");
        when(demurrageRepository.getPortDays(any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(5));

        DemurrageCalculateResponse response = service.calculateDemurrage(request);

        assertEquals(BigDecimal.TEN, response.getDemurrageAmount());
        assertEquals(BigDecimal.valueOf(5), response.getPortDays());
        assertNotNull(response.getCollectedSummaryMessage());
    }

    @Test
    void calculateDemurrage_missingDate() {
        DemurrageCalculateRequest request = new DemurrageCalculateRequest();
        request.setTransactionPoid(100L);
        request.setContainerNo("CONT001");
        request.setDemDt(null);

        assertThrows(ValidationException.class, () -> service.calculateDemurrage(request));
    }

    @Test
    void calculateDemurrage_previousDate() {
        DemurrageCalculateRequest request = new DemurrageCalculateRequest();
        request.setTransactionPoid(100L);
        request.setContainerNo("CONT001");
        request.setDemDt(LocalDate.now().minusDays(1));

        when(demurrageRepository.calculateDemurrage(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(demurrageRepository.getImportTotalMessage(any(), any(), any()))
                .thenReturn(null);
        when(demurrageRepository.getPortDays(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        DemurrageCalculateResponse response = service.calculateDemurrage(request);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getDemurrageAmount());
        assertEquals(BigDecimal.ZERO, response.getPortDays());
    }

    // ---------- importFile ----------

    @Test
    void importFile_emptyFile() {
        MockMultipartFile file =
                new MockMultipartFile("file", "test.xlsx",
                        "application/vnd.ms-excel", new byte[0]);

        assertThrows(ValidationException.class,
                () -> service.importFile(file));
    }

    @Test
    void importFile_invalidExtension() {
        MockMultipartFile file =
                new MockMultipartFile("file", "test.txt",
                        "text/plain", "abc".getBytes());

        assertThrows(ValidationException.class,
                () -> service.importFile(file));
    }

    @Test
    void importFile_success() {
        MockMultipartFile file =
                new MockMultipartFile("file", "inspection.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "dummy".getBytes());

        ExcelInspectionRow row1 = ExcelInspectionRow.builder()
                .containerNo("CONT001")
                .otherColumns(Map.of(
                        "MOVES DATE", "2025-01-01",
                        "LINE NAME", "MAERSK",
                        "SIZE/TYPE", "20GP",
                        "LOCATION_STATUS", "YARD",
                        "CONTAINER_STATUS", "SOUND",
                        "REMARKS", "OK"))
                .build();
        ExcelInspectionRow row2 = ExcelInspectionRow.builder()
                .containerNo("")
                .otherColumns(Map.of("REMARKS", "skip"))
                .build();

        try (MockedStatic<ExcelInspectionParser> parserMock = Mockito.mockStatic(ExcelInspectionParser.class)) {
            parserMock.when(() -> ExcelInspectionParser.parseFullExcelRows(any()))
                    .thenReturn(List.of(row1, row2));

            InspectionUploadResponse response = service.importFile(file);

            assertNotNull(response.getUploadId());
            assertEquals(2L, response.getRowCount());
            verify(queryRepository).clearInspectionTempTable();
            verify(queryRepository).insertInspectionTempTable(any(), eq("CONT001"), any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    void importFile_noRowsInParsedFile() {
        MockMultipartFile file =
                new MockMultipartFile("file", "inspection.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "dummy".getBytes());
        try (MockedStatic<ExcelInspectionParser> parserMock = Mockito.mockStatic(ExcelInspectionParser.class)) {
            parserMock.when(() -> ExcelInspectionParser.parseFullExcelRows(any()))
                    .thenReturn(List.of());
            assertThrows(ValidationException.class, () -> service.importFile(file));
        }
    }

    // ---------- loadContainerDetails ----------

    @Test
    void loadContainerDetails_success() {
        when(updateRepository.callProcShCntInspectXlUpload(any(), any(), any()))
                .thenReturn(Map.of(
                        "transactionPoid", "123",
                        "status", "Success"));

        InspectionLoadResponse response = service.loadContainerDetails();

        assertTrue(response.getSuccess());
        assertEquals("123", response.getTransactionPoid());
    }

    @Test
    void loadContainerDetails_missingUserContext() {
        userContextMock.when(UserContext::getUserPoid).thenReturn(null);

        assertThrows(ValidationException.class, () -> service.loadContainerDetails());
    }

    @Test
    void loadContainerDetails_usesDefaultsAndLoadedStatus() {
        userContextMock.when(UserContext::getGroupPoid).thenReturn(null);
        userContextMock.when(UserContext::getCompanyPoid).thenReturn(null);
        when(updateRepository.callProcShCntInspectXlUpload(1L, 1L, 1L))
                .thenReturn(Map.of("transactionPoid", "456", "status", "rows loaded"));

        InspectionLoadResponse response = service.loadContainerDetails();

        assertTrue(response.getSuccess());
        assertEquals("456", response.getTransactionPoid());
    }
}

