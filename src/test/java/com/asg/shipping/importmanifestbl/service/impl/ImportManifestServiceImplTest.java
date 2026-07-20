package com.asg.shipping.importmanifestbl.service.impl;

import com.asg.shipping.address.entity.AddressDetails;
import com.asg.shipping.importmanifestupdate.dto.LoadEmailFaxRequestDto;
import com.asg.shipping.importmanifestupdate.event.BlManifestSaveEvent;
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
import com.asg.shipping.address.entity.AddressDetailsRepository;
import com.asg.shipping.shippingffchargemaster.entity.ShipChargeMaster;
import com.asg.shipping.shippingffchargemaster.repository.ShipChargeMasterRepository;
import jakarta.persistence.EntityManager;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.data.domain.Pageable;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.importmanifestupdate.constants.BlManifestValidationMessages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.math.BigDecimal;
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

    // Deliberately a real instance, not a mock: mocking it stubs out every legacy
    // save-time rule (mandatory fields, hold remarks, CAN addresses, container
    // fields, negative gain, freight status, demurrage code) and the tests below
    // would then assert nothing about them.
    @Spy private BlManifestValidationService blManifestValidationService = new BlManifestValidationService();

    @Mock private ShipChargeMasterRepository chargeMasterRepository;
    @Mock private com.asg.shipping.importmanifestupdate.service.ImportManifestBlServiceImpl updateService;
    @Mock private AddressDetailsRepository addressDetailsRepository;
    @Mock private EntityManager entityManager;
    
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

    /**
     * A DTO that satisfies every save-time rule. Hold reason is left null, which the
     * legacy bean treats as "5" and skips the CAN consignee/notify/address block.
     */
    private ImportManifestBlDto.ImportManifestBlDtoBuilder validDto() {
        return ImportManifestBlDto.builder()
                .vesselVoyagePoid(100L)
                .blNumber("BL123")
                .cargo("FCL")
                .blType("IMPORT")
                .loadPortPoid(1L)
                .transactionDate(LocalDate.now());
    }

    /**
     * A DTO with a hold reason that forces the CAN block to run: consignee, notify
     * and at least one send-flagged address are then all mandatory.
     */
    private ImportManifestBlDto.ImportManifestBlDtoBuilder canActiveDto() {
        return validDto()
                .holdReason("1")
                .holdRemarks("On hold pending payment")
                .consigneePoid(200L)
                .notify1Poid(201L)
                .addressDetails(List.of(
                        AddressDetailsDto.builder().addressType("CONSIGNEE").sendYesNo("Y").build()));
    }

    private ContainerDto validContainer(Long detRowId, String action) {
        return ContainerDto.builder()
                .detRowId(detRowId)
                .actionType(action)
                .containerNumber("CONT" + detRowId)
                .equipmentIsoType("20GP")
                .build();
    }

    /** Lets validation reach the rules that run after {@code validateBlManifestDTO}. */
    private void allowVoyageAndFinancialYear() {
        lenient().when(validationRepository.getVoyageCompanyPoid(anyLong())).thenReturn(1L);
        lenient().when(validationRepository.isValidFinancialYear(anyLong(), any())).thenReturn(true);
    }

    private void stubHeaderSave() {
        when(headerRepository.saveAndFlush(any())).thenAnswer(inv -> {
            ShipBlManifestHdr h = inv.getArgument(0);
            h.setTransactionPoid(1L);
            return h;
        });
    }

    private void assertValidationMessage(String expectedMessage, Executable call) {
        ValidationException ex = assertThrows(ValidationException.class, call);
        assertTrue(ex.getMessage().contains(expectedMessage),
                "Expected message containing:\n  " + expectedMessage + "\nbut was:\n  " + ex.getMessage());
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
                    .equipmentIsoType("20GP")
                    .build();

            // Rows marked for deletion carry no container number / ISO type. They must be
            // skipped by validateContainerFields, exactly as the legacy bean never saw them
            // (ADF removed deleted rows from the view object before DocumentBeforeSave ran).
            ContainerDto containerDelete = ContainerDto.builder()
                    .detRowId(2L)
                    .actionType("ISDELETED")
                    .build();

            ImportManifestBlDto dto = validDto()
                    .transactionPoid(1L)
                    .containers(List.of(containerUpdate, containerDelete))
                    .build();

            when(headerRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(header));
            when(headerRepository.saveAndFlush(any())).thenReturn(header);
            allowVoyageAndFinancialYear();

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
            ContainerDto containerUpdate = validContainer(1L, "ISUPDATED");

            ImportManifestBlDto dto = validDto()
                    .transactionPoid(1L)
                    .containers(List.of(containerUpdate))
                    .build();

            when(headerRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(header));
            when(headerRepository.saveAndFlush(any())).thenReturn(header);
            allowVoyageAndFinancialYear();

            ShipBlManifestContainerDtl existingContainer = new ShipBlManifestContainerDtl();
            existingContainer.setId(new ShipBlManifestDtlId(1L, 1L));
            existingContainer.setContainerNo("CONT1");
            when(containerDtlRepository.findById(any())).thenReturn(Optional.of(existingContainer));

            ShipBlManifestContainerDtl conflict = new ShipBlManifestContainerDtl();
            conflict.setId(new ShipBlManifestDtlId(1L, 2L)); // Different detRowId
            conflict.setContainerNo("CONT1");
            when(containerDtlRepository.findByIdTransactionPoidAndContainerNo(eq(1L), eq("CONT1")))
                    .thenReturn(Optional.of(conflict));

            assertValidationMessage("Duplicate container number",
                    () -> service.updateImportManifestBl(1L, dto));
        }
    }

    @Test
    void createImportManifestBl_AllDetails_Success() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);

            OtherNotifyDto otherNotifies = new OtherNotifyDto();
            otherNotifies.setNotify2Poid(202L);
            otherNotifies.setNotify3Poid(203L);

            ImportManifestBlDto dto = validDto()
                    .consigneePoid(200L)
                    .notify1Poid(201L)
                    .otherNotifies(otherNotifies)
                    .generalCargoDetails(List.of(GeneralCargoDto.builder().detRowId(1L).build()))
                    .descriptionsAndMarks(List.of(DescriptionAndMarksDto.builder().detRowId(1L).build()))
                    .containers(List.of(validContainer(1L, null)))
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

            stubHeaderSave();
            allowVoyageAndFinancialYear();

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
        when(procRepository.resendCan(100L, 1L, null)).thenReturn(new ResendCanResponseDto());
        
        ResendCanResponseDto result = service.resendCan(1L, null);
        assertNotNull(result);
    }

    @Test
    void updateImportManifestBl_ValidationFail_Voyage() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);

            ShipBlManifestHdr header = createHeader();
            when(headerRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(header));
            // Voyage belongs to company 2, user is in company 1.
            lenient().when(validationRepository.getVoyageCompanyPoid(anyLong())).thenReturn(2L);

            assertValidationMessage(BlManifestValidationMessages.VOYAGE_COMPANY_MISMATCH,
                    () -> service.updateImportManifestBl(1L, validDto().build()));
        }
    }

    @Test
    void createImportManifestBl_Defaults_Success() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);

            // blType and cargo must be supplied: validateMandatoryFields rejects them as
            // null before autoPopulateDefaults ever runs. The remaining defaults are the
            // ones autoPopulateDefaults can actually reach.
            ImportManifestBlDto dto = validDto().cargo("FCL-FCL").build();

            stubHeaderSave();
            allowVoyageAndFinancialYear();

            ImportManifestBlResponseDto result = service.createImportManifestBl(dto, 1L, 1L);

            assertNotNull(result);
            verify(headerRepository).saveAndFlush(argThat(h ->
                    "IMPORT".equals(h.getBlType()) &&
                    "FCL-FCL".equals(h.getCargoType()) &&
                    "1".equals(h.getBlIssueType()) &&
                    "5".equals(h.getHoldReason()) &&
                    "1".equals(h.getFreightStatus()) &&
                    "Y".equals(h.getBookedByPp()) &&
                    Long.valueOf(51L).equals(h.getSalesmanPoid()) &&
                    Long.valueOf(10L).equals(h.getComodityPoid()) &&
                    Long.valueOf(800L).equals(h.getPortOfDischargePoid()) &&
                    Long.valueOf(800L).equals(h.getPlaceOfDeliveryPoid())
            ));
        }
    }

    @Test
    void createImportManifestBl_MandatoryFieldsMissing_Fail() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);

            assertValidationMessage(BlManifestValidationMessages.VOYAGE_REQUIRED,
                    () -> service.createImportManifestBl(validDto().vesselVoyagePoid(null).build(), 1L, 1L));
            assertValidationMessage(BlManifestValidationMessages.CARGO_TYPE_REQUIRED,
                    () -> service.createImportManifestBl(validDto().cargo(null).build(), 1L, 1L));
            assertValidationMessage(BlManifestValidationMessages.BL_NUMBER_REQUIRED,
                    () -> service.createImportManifestBl(validDto().blNumber(null).build(), 1L, 1L));
            assertValidationMessage(BlManifestValidationMessages.BL_TYPE_REQUIRED,
                    () -> service.createImportManifestBl(validDto().blType(null).build(), 1L, 1L));
        }
    }

    @Test
    void createImportManifestBl_PortOfLoadingMissing_Fail() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            // Without this the voyage-company rule fires first and the assertion below
            // would pass for the wrong reason.
            allowVoyageAndFinancialYear();

            assertValidationMessage(BlManifestValidationMessages.PORT_OF_LOADING_REQUIRED,
                    () -> service.createImportManifestBl(validDto().loadPortPoid(null).build(), 1L, 1L));
        }
    }

    @Test
    void createImportManifestBl_DuplicateBlNumberForVoyage_Fail() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            allowVoyageAndFinancialYear();
            when(headerRepository.existsByVoyageTransactionPoidAndBlNumber(100L, "BL123")).thenReturn(true);

            assertValidationMessage("already exists for this voyage",
                    () -> service.createImportManifestBl(validDto().build(), 1L, 1L));
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
        LoadEmailFaxRequestDto req = new LoadEmailFaxRequestDto();
        req.setAddressMasterPoid(100L);
        req.setAddressType("CAN");
        
        AddressDetails address = new AddressDetails();
        address.setAddressPoid("1");
        address.setEmail("test@test.com");
        
        when(addressDetailsRepository.findByAddressMasterPoidAndAddressType(1L, "CAN")).thenReturn(List.of(address));
        
        LoadEmailFaxResponseDto result = service.loadEmailFax(1L, req.toString());
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

        Connection mockConn = mock(Connection.class);
        Statement mockStmt = mock(Statement.class);
        java.sql.PreparedStatement mockPs = mock(java.sql.PreparedStatement.class);
        java.sql.ResultSet mockRs = mock(java.sql.ResultSet.class);
        when(dataSource.getConnection()).thenReturn(mockConn);
        when(mockConn.createStatement()).thenReturn(mockStmt);
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
        when(mockPs.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(false);

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
        when(documentService.resolveDateFilters(any(), any(), any(), any())).thenReturn(new ArrayList<>());
        when(documentService.search(any(), any(), any(), any(), any(), any(), any())).thenReturn(raw);
        
        Map<String, Object> result = service.list(req, null, null, pageable);
        assertNotNull(result);
    }

    @Test
    void errorScenarios_ResourceNotFound() {
        when(headerRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getImportManifest(1L));
        assertThrows(ResourceNotFoundException.class, () -> service.delete(1L, new DeleteReasonDto()));
        assertThrows(ResourceNotFoundException.class, () -> service.updateEmailVerification(1L, new EmailVerificationRequestDto()));
        assertThrows(ResourceNotFoundException.class, () -> service.resendCan(1L, null));
        
        when(headerRepository.findByTransactionPoid(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updateImportManifestBl(1L, new ImportManifestBlDto()));
    }

    @Test
    void list_Error() {
        when(documentService.resolveOperator(any())).thenThrow(new RuntimeException("Search failed"));
        assertThrows(RuntimeException.class, () -> service.list(new FilterRequestDto("AND", "N", new ArrayList<>()), null, null, mock(Pageable.class)));
    }

    // ---------------------------------------------------------------------
    // Legacy save-time rules (blmanifestpagebn.DocumentBeforeSave)
    // ---------------------------------------------------------------------

    @Test
    void createImportManifestBl_HoldReasonWithoutRemarks_Fail() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            allowVoyageAndFinancialYear();

            // Hold reasons 1, 2 and 3 all demand remarks.
            for (String holdReason : List.of("1", "2", "3")) {
                assertValidationMessage(BlManifestValidationMessages.HOLD_REMARKS_REQUIRED,
                        () -> service.createImportManifestBl(
                                validDto().holdReason(holdReason).holdRemarks("  ").build(), 1L, 1L));
            }
        }
    }

    @Test
    void createImportManifestBl_HoldCanAutoWithoutRemarks_Fail() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            allowVoyageAndFinancialYear();

            assertValidationMessage(BlManifestValidationMessages.HOLD_CAN_DO_REMARKS_REQUIRED,
                    () -> service.createImportManifestBl(validDto().holdCanAuto("Y").build(), 1L, 1L));
        }
    }

    @Test
    void createImportManifestBl_CanConsigneeAndNotify_Fail() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            allowVoyageAndFinancialYear();

            assertValidationMessage(BlManifestValidationMessages.CONSIGNEE_REQUIRED,
                    () -> service.createImportManifestBl(canActiveDto().consigneePoid(null).build(), 1L, 1L));
            // POID 1 is the legacy "unset" sentinel, not a real address.
            assertValidationMessage(BlManifestValidationMessages.CONSIGNEE_REQUIRED,
                    () -> service.createImportManifestBl(canActiveDto().consigneePoid(1L).build(), 1L, 1L));
            assertValidationMessage(BlManifestValidationMessages.NOTIFY_PARTY_REQUIRED,
                    () -> service.createImportManifestBl(canActiveDto().notify1Poid(null).build(), 1L, 1L));
            assertValidationMessage(BlManifestValidationMessages.NOTIFY_PARTY_REQUIRED,
                    () -> service.createImportManifestBl(canActiveDto().notify1Poid(1L).build(), 1L, 1L));
        }
    }

    @Test
    void createImportManifestBl_NoCanAddressAndNoManualTick_Fail() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            allowVoyageAndFinancialYear();

            // No address flagged with sendYesNo=Y, and manual CAN send left off.
            List<AddressDetailsDto> notSending =
                    List.of(AddressDetailsDto.builder().addressType("CONSIGNEE").sendYesNo("N").build());

            assertValidationMessage(BlManifestValidationMessages.NO_CAN_ADDRESS,
                    () -> service.createImportManifestBl(
                            canActiveDto().addressDetails(notSending).build(), 1L, 1L));
            assertValidationMessage(BlManifestValidationMessages.NO_CAN_ADDRESS,
                    () -> service.createImportManifestBl(
                            canActiveDto().addressDetails(notSending).manualCanSend("N").build(), 1L, 1L));
        }
    }

    @Test
    void createImportManifestBl_NoCanAddressButManualTick_Success() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            allowVoyageAndFinancialYear();
            stubHeaderSave();
            when(emailFaxDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);

            ImportManifestBlDto dto = canActiveDto()
                    .addressDetails(List.of(
                            AddressDetailsDto.builder().addressType("CONSIGNEE").sendYesNo("N").build()))
                    .manualCanSend("Y")
                    .build();

            assertNotNull(service.createImportManifestBl(dto, 1L, 1L));
        }
    }

    @Test
    void createImportManifestBl_HoldReason5_SkipsCanChecks() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            allowVoyageAndFinancialYear();
            stubHeaderSave();

            // Reason "5" means "no CAN": consignee, notify and addresses are all irrelevant.
            ImportManifestBlDto dto = validDto().holdReason("5").build();

            assertNotNull(service.createImportManifestBl(dto, 1L, 1L));
        }
    }

    @Test
    void createImportManifestBl_ContainerMissingNumberOrIsoType_Fail() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            allowVoyageAndFinancialYear();

            ContainerDto noIsoType = ContainerDto.builder().detRowId(1L).containerNumber("CONT1").build();
            ContainerDto noNumber = ContainerDto.builder().detRowId(1L).equipmentIsoType("20GP").build();

            assertValidationMessage(BlManifestValidationMessages.CONTAINER_FIELDS_REQUIRED,
                    () -> service.createImportManifestBl(validDto().containers(List.of(noIsoType)).build(), 1L, 1L));
            assertValidationMessage(BlManifestValidationMessages.CONTAINER_FIELDS_REQUIRED,
                    () -> service.createImportManifestBl(validDto().containers(List.of(noNumber)).build(), 1L, 1L));
        }
    }

    @Test
    void updateImportManifestBl_DeletedRowsSkipValidation() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            allowVoyageAndFinancialYear();
            when(headerRepository.findByTransactionPoid(1L)).thenReturn(Optional.of(createHeader()));
            when(headerRepository.saveAndFlush(any())).thenReturn(createHeader());
            when(containerDtlRepository.findAllById(any())).thenReturn(List.of(new ShipBlManifestContainerDtl()));
            when(chargesDtlRepository.findAllById(any())).thenReturn(List.of(new ShipBlManifestChargesDtl()));

            // A deleted container with no number/ISO type, and a deleted demurrage charge
            // (code 94) that would otherwise be rejected outright. Neither is being saved,
            // so neither may be validated.
            ContainerDto deletedContainer = ContainerDto.builder()
                    .detRowId(1L).actionType("ISDELETED").build();
            ChargeDto deletedDemurrage = ChargeDto.builder()
                    .detRowId(1L).actionType("ISDELETED").chargePoid(94L).build();

            ImportManifestBlDto dto = validDto()
                    .transactionPoid(1L)
                    .containers(List.of(deletedContainer))
                    .charges(List.of(deletedDemurrage))
                    .build();

            assertNotNull(service.updateImportManifestBl(1L, dto));
            verify(containerDtlRepository).deleteAllInBatch(any());
            verify(chargesDtlRepository).deleteAllInBatch(any());
        }
    }

    @Test
    void createImportManifestBl_NegativeGain_Fail() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            allowVoyageAndFinancialYear();

            // Buy 100 x 2 exceeds sell 30 x 2 -> gain of -140.
            ChargeDto loss = ChargeDto.builder()
                    .chargePoid(10L)
                    .quantity(new BigDecimal("2"))
                    .sell(new BigDecimal("30"))
                    .buy(new BigDecimal("100"))
                    .build();

            assertValidationMessage("Total financial gain cannot be negative",
                    () -> service.createImportManifestBl(validDto().charges(List.of(loss)).build(), 1L, 1L));
        }
    }

    @Test
    void createImportManifestBl_DemurrageCode94_Fail() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            allowVoyageAndFinancialYear();

            ChargeDto demurrage = ChargeDto.builder().chargePoid(94L).build();

            assertValidationMessage(BlManifestValidationMessages.DEMURRAGE_CODE_94_NOT_ALLOWED,
                    () -> service.createImportManifestBl(validDto().charges(List.of(demurrage)).build(), 1L, 1L));
        }
    }

    @Test
    void createImportManifestBl_FreightTypeNotEntered_Fail() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            allowVoyageAndFinancialYear();

            // Freight status set, hold reason is not "5", and no charge carries one of the
            // freight-bearing charge codes (65 / 748 / 928) -> global freight type stays "XX".
            ImportManifestBlDto dto = canActiveDto()
                    .freight("1")
                    .charges(List.of(ChargeDto.builder().chargePoid(10L).freightType("P").build()))
                    .build();

            assertValidationMessage(BlManifestValidationMessages.FREIGHT_TYPE_NOT_ENTERED,
                    () -> service.createImportManifestBl(dto, 1L, 1L));
        }
    }

    @Test
    void createImportManifestBl_FreightStatusMismatch_Fail() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            allowVoyageAndFinancialYear();

            // Charge code 65 carries freight type "C" (collect) while the BL says prepaid.
            ChargeDto collect = ChargeDto.builder().chargePoid(65L).freightType("C").build();

            assertValidationMessage(BlManifestValidationMessages.FREIGHT_STATUS_MISMATCH_PREPAID,
                    () -> service.createImportManifestBl(
                            canActiveDto().freight("1").charges(List.of(collect)).build(), 1L, 1L));

            ChargeDto prepaid = ChargeDto.builder().chargePoid(748L).freightType("P").build();

            assertValidationMessage(BlManifestValidationMessages.FREIGHT_STATUS_MISMATCH_COLLECT,
                    () -> service.createImportManifestBl(
                            canActiveDto().freight("2").charges(List.of(prepaid)).build(), 1L, 1L));

            assertValidationMessage(BlManifestValidationMessages.FREIGHT_STATUS_MISMATCH_ELSEWHERE,
                    () -> service.createImportManifestBl(
                            canActiveDto().freight("3").charges(List.of(prepaid)).build(), 1L, 1L));
        }
    }

    @Test
    void createImportManifestBl_FreightStatusMatches_Success() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            allowVoyageAndFinancialYear();
            stubHeaderSave();
            when(chargesDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);
            when(emailFaxDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);
            when(chargeMasterRepository.findByChargePoid(928L)).thenReturn(Optional.empty());

            ChargeDto prepaid = ChargeDto.builder().chargePoid(928L).freightType("P").build();

            assertNotNull(service.createImportManifestBl(
                    canActiveDto().freight("1").charges(List.of(prepaid)).build(), 1L, 1L));
        }
    }

    @Test
    void createImportManifestBl_FreightTypeResolvedFromOtherCharges_Success() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            allowVoyageAndFinancialYear();
            stubHeaderSave();
            when(emailFaxDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);

            // The legacy bean resolves the global freight type from the "others" charge view
            // first, falling back to the main charge view. A prepaid freight type living only
            // on an other-charge row must satisfy freight status "1".
            ChargeOtherDto prepaidOther = ChargeOtherDto.builder().chargePoid(928L).freightType("P").build();

            ImportManifestBlDto dto = canActiveDto()
                    .freight("1")
                    .otherCharges(List.of(prepaidOther))
                    .build();

            assertNotNull(service.createImportManifestBl(dto, 1L, 1L));
        }
    }

    @Test
    void createImportManifestBl_NoFreightStatus_SkipsFreightChecks() {
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mockUserContext(mocked);
            allowVoyageAndFinancialYear();
            stubHeaderSave();
            when(emailFaxDtlRepository.getMaxDetRowId(1L)).thenReturn(0L);

            // freight == null short-circuits validateFreightType even though the charge list
            // carries no freight-bearing charge code.
            assertNotNull(service.createImportManifestBl(canActiveDto().freight(null).build(), 1L, 1L));
        }
    }
}
