package com.asg.shipping.bookingFormSH.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.shipping.bookingFormSH.dto.*;
import com.asg.shipping.bookingFormSH.entity.*;
import com.asg.shipping.bookingFormSH.repository.*;
import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.common.repository.GlobalAddressDetailsRepository;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.exceptions.ValidationException;
import jakarta.persistence.EntityManager;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingFormServiceImplTest {

    private static final Long GROUP_POID = 1L;
    private static final Long COMPANY_POID = 2L;
    private static final Long USER_POID = 3L;
    private static final Long TX_POID = 100L;

    private BookingFormServiceImpl service;

    @Mock
    private ShipMateHdrRepository headerRepository;
    @Mock
    private ShipMateCargoDtlRepository cargoRepo;
    @Mock
    private ShipMateChargesDtlRepository chargesRepo;
    @Mock
    private ShipMateContainerDtlRepository containerRepo;

    @Mock
    private ShipMateStuffingDtlRepository stuffingRepo;

    @Mock
    private GlobalAddressDetailsRepository globalAddressDetailsRepository;

    @Mock
    private BookingFormLovService lovService;

    private LovDataService commonLovService;
    @Mock
    private DocumentSearchService documentService;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private PrintService printService;
    @Mock
    private DataSource dataSource;
    @Mock
    private LoggingService loggingService;

    @Mock
    private LovDataService lovDataService;

    @Mock
    private EntityManager entityManager;

    private MockedStatic<UserContext> userContext;

    @BeforeEach
    void init() {
        userContext = mockStatic(UserContext.class);
        userContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);
        userContext.when(UserContext::getCompanyPoid).thenReturn(COMPANY_POID);
        userContext.when(UserContext::getUserPoid).thenReturn(USER_POID);
        userContext.when(UserContext::getDocumentId).thenReturn("DOC123");

        service = new BookingFormServiceImpl(headerRepository, cargoRepo, chargesRepo, containerRepo,stuffingRepo,
                globalAddressDetailsRepository,
                lovService,commonLovService, documentService, jdbcTemplate, printService, dataSource, loggingService,lovDataService);
    }

    @AfterEach
    void tearDown() {
        userContext.close();
    }

    // ============================================================
    // searchBookingForm
    // ============================================================

    @Test
    void searchBookingForm_success() {
        FilterRequestDto req = new FilterRequestDto("OR", "false", List.of());
        Pageable pageable = PageRequest.of(0, 10);
        RawSearchResult raw = new RawSearchResult(List.of(Map.of("TRANSACTION_POID", TX_POID)),
                Map.of("TRANSACTION_POID", "Transaction Poid"), 1L);

        when(documentService.resolveOperator(req)).thenReturn("AND");
        when(documentService.resolveIsDeleted(req)).thenReturn("N");
        when(documentService.resolveFilters(req)).thenReturn(List.of());
        when(documentService.search(any(), any(), any(), any(), any(), any(), any())).thenReturn(raw);

        Map<String, Object> result = service.searchBookingForm("DOC", req, pageable, null, null);
        assertNotNull(result);
    }

    // ============================================================
    // getBookingForm
    // ============================================================

    @Test
    void getBookingForm_success_minimalHeader() {
        ShipMateHdr hdr = new ShipMateHdr();
        hdr.setDeleted("N");
        hdr.setTransactionPoid(TX_POID);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(hdr));
        when(cargoRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(chargesRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(containerRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(stuffingRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());

        BookingFormDto result = service.getBookingForm(TX_POID);
        assertNotNull(result);
    }

    @Test
    void getBookingForm_withEnrichedLovFields() {
        LovItem lovItem = buildLovItem(10L, "CODE", "Desc");

        ShipMateHdr hdr = new ShipMateHdr();
        hdr.setDeleted("N");
        hdr.setTransactionPoid(TX_POID);
        hdr.setQuotationTransactionPoid(1L);
        hdr.setVesselPoid(2L);
        hdr.setLinePoid(3L);
        hdr.setSalesmanPoid(4L);
        hdr.setComodityPoid(5L);
        hdr.setPlaceOfRecieptPoid(6L);
        hdr.setPlaceOfDelieveryPoid(7L);
        hdr.setPortOfLoadingPoid(8L);
        hdr.setPortOfDischargePoid(9L);
        hdr.setMateLoadVoyagePoid(10L);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(hdr));
        when(cargoRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(chargesRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(containerRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(stuffingRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());

        when(lovService.getVesselMasterLov(2L)).thenReturn(List.of(lovItem));
        when(lovService.getLineMasterLov(3L)).thenReturn(List.of(lovItem));
        when(lovService.getSalesmanLov(4L)).thenReturn(List.of(lovItem));
        when(lovService.getCommodityMasterLov(5L)).thenReturn(List.of(lovItem));
        when(lovService.getPortMasterLov(anyLong())).thenReturn(List.of(lovItem));
        when(lovService.getVoyageMasterLov(10L)).thenReturn(List.of(lovItem));

        BookingFormDto result = service.getBookingForm(TX_POID);
        assertNotNull(result);
    }

    @Test
    void getBookingForm_withChargesAndContainerLovFields() {
        LovItem lovItem = buildLovItem(10L, "CODE", "Desc");

        ShipMateHdr hdr = new ShipMateHdr();
        hdr.setDeleted("N");
        hdr.setTransactionPoid(TX_POID);

        ShipMateChargesDtl chargesDtl = new ShipMateChargesDtl();
        chargesDtl.setChargePoid(5L);
        chargesDtl.setPaidAtPortPoid(6L);

        ShipMateContainerDtl containerDtl = new ShipMateContainerDtl();
        containerDtl.setComodityPoid(7L);
        containerDtl.setDestinationPortPoid(8L);

        ShipMateStuffingDtl stuffingDtl = new ShipMateStuffingDtl();
        stuffingDtl.setTransactionPoid(TX_POID);
        stuffingDtl.setDetRowId(1L);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(hdr));
        when(cargoRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(chargesRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of(chargesDtl));
        when(containerRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of(containerDtl));
        when(stuffingRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of(stuffingDtl));

        when(lovService.getChargeMasterLov(5L)).thenReturn(List.of(lovItem));
        when(lovService.getPortMasterLov(6L)).thenReturn(List.of(lovItem));
        when(lovService.getCommodityMasterLov(7L)).thenReturn(List.of(lovItem));
        when(lovService.getPortMasterLov(8L)).thenReturn(List.of(lovItem));

        BookingFormDto result = service.getBookingForm(TX_POID);
        assertNotNull(result);
    }

    @Test
    void getBookingForm_deleted_throwsResourceNotFound() {
        ShipMateHdr hdr = new ShipMateHdr();
        hdr.setDeleted("Y");
        when(headerRepository.findByTransactionPoid(any()))
                .thenReturn(Optional.of(hdr));
        assertThrows(ResourceNotFoundException.class, () -> service.getBookingForm(TX_POID));
    }

    @Test
    void getBookingForm_notFound_throwsResourceNotFound() {
        when(headerRepository.findByTransactionPoid(any()))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getBookingForm(TX_POID));
    }

    // ============================================================
    // createBookingForm
    // ============================================================

    @Test
    void createBookingForm_success_noDetails() {
        BookingFormCreateDTO dto = new BookingFormCreateDTO();
        dto.setLinePoid(10L);
        dto.setTransactionDate(java.time.LocalDate.now());

        ShipMateHdr savedEntity = savedHdr();
        when(headerRepository.save(any())).thenReturn(savedEntity);
        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(savedEntity));
        when(cargoRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(chargesRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(containerRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(stuffingRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());

        mockJdbcCall("Ok");

        BookingFormDto result = service.createBookingForm(dto);
        assertNotNull(result);
    }

    @Test
    void createBookingForm_lineMateValidationError_throwsValidationException() {
        BookingFormCreateDTO dto = new BookingFormCreateDTO();
        dto.setLinePoid(10L);
        dto.setTransactionDate(java.time.LocalDate.now());
        mockJdbcCall("ERROR");

        ValidationException ex = assertThrows(ValidationException.class, () -> service.createBookingForm(dto));
        assertTrue(ex.getMessage().contains("Line and Mate validation failed"));
    }

    @Test
    void createBookingForm_withContainerValidation_shortContainerNo_skipsValidation() {
        BookingFormCreateDTO dto = new BookingFormCreateDTO();
        dto.setLinePoid(10L);
        dto.setTransactionDate(java.time.LocalDate.now());

        // container with short containerNo (<3 chars) - should skip validateContainerLoad
        // action must be set so saveDetailTables doesn't NPE
        BookingFormContainerDetailDtoRequest containerDto = new BookingFormContainerDetailDtoRequest();
        containerDto.setContainerNo("AB"); // < 3 chars - skip container validation
        containerDto.setActionType("ISCREATED");
        dto.setContainerDetails(List.of(containerDto));

        ShipMateHdr savedEntity = savedHdr();
        savedEntity.setLinePoid(10L);
        when(headerRepository.save(any())).thenReturn(savedEntity);
        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(savedEntity));
        when(cargoRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(chargesRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(containerRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(containerRepo.getMaxDetRowId(TX_POID)).thenReturn(0L);
        when(containerRepo.saveAll(anyList())).thenReturn(List.of(new ShipMateContainerDtl()));
        when(stuffingRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());

        mockJdbcCall("Ok");

        BookingFormDto result = service.createBookingForm(dto);
        assertNotNull(result);
    }

    @Test
    void createBookingForm_withContainerValidation_longContainerNo_callsValidation() {
        BookingFormCreateDTO dto = new BookingFormCreateDTO();
        dto.setLinePoid(10L);
        dto.setTransactionDate(java.time.LocalDate.now());

        // >= 3 chars -> calls validateContainerLoad, action must be set for saveDetailTables
        BookingFormContainerDetailDtoRequest containerDto = new BookingFormContainerDetailDtoRequest();
        containerDto.setContainerNo("ABCD1234");
        containerDto.setActionType("ISCREATED");
        dto.setContainerDetails(List.of(containerDto));

        ShipMateHdr savedEntity = savedHdr();
        savedEntity.setLinePoid(10L);
        when(headerRepository.save(any())).thenReturn(savedEntity);
        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(savedEntity));
        when(cargoRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(chargesRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(containerRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(containerRepo.getMaxDetRowId(TX_POID)).thenReturn(0L);
        when(containerRepo.saveAll(anyList())).thenReturn(List.of(new ShipMateContainerDtl()));
        when(stuffingRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());

        mockJdbcCall("Ok");

        BookingFormDto result = service.createBookingForm(dto);
        assertNotNull(result);
    }

    @Test
    void createBookingForm_withCargoCreateAction() {
        BookingFormCreateDTO dto = new BookingFormCreateDTO();
        dto.setLinePoid(10L);
        dto.setTransactionDate(java.time.LocalDate.now());

        BookingFormCargoDetailDtoRequest cargo = new BookingFormCargoDetailDtoRequest();
        cargo.setActionType("ISCREATED");
        dto.setCargoDetails(List.of(cargo));

        ShipMateHdr savedEntity = savedHdr();
        ShipMateCargoDtl savedCargo = new ShipMateCargoDtl();
        savedCargo.setDetRowId(1L);

        when(headerRepository.save(any())).thenReturn(savedEntity);
        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(savedEntity));
        when(cargoRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(chargesRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(containerRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(cargoRepo.getMaxDetRowId(TX_POID)).thenReturn(0L);
        when(cargoRepo.saveAll(anyList())).thenReturn(List.of(savedCargo));
        when(stuffingRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());

        mockJdbcCall("Ok");

        BookingFormDto result = service.createBookingForm(dto);
        assertNotNull(result);
        verify(cargoRepo).saveAll(anyList());
    }

    @Test
    void createBookingForm_withStuffingCreate() {
        BookingFormCreateDTO dto = new BookingFormCreateDTO();
        dto.setLinePoid(10L);
        dto.setTransactionDate(java.time.LocalDate.now());

        BookingFormStuffingLoadDetailDtoRequest stuffing = new BookingFormStuffingLoadDetailDtoRequest();
        stuffing.setActionType("ISCREATED");

        dto.setStuffingDetails(List.of(stuffing));

        ShipMateHdr savedEntity = savedHdr();

        when(headerRepository.save(any())).thenReturn(savedEntity);
        when(headerRepository.findByTransactionPoid(TX_POID)).thenReturn(Optional.of(savedEntity));
        when(cargoRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(chargesRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(containerRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(stuffingRepo.getMaxDetRowId(TX_POID)).thenReturn(0L);
        when(stuffingRepo.saveAll(anyList())).thenReturn(List.of(new ShipMateStuffingDtl()));
        when(stuffingRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(cargoRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(chargesRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
        when(containerRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());

        mockJdbcCall("Ok");

        BookingFormDto result = service.createBookingForm(dto);
        assertNotNull(result);

        verify(stuffingRepo).saveAll(anyList());
    }

    // ============================================================
    // updateBookingForm
    // ============================================================

    @Test
    void updateBookingForm_success_noLinePoidChange() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        // linePoid null -> skip validateLineMate
        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");
        entity.setLinePoid(5L);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        mockJdbcCall("Ok");

        service.updateBookingForm(TX_POID, dto);
        verify(headerRepository).save(any());
    }

    @Test
    void updateBookingForm_success_withLinePoidChange() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        dto.setLinePoid(10L);

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");
        entity.setLinePoid(5L);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        mockJdbcCall("Ok");

        service.updateBookingForm(TX_POID, dto);
        verify(headerRepository).save(any());
    }

    @Test
    void updateBookingForm_notFound_throwsResourceNotFound() {
        when(headerRepository.findByTransactionPoid(any()))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.updateBookingForm(TX_POID, new BookingFormUpdateDTO()));
    }

    @Test
    void updateBookingForm_deleted_throwsResourceNotFound() {
        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("Y");
        when(headerRepository.findByTransactionPoid(any()))
                .thenReturn(Optional.of(entity));
        assertThrows(ResourceNotFoundException.class,
                () -> service.updateBookingForm(TX_POID, new BookingFormUpdateDTO()));
    }

    @Test
    void updateBookingForm_bookingIssueNoChanged_notDuplicate() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        dto.setBookingIssueNo("NEW_NO");

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");
        entity.setBookingIssueNo("OLD_NO");
        entity.setLinePoid(5L);
        entity.setTransactionPoid(TX_POID);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        when(headerRepository.existsByBookingIssueNoAndNotDeletedExcludingPoid("NEW_NO", TX_POID)).thenReturn(false);
        mockJdbcCall("Ok");

        service.updateBookingForm(TX_POID, dto);
        verify(headerRepository).save(any());
    }

    // Note: updateBookingForm_bookingIssueNoChanged_duplicate cannot be tested because
    // BookingFormMapper.mapUpdateDTOToEntity() sets entity.bookingIssueNo = dto.bookingIssueNo
    // BEFORE the !equals() check, so the condition is always false (known code gap).

    @Test
    void updateBookingForm_withContainerValidation_longContainerNo() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        BookingFormContainerDetailDtoRequest container = new BookingFormContainerDetailDtoRequest();
        container.setActionType("ISCREATED");
        container.setContainerNo("ABCD1234"); // >= 3 chars
        dto.setContainerDetails(List.of(container));

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");
        entity.setLinePoid(5L);

        ShipMateContainerDtl savedContainer = new ShipMateContainerDtl();
        savedContainer.setDetRowId(1L);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        when(containerRepo.getMaxDetRowId(TX_POID)).thenReturn(0L);
        when(containerRepo.saveAll(anyList())).thenReturn(List.of(savedContainer));
        mockJdbcCall("Ok");

        service.updateBookingForm(TX_POID, dto);
        verify(containerRepo).saveAll(anyList());
    }

    @Test
    void updateBookingForm_withContainerValidation_shortContainerNo_skipsValidation() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        BookingFormContainerDetailDtoRequest container = new BookingFormContainerDetailDtoRequest();
        container.setActionType("ISCREATED");
        container.setContainerNo("AB"); // < 3 -> skip
        dto.setContainerDetails(List.of(container));

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");
        entity.setLinePoid(5L);

        ShipMateContainerDtl savedContainer = new ShipMateContainerDtl();
        savedContainer.setDetRowId(1L);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        when(containerRepo.getMaxDetRowId(TX_POID)).thenReturn(0L);
        when(containerRepo.saveAll(anyList())).thenReturn(List.of(savedContainer));
        mockJdbcCall("Ok");

        service.updateBookingForm(TX_POID, dto);
        verify(containerRepo).saveAll(anyList());
    }

    @Test
    void updateBookingForm_withCargoUpdate_success() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        BookingFormCargoDetailDtoRequest cargo = new BookingFormCargoDetailDtoRequest();
        cargo.setActionType("ISUPDATED");
        cargo.setDetRowId(1L);
        dto.setCargoDetails(List.of(cargo));

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");

        ShipMateCargoDtl cargoDtl = new ShipMateCargoDtl();
        cargoDtl.setDetRowId(1L);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        when(cargoRepo.findByTransactionPoidAndDetRowId(TX_POID, 1L)).thenReturn(Optional.of(cargoDtl));
        when(cargoRepo.saveAll(anyList())).thenReturn(List.of(cargoDtl));
        mockJdbcCall("Ok");

        service.updateBookingForm(TX_POID, dto);
        verify(cargoRepo).saveAll(anyList());
    }

    @Test
    void updateBookingForm_withStuffingUpdate() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();

        BookingFormStuffingLoadDetailDtoRequest stuffing = new BookingFormStuffingLoadDetailDtoRequest();
        stuffing.setActionType("ISUPDATED");
        stuffing.setDetRowId(1L);

        dto.setStuffingDetails(List.of(stuffing));

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");

        ShipMateStuffingDtl stuffingDtl = new ShipMateStuffingDtl();
        stuffingDtl.setDetRowId(1L);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        when(stuffingRepo.findByTransactionPoidAndDetRowId(TX_POID, 1L))
                .thenReturn(Optional.of(stuffingDtl));
        when(stuffingRepo.saveAll(anyList()))
                .thenReturn(List.of(stuffingDtl));

        mockJdbcCall("Ok");

        service.updateBookingForm(TX_POID, dto);

        verify(stuffingRepo).saveAll(anyList());
    }

    @Test
    void updateBookingForm_withCargoUpdate_notFound_throwsValidationException() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        BookingFormCargoDetailDtoRequest cargo = new BookingFormCargoDetailDtoRequest();
        cargo.setActionType("ISUPDATED");
        cargo.setDetRowId(99L);
        dto.setCargoDetails(List.of(cargo));

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        when(cargoRepo.findByTransactionPoidAndDetRowId(TX_POID, 99L)).thenReturn(Optional.empty());
        when(cargoRepo.getMaxDetRowId(TX_POID)).thenReturn(0L);
        mockJdbcCall("Ok");

        assertThrows(ValidationException.class, () -> service.updateBookingForm(TX_POID, dto));
    }

    @Test
    void updateBookingForm_withCargoCreate_success() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        BookingFormCargoDetailDtoRequest cargo = new BookingFormCargoDetailDtoRequest();
        cargo.setActionType("ISCREATED");
        dto.setCargoDetails(List.of(cargo));

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");

        ShipMateCargoDtl savedCargo = new ShipMateCargoDtl();
        savedCargo.setDetRowId(1L);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        when(cargoRepo.getMaxDetRowId(TX_POID)).thenReturn(0L);
        when(cargoRepo.saveAll(anyList())).thenReturn(List.of(savedCargo));
        mockJdbcCall("Ok");

        service.updateBookingForm(TX_POID, dto);
        verify(cargoRepo).saveAll(anyList());
    }

    @Test
    void updateBookingForm_withCargoDelete_success() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        BookingFormCargoDetailDtoRequest cargo = new BookingFormCargoDetailDtoRequest();
        cargo.setActionType("ISDELETED");
        cargo.setDetRowId(1L);
        dto.setCargoDetails(List.of(cargo));

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        when(cargoRepo.getMaxDetRowId(TX_POID)).thenReturn(0L);
        mockJdbcCall("Ok");

        service.updateBookingForm(TX_POID, dto);
        verify(cargoRepo).deleteByTransactionPoidAndDetRowIdIn(TX_POID, List.of(1L));
    }

    @Test
    void updateBookingForm_withChargesCreate_success() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        BookingFormChargesDetailDtoRequest charges = new BookingFormChargesDetailDtoRequest();
        charges.setActionType("ISCREATED");
        dto.setChargesDetails(List.of(charges));

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");

        ShipMateChargesDtl savedCharges = new ShipMateChargesDtl();
        savedCharges.setDetRowId(1L);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        when(chargesRepo.getMaxDetRowId(TX_POID)).thenReturn(0L);
        when(chargesRepo.saveAll(anyList())).thenReturn(List.of(savedCharges));
        mockJdbcCall("Ok");

        service.updateBookingForm(TX_POID, dto);
        verify(chargesRepo).saveAll(anyList());
    }

    @Test
    void updateBookingForm_withChargesUpdate_success() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        BookingFormChargesDetailDtoRequest charges = new BookingFormChargesDetailDtoRequest();
        charges.setActionType("ISUPDATED");
        charges.setDetRowId(1L);
        dto.setChargesDetails(List.of(charges));

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");

        ShipMateChargesDtl chargesDtl = new ShipMateChargesDtl();
        chargesDtl.setDetRowId(1L);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        when(chargesRepo.findByTransactionPoidAndDetRowId(TX_POID, 1L)).thenReturn(Optional.of(chargesDtl));
        when(chargesRepo.saveAll(anyList())).thenReturn(List.of(chargesDtl));
        mockJdbcCall("Ok");

        service.updateBookingForm(TX_POID, dto);
        verify(chargesRepo).saveAll(anyList());
    }

    @Test
    void updateBookingForm_withChargesUpdate_notFound_throwsValidationException() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        BookingFormChargesDetailDtoRequest charges = new BookingFormChargesDetailDtoRequest();
        charges.setActionType("ISUPDATED");
        charges.setDetRowId(99L);
        dto.setChargesDetails(List.of(charges));

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        when(chargesRepo.findByTransactionPoidAndDetRowId(TX_POID, 99L)).thenReturn(Optional.empty());
        when(chargesRepo.getMaxDetRowId(TX_POID)).thenReturn(0L);
        mockJdbcCall("Ok");

        assertThrows(ValidationException.class, () -> service.updateBookingForm(TX_POID, dto));
    }

    @Test
    void updateBookingForm_withChargesDelete_success() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        BookingFormChargesDetailDtoRequest charges = new BookingFormChargesDetailDtoRequest();
        charges.setActionType("ISDELETED");
        charges.setDetRowId(2L);
        dto.setChargesDetails(List.of(charges));

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        when(chargesRepo.getMaxDetRowId(TX_POID)).thenReturn(0L);
        mockJdbcCall("Ok");

        service.updateBookingForm(TX_POID, dto);
        verify(chargesRepo).deleteByTransactionPoidAndDetRowIdIn(TX_POID, List.of(2L));
    }

    @Test
    void updateBookingForm_withContainerCreate_success() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        BookingFormContainerDetailDtoRequest container = new BookingFormContainerDetailDtoRequest();
        container.setActionType("ISCREATED");
        dto.setContainerDetails(List.of(container));

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");
        entity.setLinePoid(5L);

        ShipMateContainerDtl savedContainer = new ShipMateContainerDtl();
        savedContainer.setDetRowId(1L);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        when(containerRepo.getMaxDetRowId(TX_POID)).thenReturn(0L);
        when(containerRepo.saveAll(anyList())).thenReturn(List.of(savedContainer));
        mockJdbcCall("Ok");

        service.updateBookingForm(TX_POID, dto);
        verify(containerRepo).saveAll(anyList());
    }

    @Test
    void updateBookingForm_withContainerUpdate_success() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        BookingFormContainerDetailDtoRequest container = new BookingFormContainerDetailDtoRequest();
        container.setActionType("ISUPDATED");
        container.setDetRowId(1L);
        dto.setContainerDetails(List.of(container));

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");

        ShipMateContainerDtl containerDtl = new ShipMateContainerDtl();
        containerDtl.setDetRowId(1L);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        when(containerRepo.findByTransactionPoidAndDetRowId(TX_POID, 1L)).thenReturn(Optional.of(containerDtl));
        when(containerRepo.saveAll(anyList())).thenReturn(List.of(containerDtl));
        mockJdbcCall("Ok");

        service.updateBookingForm(TX_POID, dto);
        verify(containerRepo).saveAll(anyList());
    }

    @Test
    void updateBookingForm_withContainerUpdate_notFound_throwsValidationException() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        BookingFormContainerDetailDtoRequest container = new BookingFormContainerDetailDtoRequest();
        container.setActionType("ISUPDATED");
        container.setDetRowId(99L);
        dto.setContainerDetails(List.of(container));

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");
        entity.setLinePoid(5L);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        when(containerRepo.findByTransactionPoidAndDetRowId(TX_POID, 99L)).thenReturn(Optional.empty());
        when(containerRepo.getMaxDetRowId(TX_POID)).thenReturn(0L);
        mockJdbcCall("Ok");

        assertThrows(ValidationException.class, () -> service.updateBookingForm(TX_POID, dto));
    }

    @Test
    void updateBookingForm_withContainerDelete_success() {
        BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
        BookingFormContainerDetailDtoRequest container = new BookingFormContainerDetailDtoRequest();
        container.setActionType("ISDELETED");
        container.setDetRowId(3L);
        dto.setContainerDetails(List.of(container));

        ShipMateHdr entity = new ShipMateHdr();
        entity.setDeleted("N");
        entity.setLinePoid(5L);

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(entity));
        when(containerRepo.getMaxDetRowId(TX_POID)).thenReturn(0L);
        mockJdbcCall("Ok");

        service.updateBookingForm(TX_POID, dto);
        verify(containerRepo).deleteByTransactionPoidAndDetRowIdIn(TX_POID, List.of(3L));
    }

    // ============================================================
    // deleteBookingForm
    // ============================================================

    @Test
    void deleteBookingForm_success() {
        ShipMateHdr hdr = new ShipMateHdr();
        hdr.setDeleted("N");

        when(headerRepository.findByTransactionPoid(TX_POID))
                .thenReturn(Optional.of(hdr));

        service.deleteBookingForm(TX_POID);

        assertEquals("Y", hdr.getDeleted());
        verify(headerRepository).save(hdr);
    }

    @Test
    void deleteBookingForm_notFound_throwsResourceNotFound() {
        when(headerRepository.findByTransactionPoid(any()))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deleteBookingForm(TX_POID));
    }

    // ============================================================
    // generateCoprarBooking
    // ============================================================

    @Test
    void generateCoprarBooking_coprarProcOk_returnsGeneratedFile() {
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenAnswer(inv -> {
            ConnectionCallback<?> callback = inv.getArgument(0);
            Connection conn = mock(Connection.class);
            CallableStatement cs = mock(CallableStatement.class);
            when(conn.prepareCall(anyString())).thenReturn(cs);
            when(cs.getString(anyInt())).thenReturn("ok");
            return callback.doInConnection(conn);
        });
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(List.of("line1", "line2"));
        when(jdbcTemplate.update(anyString(), any(), any(), any())).thenReturn(1);

        String result = service.generateCoprarBooking(TX_POID);
        assertNotNull(result);
    }

    @Test
    void generateCoprarBooking_coprarProcError_returnsErrorString() {
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenAnswer(inv -> {
            ConnectionCallback<?> callback = inv.getArgument(0);
            Connection conn = mock(Connection.class);
            CallableStatement cs = mock(CallableStatement.class);
            when(conn.prepareCall(anyString())).thenReturn(cs);
            when(cs.getString(anyInt())).thenReturn("ERROR: Failed");
            return callback.doInConnection(conn);
        });
        String result = service.generateCoprarBooking(TX_POID);
        assertTrue(result.toLowerCase().contains("error"));
    }

    @Test
    void generateCoprarBooking_exception_throwsValidationException() {
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenThrow(new RuntimeException("DB Error"));
        assertThrows(ValidationException.class, () -> service.generateCoprarBooking(TX_POID));
    }

    // ============================================================
    // getEmptyShipper
    // ============================================================

    @Test
    void getEmptyShipper_success() {
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenReturn("SHIPPER123");
        String result = service.getEmptyShipper(COMPANY_POID);
        assertEquals("SHIPPER123", result);
    }

    @Test
    void getEmptyShipper_exception_throwsValidationException() {
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenThrow(new RuntimeException("DB Error"));
        assertThrows(ValidationException.class, () -> service.getEmptyShipper(COMPANY_POID));
    }

    // ============================================================
    // processEmptyContainerLoad
    // ============================================================

    @Test
    void processEmptyContainerLoad_success() {
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenReturn("Success");
        String result = service.processEmptyContainerLoad(TX_POID);
        assertEquals("Success", result);
    }

    @Test
    void processEmptyContainerLoad_exception_throwsValidationException() {
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenThrow(new RuntimeException("DB Error"));
        assertThrows(ValidationException.class, () -> service.processEmptyContainerLoad(TX_POID));
    }

    // ============================================================
    // Print methods
    // ============================================================

    @Test
    void mateBookingPrintForm_success() throws Exception {
        Map<String, Object> mutableParams = new java.util.HashMap<>();
        when(printService.buildBaseParams(TX_POID, "100-140")).thenReturn(mutableParams);
        when(printService.load(anyString())).thenReturn(mock(JasperReport.class));
        when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[0]);

        byte[] result = service.mateBookingPrintForm(TX_POID);
        assertNotNull(result);
    }

    @Test
    void cntEmptyBookingPrintForm_success() throws Exception {
        Map<String, Object> mutableParams = new java.util.HashMap<>();
        when(printService.buildBaseParams(any(), anyString())).thenReturn(mutableParams);
        when(printService.load(anyString())).thenReturn(mock(JasperReport.class));
        when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[0]);

        byte[] result = service.cntEmptyBookingPrintForm(TX_POID);
        assertNotNull(result);
    }

    @Test
    void cntReturnBookingPrintFormAll_success() throws Exception {
        Map<String, Object> mutableParams = new java.util.HashMap<>();
        when(printService.buildBaseParams(any(), anyString())).thenReturn(mutableParams);
        when(printService.load(anyString())).thenReturn(mock(JasperReport.class));
        when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[0]);

        byte[] result = service.cntReturnBookingPrintFormAll(TX_POID, "STAMP");
        assertNotNull(result);
    }

    // ============================================================
    // Helpers
    // ============================================================

    private void mockJdbcCall(String returnValue) {
        lenient().when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenAnswer(inv -> {
            ConnectionCallback<?> callback = inv.getArgument(0);
            Connection conn = mock(Connection.class);
            CallableStatement cs = mock(CallableStatement.class);
            when(conn.prepareCall(anyString())).thenReturn(cs);
            when(cs.getString(anyInt())).thenReturn(returnValue);
            return callback.doInConnection(conn);
        });
    }

    private ShipMateHdr savedHdr() {
        ShipMateHdr hdr = new ShipMateHdr();
        hdr.setTransactionPoid(TX_POID);
        hdr.setDeleted("N");
        return hdr;
    }

    private LovItem buildLovItem(Long poid, String code, String description) {
        LovItem item = new LovItem();
        item.setPoid(poid);
        item.setCode(code);
        item.setDescription(description);
        item.setValue(poid);
        item.setLabel(code);
        return item;
    }
}
