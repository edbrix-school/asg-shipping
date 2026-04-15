package com.asg.shipping.importmanifestbl.service.impl;

import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.shipping.address.entity.AddressDetails;
import com.asg.shipping.importmanifestupdate.dto.LoadEmailFaxRequestDto;
import com.asg.shipping.importmanifestupdate.event.BlManifestSaveEvent;
import com.asg.shipping.shippingFFChargeMaster.entity.ShipChargeMaster;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.shipping.importmanifestupdate.dto.*;
import com.asg.shipping.importmanifestupdate.entity.*;
import com.asg.shipping.importmanifestupdate.respository.*;
import com.asg.shipping.importmanifestupdate.service.BlManifestValidationService;
import com.asg.shipping.importmanifestbl.dto.*;
import com.asg.shipping.importmanifestbl.repository.ContainerDropdownRepository;
import com.asg.shipping.shippingFFChargeMaster.repository.ShipChargeMasterRepository;
import com.asg.shipping.address.entity.AddressDetailsRepository;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.data.domain.Pageable;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.FilterRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportManifestServiceImplTest {

    @Mock private ShipBlManifestHdrRepository headerRepository;
    @Mock private ImportManifestBlProcRepository procRepository;
    @Mock private ShipBlManifestGeneralDtlRepository generalDtlRepository;
    @Mock private ShipBlManifestCargoDtlRepository cargoDtlRepository;
    @Mock private ShipBlManifestContainerDtlRepository containerDtlRepository;
    @Mock private ShipBlManifestChargesDtlRepository chargesDtlRepository;
    @Mock private ShipBlManifestPartBLRepository containerPrtRepository;
    @Mock private ShipBlManifestEmailFaxDtlRepository emailFaxDtlRepository;
    @Mock private ShipBlManifestMafiDtlRepository mafiDtlRepository;
    @Mock private DocumentSearchService documentService;
    @Mock private PrintService printService;
    @Mock private DataSource dataSource;
    @Mock private ContainerDropdownRepository containerDropdownRepository;
    @Mock private BlManifestValidationRepository validationRepository;
    @Mock private DocumentDeleteService documentDeleteService;
    @Mock private LoggingService loggingService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private BlManifestValidationService blManifestValidationService;
    @Mock private ShipChargeMasterRepository chargeMasterRepository;
    @Mock private com.asg.shipping.importmanifestupdate.service.ImportManifestBlServiceImpl updateService;
    @Mock private AddressDetailsRepository addressDetailsRepository;
    
    @InjectMocks
    private ImportManifestServiceImpl service;

    private ShipBlManifestHdr createHeader() {
        ShipBlManifestHdr h = new ShipBlManifestHdr();
        h.setTransactionPoid(1L);
        h.setBlType("IMPORT");
        h.setBlNumber("BL123");
        h.setTransactionDate(LocalDate.now());
        return h;
    }

    private void mockUserContext(MockedStatic<UserContext> mocked) {
        mocked.when(UserContext::getDocumentId).thenReturn("DOC1");
        mocked.when(UserContext::getCompanyPoid).thenReturn(1L);
        mocked.when(UserContext::getGroupPoid).thenReturn(1L);
        mocked.when(UserContext::getUserPoid).thenReturn(1L);
        mocked.when(UserContext::getTimeZoneCode).thenReturn("UTC");
    }

    @Test
    void updateImportManifestBl_Success() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            
            ShipBlManifestHdr header = createHeader();
            ImportManifestBlDto dto = ImportManifestBlDto.builder()
                    .transactionPoid(1L)
                    .vesselVoyagePoid(100L)
                    .blNumber("BL123")
                    .cargo("FCL")
                    .blType("IMPORT")
                    .loadPortPoid(1L)
                    .transactionDate(LocalDate.now())
                    .containers(new ArrayList<>())
                    .charges(new ArrayList<>())
                    .generalCargoDetails(new ArrayList<>())
                    .build();

            when(headerRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(header));
            when(headerRepository.saveAndFlush(any())).thenAnswer(inv -> {
                ShipBlManifestHdr h = inv.getArgument(0);
                h.setTransactionPoid(1L);
                return h;
            });
            lenient().when(validationRepository.getVoyageCompanyPoid(anyLong())).thenReturn(1L);
            lenient().when(validationRepository.isValidFinancialYear(anyLong(), any())).thenReturn(true);

            ImportManifestBlResponseDto result = service.updateImportManifestBl(1L, dto);
            assertNotNull(result);
        }
    }

    @Test
    void updateImportManifestBl_Complex_Success() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);

            ShipBlManifestHdr header = createHeader();
            ContainerDto container = ContainerDto.builder()
                    .detRowId(1L)
                    .actionType("ACTION_ISCREATED")
                    .containerNumber("CONT123")
                    .equipmentIsoType("20GP")
                    .build();

            GeneralCargoDto cargo = GeneralCargoDto.builder()
                    .detRowId(1L)
                    .actionType("ACTION_ISCREATED")
                    .description("TEST")
                    .build();

            ImportManifestBlDto dto = ImportManifestBlDto.builder()
                    .transactionPoid(1L)
                    .vesselVoyagePoid(100L)
                    .blNumber("BL123")
                    .cargo("FCL")
                    .blType("IMPORT")
                    .loadPortPoid(1L)
                    .transactionDate(LocalDate.now())
                    .containers(List.of(container))
                    .generalCargoDetails(List.of(cargo))
                    .build();

            when(headerRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(header));
            when(headerRepository.saveAndFlush(any())).thenAnswer(inv -> {
                ShipBlManifestHdr h = inv.getArgument(0);
                h.setTransactionPoid(1L);
                return h;
            });
            lenient().when(validationRepository.getVoyageCompanyPoid(anyLong())).thenReturn(1L);
            lenient().when(validationRepository.isValidFinancialYear(anyLong(), any())).thenReturn(true);
            when(containerDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);
            when(generalDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);

            ImportManifestBlResponseDto result = service.updateImportManifestBl(1L, dto);
            assertNotNull(result);
            verify(containerDtlRepository).save(any());
            verify(generalDtlRepository).save(any());
        }
    }

    @Test
    void updateImportManifestBl_UpdateDelete_Success() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);

            ShipBlManifestHdr header = createHeader();
            ContainerDto containerUpdate = ContainerDto.builder()
                    .detRowId(1L)
                    .actionType("ISUPDATED")
                    .containerNumber("CONT123-UPD")
                    .build();

            ContainerDto containerDelete = ContainerDto.builder()
                    .detRowId(2L)
                    .actionType("ISDELETED")
                    .build();

            ImportManifestBlDto dto = ImportManifestBlDto.builder()
                    .transactionPoid(1L)
                    .vesselVoyagePoid(100L)
                    .blNumber("BL123")
                    .cargo("FCL")
                    .blType("IMPORT")
                    .loadPortPoid(1L)
                    .transactionDate(LocalDate.now())
                    .containers(List.of(containerUpdate, containerDelete))
                    .build();

            when(headerRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(header));
            when(headerRepository.saveAndFlush(any())).thenReturn(header);
            lenient().when(validationRepository.getVoyageCompanyPoid(anyLong())).thenReturn(1L);
            lenient().when(validationRepository.isValidFinancialYear(anyLong(), any())).thenReturn(true);
            
            ShipBlManifestContainerDtl existingContainer = new ShipBlManifestContainerDtl();
            existingContainer.setId(new ShipBlManifestDtlId(1L, 1L));
            when(containerDtlRepository.findById(any())).thenReturn(Optional.of(existingContainer));
            when(containerDtlRepository.findAllById(any())).thenReturn(List.of(new ShipBlManifestContainerDtl()));

            service.updateImportManifestBl(1L, dto);
            
            verify(containerDtlRepository).saveAll(any());
            verify(containerDtlRepository).deleteAllInBatch(any());
        }
    }

    @Test
    void updateImportManifestBl_ContainerDuplicate_Fail() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);

            ShipBlManifestHdr header = createHeader();
            ContainerDto containerUpdate = ContainerDto.builder()
                    .detRowId(1L)
                    .actionType("ISUPDATED")
                    .containerNumber("CONT123")
                    .build();

            ImportManifestBlDto dto = ImportManifestBlDto.builder()
                    .transactionPoid(1L)
                    .vesselVoyagePoid(100L)
                    .blNumber("BL123")
                    .cargo("FCL")
                    .blType("IMPORT")
                    .loadPortPoid(1L)
                    .transactionDate(LocalDate.now())
                    .containers(List.of(containerUpdate))
                    .build();

            when(headerRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(header));
            when(headerRepository.saveAndFlush(any())).thenReturn(header);
            lenient().when(validationRepository.getVoyageCompanyPoid(anyLong())).thenReturn(1L);
            lenient().when(validationRepository.isValidFinancialYear(anyLong(), any())).thenReturn(true);

            ShipBlManifestContainerDtl existingContainer = new ShipBlManifestContainerDtl();
            existingContainer.setId(new ShipBlManifestDtlId(1L, 1L));
            existingContainer.setContainerNo("CONT123");
            when(containerDtlRepository.findById(any())).thenReturn(Optional.of(existingContainer));

            ShipBlManifestContainerDtl conflict = new ShipBlManifestContainerDtl();
            conflict.setId(new ShipBlManifestDtlId(1L, 2L)); // Different detRowId
            conflict.setContainerNo("CONT123");
            when(containerDtlRepository.findByIdTransactionPoidAndContainerNo(eq(1L), eq("CONT123")))
                    .thenReturn(Optional.of(conflict));

            assertThrows(ValidationException.class, () -> service.updateImportManifestBl(1L, dto));
        }
    }

    @Test
    void createImportManifestBl_AllDetails_Success() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);

            OtherNotifyDto otherNotifies = new OtherNotifyDto();
            otherNotifies.setNotify2Poid(202L);
            otherNotifies.setNotify3Poid(203L);

            ImportManifestBlDto dto = ImportManifestBlDto.builder()
                    .vesselVoyagePoid(100L)
                    .blNumber("BL123")
                    .cargo("FCL")
                    .blType("IMPORT")
                    .loadPortPoid(1L)
                    .transactionDate(LocalDate.now())
                    .consigneePoid(200L)
                    .notify1Poid(201L)
                    .otherNotifies(otherNotifies)
                    .generalCargoDetails(List.of(GeneralCargoDto.builder().detRowId(1L).build()))
                    .descriptionsAndMarks(List.of(DescriptionAndMarksDto.builder().detRowId(1L).build()))
                    .containers(List.of(ContainerDto.builder().detRowId(1L).build()))
                    .charges(List.of(ChargeDto.builder().chargePoid(1L).build()))
                    .partBls(List.of(PartBlDto.builder().detRowId(1L).build()))
                    .addressDetails(List.of(
                            AddressDetailsDto.builder().addressType("CONSIGNEE").build(),
                            AddressDetailsDto.builder().addressType("NOTIFY1").build(),
                            AddressDetailsDto.builder().addressType("NOTIFY2").build(),
                            AddressDetailsDto.builder().addressType("NOTIFY3").build()
                    ))
                    .mafiDetails(List.of(MafiDetailsDto.builder().detRowId(1L).build()))
                    .build();

            when(headerRepository.saveAndFlush(any())).thenAnswer(inv -> {
                ShipBlManifestHdr h = inv.getArgument(0);
                h.setTransactionPoid(1L);
                return h;
            });
            lenient().when(validationRepository.getVoyageCompanyPoid(anyLong())).thenReturn(1L);
            lenient().when(validationRepository.isValidFinancialYear(anyLong(), any())).thenReturn(true);

            when(generalDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);
            when(cargoDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);
            when(containerDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);
            when(chargesDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);
            when(containerPrtRepository.getMaxDetRowId(1L)).thenReturn(0L);
            when(emailFaxDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);
            when(mafiDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);

            ShipChargeMaster master = new ShipChargeMaster();
            master.setChargeType("TEST");
            master.setChargeName("TEST CHARGE");
            when(chargeMasterRepository.findByChargePoid(1L)).thenReturn(Optional.of(master));

            ImportManifestBlResponseDto result = service.createImportManifestBl(dto, 1L, 1L);

            assertNotNull(result);
            assertEquals(1L, result.getTransactionPoid());

            verify(generalDtlRepository).save(any());
            verify(cargoDtlRepository).save(any());
            verify(containerDtlRepository).save(any());
            verify(chargesDtlRepository).save(any());
            verify(containerPrtRepository).save(any());
            verify(emailFaxDtlRepository, times(4)).save(any());
            verify(mafiDtlRepository).save(any());
            verify(eventPublisher).publishEvent(any(BlManifestSaveEvent.class));
            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.CREATED), any(String.class), any(String.class));
            verify(loggingService, atLeastOnce()).createLogSummaryEntry(any(String.class), any(String.class), any(String.class));
        }
    }

    @Test
    void delete_Success() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);

            ShipBlManifestHdr header = createHeader();
            DeleteReasonDto deleteReason = new DeleteReasonDto();
            deleteReason.setDeleteReason("Test Reason");
            when(headerRepository.findById(1L)).thenReturn(Optional.of(header));

            service.delete(1L, deleteReason);

            verify(documentDeleteService).deleteDocument(eq(1L), eq("SHIP_BL_MANIFEST_HDR"), eq("TRANSACTION_POID"),
                    eq(deleteReason), any());
        }
    }

    @Test
    void getImportManifest_Success() {
        ShipBlManifestHdr header = createHeader();
        when(headerRepository.findById(1L)).thenReturn(Optional.of(header));
        when(updateService.getImportManifestBl(1L)).thenReturn(new ImportManifestBlRequestDto());
        ImportManifestBlDto result = service.getImportManifest(1L);
        assertNotNull(result);
    }

    @Test
    void getBlStatus_Success() {
        ShipBlManifestHdr header = createHeader();
        when(headerRepository.findById(1L)).thenReturn(Optional.of(header));
        when(procRepository.getBlStatus(1L)).thenReturn(new BlStatusResponseDto());
        
        BlStatusResponseDto result = service.getBlStatus(1L);
        assertNotNull(result);
    }

    @Test
    void updateEmailVerification_Success() {
        ShipBlManifestHdr header = createHeader();
        when(headerRepository.findById(1L)).thenReturn(Optional.of(header));
        EmailVerificationRequestDto req = new EmailVerificationRequestDto();
        when(procRepository.updateEmailVerification(eq(1L), eq(req))).thenReturn(new EmailVerificationResponseDto());
        
        EmailVerificationResponseDto result = service.updateEmailVerification(1L, req);
        assertNotNull(result);
    }

    @Test
    void resendCan_Success() {
        ShipBlManifestHdr header = createHeader();
        header.setVoyageTransactionPoid(100L);
        when(headerRepository.findById(1L)).thenReturn(Optional.of(header));
        when(procRepository.resendCan(100L, 1L)).thenReturn(new ResendCanResponseDto());
        
        ResendCanResponseDto result = service.resendCan(1L);
        assertNotNull(result);
    }

    @Test
    void updateImportManifestBl_ValidationFail_Voyage() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);

            ShipBlManifestHdr header = createHeader();
            when(headerRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(header));
            lenient().when(validationRepository.getVoyageCompanyPoid(anyLong())).thenReturn(2L); // 2L != 1L (User company)
            ImportManifestBlDto dto = ImportManifestBlDto.builder()
                    .vesselVoyagePoid(100L)
                    .blNumber("BL123")
                    .cargo("FCL")
                    .blType("IMPORT")
                    .loadPortPoid(null) // Should trigger validation failure
                    .transactionDate(LocalDate.now())
                    .build();
            assertThrows(ValidationException.class, () -> service.updateImportManifestBl(1L, dto));
        }
    }

    @Test
    void createImportManifestBl_Defaults_Success() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);

            ImportManifestBlDto dto = ImportManifestBlDto.builder()
                    .vesselVoyagePoid(100L)
                    .blNumber("BL123")
                    .loadPortPoid(1L)
                    .transactionDate(LocalDate.now())
                    .build();

            when(headerRepository.saveAndFlush(any())).thenAnswer(inv -> {
                ShipBlManifestHdr h = inv.getArgument(0);
                h.setTransactionPoid(1L);
                return h;
            });
            lenient().when(validationRepository.getVoyageCompanyPoid(anyLong())).thenReturn(1L);
            lenient().when(validationRepository.isValidFinancialYear(anyLong(), any())).thenReturn(true);

            ImportManifestBlResponseDto result = service.createImportManifestBl(dto, 1L, 1L);

            assertNotNull(result);
            verify(headerRepository).saveAndFlush(argThat(h ->
                    "IMPORT".equals(h.getBlType()) &&
                    "FCL-FCL".equals(h.getCargoType()) &&
                    Long.valueOf(800L).equals(h.getPortOfDischargePoid())
            ));
        }
    }

    @Test
    void createImportManifestBl_ValidationFail() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);

            ImportManifestBlDto dto = ImportManifestBlDto.builder()
                    .vesselVoyagePoid(100L)
                    .blNumber("BL123")
                    .cargo("FCL")
                    .blType("IMPORT")
                    .loadPortPoid(null) // Trigger Port of Loading Required
                    .transactionDate(LocalDate.now())
                    .build();

            assertThrows(ValidationException.class, () -> service.createImportManifestBl(dto, 1L, 1L));
        }
    }

    @Test
    void createImportManifestBl_ProcValidationFail() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);

            ImportManifestBlDto dto = ImportManifestBlDto.builder()
                    .vesselVoyagePoid(100L)
                    .blNumber("BL123")
                    .cargo("FCL")
                    .blType("IMPORT")
                    .loadPortPoid(1L)
                    .transactionDate(LocalDate.now())
                    .build();

            when(headerRepository.saveAndFlush(any())).thenAnswer(inv -> {
                ShipBlManifestHdr h = inv.getArgument(0);
                h.setTransactionPoid(1L);
                return h;
            });
            lenient().when(validationRepository.getVoyageCompanyPoid(anyLong())).thenReturn(1L);
            lenient().when(validationRepository.isValidFinancialYear(anyLong(), any())).thenReturn(true);

            doThrow(new RuntimeException("Proc Error")).when(procRepository)
                    .validateBeforeSave(anyLong(), anyLong(), any(), any(), any());

            assertThrows(RuntimeException.class, () -> service.createImportManifestBl(dto, 1L, 1L));
        }
    }

    @Test
    void sendEdiEmails_Success() {
        ShipBlManifestHdr header = createHeader();
        when(headerRepository.findById(1L)).thenReturn(Optional.of(header));
        when(procRepository.getEdiEmails(1L)).thenReturn(new SendEdiEmailsResponseDto());
        
        SendEdiEmailsResponseDto result = service.sendEdiEmails(1L);
        assertNotNull(result);
    }

    @Test
    void loadEmailFax_Success() {
        ShipBlManifestHdr header = createHeader();
        when(headerRepository.findById(1L)).thenReturn(Optional.of(header));
        LoadEmailFaxRequestDto req = new LoadEmailFaxRequestDto();
        req.setAddressMasterPoid(100L);
        req.setAddressType("CAN");
        
        AddressDetails address = new AddressDetails();
        address.setAddressPoid("1");
        address.setEmail("test@test.com");
        
        when(addressDetailsRepository.findByAddressMasterPoidAndAddressType(100L, "CAN")).thenReturn(List.of(address));
        
        LoadEmailFaxResponseDto result = service.loadEmailFax(1L, req);
        assertNotNull(result);
        assertFalse(result.getEmailFaxDetails().isEmpty());
    }

    @Test
    void getContainerTypesByVoyage_Success() {
        when(containerDropdownRepository.findContainerTypes(1L)).thenReturn(new ArrayList<>());
        when(containerDropdownRepository.findAllCommodities()).thenReturn(new ArrayList<>());
        
        ContainersDropDownDto result = service.getContainerTypesByVoyage(1L);
        assertNotNull(result);
    }

    @Test
    void getDefaultValues_Success() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            when(procRepository.callDefaultGetValue(1L, 1L, 1L, "DOC1")).thenReturn(new DefaultValueDto());
            DefaultValueDto result = service.getDefaultValues("DOC1");
            assertNotNull(result);
        }
    }

    @Test
    void saveEmails_Success() {
        SaveEmailsRequestDto req = new SaveEmailsRequestDto();
        req.setUpdateConsignee(true);
        req.setEmailsText("test1@test.com,test2@test.com");
        req.setScope("GLOBAL");
        
        String result = service.saveEmails(1L, req);
        assertEquals("Emails saved successfully", result);
        verify(procRepository, atLeastOnce()).saveEmailsToDb(any(), any(), any(), any(), any());
    }

    @Test
    void saveEmails_ValidationFail() {
        SaveEmailsRequestDto req = new SaveEmailsRequestDto();
        assertThrows(IllegalArgumentException.class, () -> service.saveEmails(1L, req));
        
        req.setUpdateConsignee(true);
        req.setEmailsText("");
        assertThrows(IllegalArgumentException.class, () -> service.saveEmails(1L, req));
    }

    @Test
    void printMethods_Success() throws Exception {
        when(printService.buildBaseParams(anyLong(), anyString())).thenReturn(new HashMap<>());
        when(printService.load(anyString())).thenReturn(mock(JasperReport.class));
        when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[0]);
        
        assertNotNull(service.printUnclearedCargoNotice(1L));
        assertNotNull(service.printProformaInvoice(1L, LocalDate.now(), 0L));
        
        when(validationRepository.getLineCode(1L)).thenReturn("MSC");
        assertNotNull(service.printCargoArrivalNotice(1L, 1L));
        
        assertNotNull(service.printCargoManifest(1L, true));
        assertNotNull(service.printCheckPortCharges(1L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_Success() {
        FilterRequestDto req = new FilterRequestDto("AND", "N", new ArrayList<>());
        Pageable pageable = mock(Pageable.class);
        RawSearchResult raw = new RawSearchResult(List.of(new HashMap<>()), new HashMap<>(), 1L);
        
        when(documentService.resolveOperator(req)).thenReturn("AND");
        when(documentService.resolveIsDeleted(req)).thenReturn("N");
        when(documentService.resolveFilters(req)).thenReturn(new ArrayList<>());
        when(documentService.search(any(), any(), any(), any(), any(), any(), any())).thenReturn(raw);
        
        Map<String, Object> result = service.list(req, pageable);
        assertNotNull(result);
    }

    @Test
    void errorScenarios_ResourceNotFound() {
        when(headerRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getImportManifest(1L));
        assertThrows(ResourceNotFoundException.class, () -> service.delete(1L, new DeleteReasonDto()));
        assertThrows(ResourceNotFoundException.class, () -> service.updateEmailVerification(1L, new EmailVerificationRequestDto()));
        assertThrows(ResourceNotFoundException.class, () -> service.resendCan(1L));
        
        when(headerRepository.findByTransactionPoid(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updateImportManifestBl(1L, new ImportManifestBlDto()));
    }

    @Test
    void list_Error() {
        when(documentService.resolveOperator(any())).thenThrow(new RuntimeException("Search failed"));
        assertThrows(RuntimeException.class, () -> service.list(new FilterRequestDto("AND", "N", new ArrayList<>()), mock(Pageable.class)));
    }
}
