package com.asg.shipping.containerinventorymovementupdate;

import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.containerinventorymovementupdate.dto.*;
import com.asg.shipping.containerinventorymovementupdate.repository.jdbc.*;
import com.asg.shipping.containerinventorymovementupdate.service.impl.CimuServiceImpl;
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

    // ---------- updateContainerData ----------

    @Test
    void updateContainerData_success() {
        UpdateCimuRequest request = new UpdateCimuRequest();
        request.setTransactionPoid(100L);
        request.setContainerNo("CONT001");

        when(queryRepository.containerExistsInBl(any(), any())).thenReturn(true);
        when(updateRepository.callProcShipCntInvtUpdate(
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("TRUE");

        UpdateCimuResponse response = service.updateContainerData(request);

        assertEquals("TRUE", response.getStatus());
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

    // ---------- calculateDemurrage ----------

    @Test
    void calculateDemurrage_success() {
        DemurrageCalculateRequest request = new DemurrageCalculateRequest();
        request.setTransactionPoid(100L);
        request.setContainerNo("CONT001");
        request.setDemDt(LocalDate.now().plusDays(1).toString());

        when(demurrageRepository.calculateDemurrage(any(), any(), any()))
                .thenReturn(BigDecimal.TEN);
        when(demurrageRepository.getImportTotalMessage(any(), any(), any()))
                .thenReturn("Total amount need to collect =10");
        when(queryRepository.fetchTotalCollectedAmount(any(), any()))
                .thenReturn(BigDecimal.ONE);

        DemurrageCalculateResponse response = service.calculateDemurrage(request);

        assertEquals(BigDecimal.TEN, response.getDemurrageAmount());
        assertEquals(BigDecimal.ONE, response.getTotalCollectedAmount());
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
}

