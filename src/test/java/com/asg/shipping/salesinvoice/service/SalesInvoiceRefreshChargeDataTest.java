package com.asg.shipping.salesinvoice.service;

import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.shipping.salesinvoice.dto.LoadChargeDataResponseDTO;
import com.asg.shipping.salesinvoice.dto.RefreshChargeDataRequestDTO;
import com.asg.shipping.salesinvoice.dto.SalesInvoiceChargesDtlDto;
import com.asg.shipping.salesinvoice.dto.SalesInvoiceContainerDtlDto;
import com.asg.shipping.salesinvoice.entity.ArShSalesInvoiceHdr;
import com.asg.shipping.salesinvoice.repository.ArShSalesInvoiceChargDtlRepository;
import com.asg.shipping.salesinvoice.repository.ArShSalesInvoiceContnrDtlRepository;
import com.asg.shipping.salesinvoice.repository.ArShSalesInvoiceHdrRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the behaviour that separates refreshChargeData from loadChargeData: the recalculation is
 * driven by the grids the UI sends rather than by a fresh database read.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesInvoiceRefreshChargeDataTest {

    private static final Long INVOICE_POID = 12345L;
    private static final Long BL_POID = 456L;
    private static final Long DEMURRAGE_CHARGE_POID = 7001L;
    private static final Long LATE_CHARGE_POID = 8001L;
    private static final Long MANIFEST_CHARGE_POID = 9001L;

    @Mock private ArShSalesInvoiceHdrRepository hdrRepository;
    @Mock private ArShSalesInvoiceContnrDtlRepository contnrDtlRepository;
    @Mock private ArShSalesInvoiceChargDtlRepository chargDtlRepository;
    @Mock private DocumentSearchService documentService;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private DataSource dataSource;
    @Mock private PrintService printService;
    @Mock private DocumentDeleteService documentDeleteService;
    @Mock private LoggingService loggingService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private SalesInvoiceShippingServiceImpl service;

    @BeforeEach
    void setUp() {
        ArShSalesInvoiceHdr invoice = new ArShSalesInvoiceHdr();
        invoice.setTransactionPoid(INVOICE_POID);
        invoice.setCompanyPoid(1L);
        invoice.setGroupPoid(1L);
        when(hdrRepository.findActiveByTransactionPoid(INVOICE_POID)).thenReturn(Optional.of(invoice));

        // BL type resolved from the manifest
        when(jdbcTemplate.queryForObject(contains("SELECT BL_TYPE FROM SHIP_BL_MANIFEST_HDR"),
                eq(String.class), any(Object[].class))).thenReturn("IMPORT");

        // Container size lookups
        when(jdbcTemplate.queryForObject(contains("GET_CONTAINER_TYPE"), eq(String.class), eq("22G1")))
                .thenReturn("20");
        when(jdbcTemplate.queryForObject(contains("GET_CONTAINER_TYPE"), eq(String.class), eq("42G1")))
                .thenReturn("40");

        // Default: no manifest charges, no demurrage code, no late charges. Individual tests override.
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(new ArrayList<>());
    }

    private SalesInvoiceContainerDtlDto container(String containerNo, String isoType, String amount) {
        return SalesInvoiceContainerDtlDto.builder()
                .blPoid(BL_POID)
                .containerNo(containerNo)
                .equipmentIsoType(isoType)
                .dmChargeAmt(new BigDecimal(amount))
                .build();
    }

    private void stubDemurrageChargeLookup(BigDecimal amount) {
        when(jdbcTemplate.query(contains("FROM GLOBAL_PARAMETERS WHERE PARAMETER_KEYID_TYPE"),
                any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(SalesInvoiceChargesDtlDto.builder()
                        .blPoid(BL_POID)
                        .chargePoid(DEMURRAGE_CHARGE_POID)
                        .chargesDetRowId(0L)
                        .amount(amount)
                        .amountSelect("Y")
                        .chargeType("LOCAL")
                        .chargeNewRecord("Y")
                        .demurrageCharge("Y")
                        .build()));
    }

    @Test
    @DisplayName("Demurrage is calculated from the containers the UI sends, not from the database")
    void demurrageUsesUiContainers() {
        stubDemurrageChargeLookup(new BigDecimal("1250.000"));

        RefreshChargeDataRequestDTO request = RefreshChargeDataRequestDTO.builder()
                .blPoid(BL_POID)
                .blTypeInvoice("IMPORT")
                .containerDetails(List.of(
                        container("ABCU1111111", "22G1", "500.000"),
                        container("ABCU2222222", "42G1", "750.000")))
                .build();

        LoadChargeDataResponseDTO response = service.refreshChargeData(INVOICE_POID, request);

        assertEquals(0, new BigDecimal("1250.000").compareTo(response.getDemurrageAmount()),
                "demurrage should be the sum of the UI container amounts");

        // The container demurrage query must not have run - the UI already told us the values.
        verify(jdbcTemplate, never()).query(contains("FUNC_RTN_DEM_DETTN_FULL"),
                any(RowMapper.class), any(Object[].class));

        // Containers are echoed back untouched so the UI grid is not reset.
        assertEquals(2, response.getContainers().size());
        assertEquals("ABCU1111111", response.getContainers().get(0).getContainerNo());
    }

    @Test
    @DisplayName("Containers are read from the database when the UI sends none")
    void fallsBackToDatabaseWhenNoUiContainers() {
        when(jdbcTemplate.query(contains("FUNC_RTN_DEM_DETTN_FULL"), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(container("DBCU9999999", "22G1", "300.000")));
        stubDemurrageChargeLookup(new BigDecimal("300.000"));

        RefreshChargeDataRequestDTO request = RefreshChargeDataRequestDTO.builder()
                .blPoid(BL_POID)
                .blTypeInvoice("IMPORT")
                .build();

        LoadChargeDataResponseDTO response = service.refreshChargeData(INVOICE_POID, request);

        assertEquals(1, response.getContainers().size());
        assertEquals("DBCU9999999", response.getContainers().get(0).getContainerNo());
        assertEquals(0, new BigDecimal("300.000").compareTo(response.getDemurrageAmount()));
    }

    @Test
    @DisplayName("A charge the user added by hand survives the refresh")
    void preservesUserAddedCharge() {
        SalesInvoiceChargesDtlDto userAdded = SalesInvoiceChargesDtlDto.builder()
                .blPoid(BL_POID)
                .chargePoid(5555L)
                .chargesDetRowId(0L)
                .amount(new BigDecimal("99.000"))
                .amountSelect("Y")
                .chargeNewRecord("Y")
                .build();

        RefreshChargeDataRequestDTO request = RefreshChargeDataRequestDTO.builder()
                .blPoid(BL_POID)
                .blTypeInvoice("IMPORT")
                .containerDetails(List.of())
                .chargesDetails(List.of(userAdded))
                .build();

        LoadChargeDataResponseDTO response = service.refreshChargeData(INVOICE_POID, request);

        assertTrue(response.getCharges().stream().anyMatch(c -> Long.valueOf(5555L).equals(c.getChargePoid())),
                "manually added charge should still be present after refresh");
    }

    @Test
    @DisplayName("A stale system-calculated charge from the UI is dropped, not duplicated")
    void dropsStaleSystemCharge() {
        stubDemurrageChargeLookup(new BigDecimal("500.000"));

        SalesInvoiceChargesDtlDto staleDemurrage = SalesInvoiceChargesDtlDto.builder()
                .blPoid(BL_POID)
                .chargePoid(DEMURRAGE_CHARGE_POID)
                .chargesDetRowId(0L)
                .amount(new BigDecimal("111.000"))
                .demurrageCharge("Y")
                .build();

        RefreshChargeDataRequestDTO request = RefreshChargeDataRequestDTO.builder()
                .blPoid(BL_POID)
                .blTypeInvoice("IMPORT")
                .containerDetails(List.of(container("ABCU1111111", "22G1", "500.000")))
                .chargesDetails(List.of(staleDemurrage))
                .build();

        LoadChargeDataResponseDTO response = service.refreshChargeData(INVOICE_POID, request);

        List<SalesInvoiceChargesDtlDto> demurrageRows = response.getCharges().stream()
                .filter(c -> DEMURRAGE_CHARGE_POID.equals(c.getChargePoid()))
                .toList();
        assertEquals(1, demurrageRows.size(), "exactly one demurrage line expected");
        assertEquals(0, new BigDecimal("500.000").compareTo(demurrageRows.get(0).getAmount()),
                "the recalculated amount should win over the stale one");
    }

    @Test
    @DisplayName("User edits on a manifest charge are carried onto the refreshed row")
    void carriesUserEditsOntoManifestRow() {
        when(jdbcTemplate.query(contains("SHIP_BL_MANIFEST_CHARGES_DTL CHARGEDTL"),
                any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(SalesInvoiceChargesDtlDto.builder()
                        .blPoid(BL_POID)
                        .chargePoid(MANIFEST_CHARGE_POID)
                        .chargesDetRowId(3L)
                        .amount(new BigDecimal("120.000"))
                        .amountSelect("Y")
                        .build()));

        // Same row as the UI has it, but the user unticked it and typed a print rate.
        SalesInvoiceChargesDtlDto edited = SalesInvoiceChargesDtlDto.builder()
                .blPoid(BL_POID)
                .chargePoid(MANIFEST_CHARGE_POID)
                .chargesDetRowId(3L)
                .amount(new BigDecimal("120.000"))
                .amountSelect("N")
                .printRateAmt(new BigDecimal("15.500"))
                .build();

        RefreshChargeDataRequestDTO request = RefreshChargeDataRequestDTO.builder()
                .blPoid(BL_POID)
                .blTypeInvoice("IMPORT")
                .containerDetails(List.of())
                .chargesDetails(List.of(edited))
                .build();

        LoadChargeDataResponseDTO response = service.refreshChargeData(INVOICE_POID, request);

        List<SalesInvoiceChargesDtlDto> rows = response.getCharges().stream()
                .filter(c -> MANIFEST_CHARGE_POID.equals(c.getChargePoid()))
                .toList();
        assertEquals(1, rows.size(), "manifest row should not be duplicated by the UI copy");
        assertEquals("N", rows.get(0).getAmountSelect(), "selection flag should be preserved");
        assertEquals(0, new BigDecimal("15.500").compareTo(rows.get(0).getPrintRateAmt()),
                "print rate should be preserved");
    }

    @Test
    @DisplayName("DetRowId runs in one sequence across charges and late charges")
    void sequencesDetRowIdAcrossBothLists() {
        when(jdbcTemplate.query(contains("SHIP_BL_MANIFEST_CHARGES_DTL CHARGEDTL"),
                any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(
                        SalesInvoiceChargesDtlDto.builder().blPoid(BL_POID).chargePoid(MANIFEST_CHARGE_POID)
                                .chargesDetRowId(1L).amount(new BigDecimal("10.000")).build(),
                        SalesInvoiceChargesDtlDto.builder().blPoid(BL_POID).chargePoid(MANIFEST_CHARGE_POID)
                                .chargesDetRowId(2L).amount(new BigDecimal("20.000")).build()));
        when(jdbcTemplate.query(contains("SHIP_PORT_CHARGES_HDR"), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(SalesInvoiceChargesDtlDto.builder().blPoid(BL_POID)
                        .chargePoid(LATE_CHARGE_POID).chargesDetRowId(0L)
                        .amount(new BigDecimal("50.000")).build()));

        RefreshChargeDataRequestDTO request = RefreshChargeDataRequestDTO.builder()
                .blPoid(BL_POID)
                .blTypeInvoice("IMPORT")
                .containerDetails(List.of())
                .build();

        LoadChargeDataResponseDTO response = service.refreshChargeData(INVOICE_POID, request);

        assertEquals(2, response.getCharges().size());
        assertEquals(1L, response.getCharges().get(0).getDetRowId());
        assertEquals(2L, response.getCharges().get(1).getDetRowId());

        assertFalse(response.getLateCharges().isEmpty());
        assertEquals(3L, response.getLateCharges().get(0).getDetRowId(),
                "late charges continue the same sequence rather than restarting at 1");
    }

    @Test
    @DisplayName("An unsaved invoice (-999) does not hit the header repository")
    void handlesUnsavedInvoice() {
        RefreshChargeDataRequestDTO request = RefreshChargeDataRequestDTO.builder()
                .blPoid(BL_POID)
                .blTypeInvoice("IMPORT")
                .containerDetails(List.of())
                .build();

        LoadChargeDataResponseDTO response = service.refreshChargeData(-999L, request);

        assertNotNull(response);
        verify(hdrRepository, never()).findActiveByTransactionPoid(-999L);
    }
}
