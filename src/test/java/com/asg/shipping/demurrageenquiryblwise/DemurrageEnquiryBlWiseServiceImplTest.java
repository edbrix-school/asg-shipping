package com.asg.shipping.demurrageenquiryblwise;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.shipping.demurrageenquiryblwise.dto.ContainerDemurrageCalcDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageChargeConfigDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageContainerCalcRequestDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageContainerCalcResponseDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageContainerCalcRowDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageEnquiryChargeDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageEnquiryContainerDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageEnquiryRequestDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageEnquiryResponseDto;
import com.asg.shipping.demurrageenquiryblwise.dto.ManifestChargeRowDto;
import com.asg.shipping.demurrageenquiryblwise.dto.PortChargeRowDto;
import com.asg.shipping.demurrageenquiryblwise.repository.DemurrageEnquiryBlWiseRepository;
import com.asg.shipping.demurrageenquiryblwise.service.impl.DemurrageEnquiryBlWiseServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DemurrageEnquiryBlWiseServiceImplTest {

	private static final Long BL_POID = 1001L;
	private static final LocalDate TO_DATE = LocalDate.of(2025, 7, 28);

	@Mock
	private DemurrageEnquiryBlWiseRepository enquiryRepository;
	@Mock
	private DocumentSearchService documentSearchService;
	@Mock
	private LovDataService lovService;
	@Mock
	private PrintService printService;
	@Mock
	private DataSource dataSource;

	@InjectMocks
	private DemurrageEnquiryBlWiseServiceImpl service;

	private MockedStatic<UserContext> userContextMock;

	@BeforeEach
	void setUp() {
		userContextMock = Mockito.mockStatic(UserContext.class);
		userContextMock.when(UserContext::getCompanyPoid).thenReturn(100L);
		userContextMock.when(UserContext::getGroupPoid).thenReturn(1L);
		userContextMock.when(UserContext::getTimeZoneCode).thenReturn("Asia/Bahrain");

		when(enquiryRepository.blExists(BL_POID)).thenReturn(true);
		when(enquiryRepository.findDoStatus(BL_POID)).thenReturn("DOISSUED");
		when(enquiryRepository.findManifestCharges(any())).thenReturn(List.of());
		when(enquiryRepository.findPortCharges(any(), any())).thenReturn(List.of());
		when(enquiryRepository.findDemurrageChargeConfig(any())).thenReturn(null);
		when(lovService.getDetailsByPoidsAndLovName(anyList(), anyString())).thenReturn(Map.of());
	}

	@AfterEach
	void tearDown() {
		userContextMock.close();
	}

	@Test
	void applyDate_unknownBl_throwsNotFound() {
		when(enquiryRepository.blExists(9999L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> service.applyDate(DemurrageEnquiryRequestDto.builder().blPoid(9999L).build()));
	}

	@Test
	void applyDate_appliesDiscountOnContainerDemurrage() {
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", null)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 10L, new BigDecimal("1000")));

		DemurrageEnquiryResponseDto response = service.applyDate(request(TO_DATE, new BigDecimal("10")));

		DemurrageEnquiryContainerDto row = response.getContainers().get(0);
		assertEquals(10L, row.getDmDays());
		assertEquals(0, new BigDecimal("1000").compareTo(row.getDmChargeAmtBeforeDiscount()));
		assertEquals(0, new BigDecimal("900").compareTo(row.getDmChargeAmt()));
		assertEquals(0, new BigDecimal("900").compareTo(response.getTotalDemurrageAmount()));
		assertEquals(TO_DATE, row.getDmToDate());
	}

	@Test
	void applyDate_containerReturnedEmpty_isChargedUpToEmptyInDate() {
		LocalDate emptyIn = LocalDate.of(2025, 7, 20);
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", emptyIn)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", emptyIn, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), emptyIn, 2L, new BigDecimal("200")));

		DemurrageEnquiryResponseDto response = service.applyDate(request(TO_DATE, BigDecimal.ZERO));

		DemurrageEnquiryContainerDto row = response.getContainers().get(0);
		assertEquals(emptyIn, row.getDmToDate());
		assertEquals(2L, row.getDmDays());
		assertEquals(0, new BigDecimal("200").compareTo(row.getDmChargeAmt()));
	}

	@Test
	void applyDate_withinFreeDays_returnsZeroDaysAndAmount() {
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", null)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 8, 1), TO_DATE, -4L, new BigDecimal("500")));

		DemurrageEnquiryResponseDto response = service.applyDate(request(TO_DATE, BigDecimal.ZERO));

		DemurrageEnquiryContainerDto row = response.getContainers().get(0);
		assertEquals(0L, row.getDmDays());
		assertEquals(0, BigDecimal.ZERO.compareTo(row.getDmChargeAmt()));
		assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalDemurrageAmount()));
		assertTrue(response.getCharges().isEmpty());
	}

	@Test
	void applyDate_buildsDemurrageChargeRowWithTax() {
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", null)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 10L, new BigDecimal("1000")));
		when(enquiryRepository.findDemurrageChargeConfig(100L)).thenReturn(DemurrageChargeConfigDto.builder()
				.chargePoid(2001L)
				.taxPoid(5L)
				.taxPercentage(new BigDecimal("10"))
				.taxApplicable("Y")
				.build());

		DemurrageEnquiryResponseDto response = service.applyDate(request(TO_DATE, BigDecimal.ZERO));

		DemurrageEnquiryChargeDto charge = response.getCharges().get(0);
		assertEquals("SHDEMURRAGE", charge.getChargeType());
		assertEquals(2001L, charge.getChargePoid());
		assertEquals(0, new BigDecimal("1000").compareTo(charge.getAmount()));
		assertEquals(0, new BigDecimal("100").compareTo(charge.getTaxAmount()));
		assertEquals(0, new BigDecimal("1000").compareTo(response.getReceiptAmount()));
		assertEquals(0, new BigDecimal("1100").compareTo(response.getTotalAmountWithVat()));
	}

	@Test
	void applyDate_totalsIncludeManifestAndPortCharges() {
		when(enquiryRepository.loadContainers(BL_POID))
				.thenReturn(List.of(container("CONT001", null), container("CONT002", null)));
		when(enquiryRepository.calculateContainerDemurrage(eq(BL_POID), anyString(), eq(TO_DATE), any()))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 5L, new BigDecimal("100")));
		when(enquiryRepository.findManifestCharges(BL_POID)).thenReturn(List.of(ManifestChargeRowDto.builder()
				.blPoid(BL_POID)
				.chargePoid(3001L)
				.detRowId(1L)
				.amount(new BigDecimal("250"))
				.taxPoid(5L)
				.taxPercentage(new BigDecimal("10"))
				.taxAmount(new BigDecimal("25"))
				.totalAmount(new BigDecimal("275"))
				.build()));
		when(enquiryRepository.findPortCharges(BL_POID, 100L)).thenReturn(List.of(
				PortChargeRowDto.builder()
						.chargeTypeApplicable("LATECOLLECTIONIMP")
						.chargeApplicable("PERBL")
						.chargeCodePoid(4001L)
						.amountOther(new BigDecimal("50"))
						.taxPoid(5L)
						.taxPercentage(new BigDecimal("10"))
						.taxApplicable("Y")
						.build(),
				PortChargeRowDto.builder()
						.chargeTypeApplicable("REVALIDATEIMP")
						.chargeApplicable("PERQUENTITY")
						.chargeCodePoid(4002L)
						.amount20(new BigDecimal("30"))
						.amount40(new BigDecimal("60"))
						.taxApplicable("N")
						.build()));
		when(enquiryRepository.getContainerSize("22G1")).thenReturn("20");

		DemurrageEnquiryResponseDto response = service.applyDate(request(TO_DATE, BigDecimal.ZERO));

		// 250 manifest + 50 per BL + (2 x 30) per quantity
		assertEquals(3, response.getCharges().size());
		assertEquals(0, new BigDecimal("360").compareTo(response.getReceiptAmount()));
		assertEquals(0, new BigDecimal("30").compareTo(response.getTotalTaxAmount()));
		assertEquals(0, new BigDecimal("390").compareTo(response.getTotalAmountWithVat()));
	}

	@Test
	void applyDate_perQuantityChargeCountsOnlyContainersCarryingDemurrage() {
		when(enquiryRepository.loadContainers(BL_POID))
				.thenReturn(List.of(container("CONT001", null), container("CONT002", null)));
		// CONT001 is still inside its free days, CONT002 is charged
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 0L, new BigDecimal("100")));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT002", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 5L, new BigDecimal("100")));
		when(enquiryRepository.findPortCharges(BL_POID, 100L)).thenReturn(List.of(PortChargeRowDto.builder()
				.chargeTypeApplicable("REVALIDATEIMP")
				.chargeApplicable("PERQUENTITY")
				.chargeCodePoid(4002L)
				.amount20(new BigDecimal("30"))
				.amount40(new BigDecimal("60"))
				.taxApplicable("N")
				.build()));
		when(enquiryRepository.getContainerSize("22G1")).thenReturn("20");

		DemurrageEnquiryResponseDto response = service.applyDate(request(TO_DATE, BigDecimal.ZERO));

		DemurrageEnquiryChargeDto perQuantity = response.getCharges().stream()
				.filter(charge -> "REVALIDATEIMP".equals(charge.getChargeType()))
				.findFirst()
				.orElseThrow();
		// one chargeable container x the 20' rate, not both containers
		assertEquals(0, new BigDecimal("30").compareTo(perQuantity.getAmount()));
	}

	/**
	 * The screen prints amounts with 3 decimals; Oracle hands back whatever scale the column has, so
	 * the service normalises every monetary figure on the way out.
	 */
	@Test
	void applyDate_returnsAmountsOnTheScaleTheScreenShows() {
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", null)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 10L, new BigDecimal("192080")));
		when(enquiryRepository.findDemurrageChargeConfig(100L)).thenReturn(DemurrageChargeConfigDto.builder()
				.chargePoid(94L)
				.taxPoid(3L)
				.taxPercentage(BigDecimal.ZERO)
				.taxApplicable("Y")
				.build());

		DemurrageEnquiryResponseDto response = service.applyDate(request(TO_DATE, BigDecimal.ZERO));

		DemurrageEnquiryChargeDto charge = response.getCharges().get(0);
		assertEquals("192080.000", charge.getAmount().toPlainString());
		assertEquals("0.000", charge.getTaxAmount().toPlainString());
		assertEquals("192080.000", response.getReceiptAmount().toPlainString());
		assertEquals("192080.000", response.getTotalAmountWithVat().toPlainString());
		assertEquals("192080.000", response.getContainers().get(0).getDmChargeAmt().toPlainString());
		// a zero rate charge still carries its tax master, as the legacy grid shows it
		assertEquals(3L, charge.getTaxPoid());
		// the demurrage row never carries Remarks - the breakdown lives on the Containers tab
		assertNull(charge.getRemarks());
	}

	/**
	 * The scale has to survive serialisation even for a value the service did not compute - the
	 * amounts are written by {@link com.asg.shipping.demurrageenquiryblwise.dto.AmountSerializer}.
	 */
	@Test
	void responseJsonWritesEveryAmountWithThreeDecimals() throws Exception {
		DemurrageEnquiryResponseDto response = DemurrageEnquiryResponseDto.builder()
				.blPoid(BL_POID)
				.receiptAmount(new BigDecimal("192136"))          // unscaled, straight off a NUMBER column
				.totalTaxAmount(new BigDecimal("2.8"))
				.totalAmountWithVat(new BigDecimal("192138.8"))
				.totalDemurrageAmount(new BigDecimal("192080"))
				.containers(List.of(DemurrageEnquiryContainerDto.builder()
						.containerNo("CONT001")
						.dmChargeAmt(new BigDecimal("13720"))
						.dmChargeAmtBeforeDiscount(new BigDecimal("13720"))
						.build()))
				.charges(List.of(DemurrageEnquiryChargeDto.builder()
						.chargePoid(94L)
						.amount(new BigDecimal("56"))
						.taxAmount(new BigDecimal("2.8"))
						.totalAmount(new BigDecimal("58.8"))
						.taxPercentage(new BigDecimal("5"))
						.build()))
				.build();

		String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(response);

		for (String amount : List.of("\"receiptAmount\":192136.000", "\"totalTaxAmount\":2.800",
				"\"totalAmountWithVat\":192138.800", "\"totalDemurrageAmount\":192080.000",
				"\"dmChargeAmt\":13720.000", "\"dmChargeAmtBeforeDiscount\":13720.000",
				"\"amount\":56.000", "\"taxAmount\":2.800", "\"totalAmount\":58.800")) {
			assertTrue(json.contains(amount), "missing " + amount + " in " + json);
		}
		// percentages are not amounts and keep their own scale, as the legacy AmtColumns did
		assertTrue(json.contains("\"taxPercentage\":5"), json);
	}

	@Test
	void applyDate_withoutCompanyContext_failsInsteadOfDroppingCharges() {
		userContextMock.when(UserContext::getCompanyPoid).thenReturn(null);
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", null)));

		// RTN_GLOBAL_PARAMETER raises ORA-01400 on a null company; silently returning fewer charges
		// than the legacy screen is worse than refusing the request.
		assertThrows(ValidationException.class, () -> service.applyDate(request(TO_DATE, BigDecimal.ZERO)));
	}

	@Test
	void printDemurrageCalculation_passesTheParametersTheReportCanParse() throws Exception {
		Map<String, Object> baseParams = new java.util.HashMap<>();
		when(printService.buildBaseParams(BL_POID, "100-144")).thenReturn(baseParams);
		when(printService.load(anyString())).thenReturn(null);
		when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

		service.printDemurrageCalculation(BL_POID, TO_DATE, new BigDecimal("12.5"), 7);

		// LINE_DEMURRAGE_DTL does to_date(SUBSTR(P_TILL_DATE,1,10),'RRRR-MM-DD') - anything but
		// yyyy-MM-dd fails the fill with ORA-01858.
		assertEquals("2025-07-28", baseParams.get("P_TILL_DATE"));
		// ... and TO_NUMBER(P_DISCOUNT), so it has to stay a plain number without a percent sign.
		assertEquals("12.5", baseParams.get("P_DISCOUNT"));
		assertEquals(String.valueOf(BL_POID), baseParams.get("DOC_KEY_POID"));
		// ... and TO_NUMBER(P_FREE_DAYS) in the same way
		assertEquals("7", baseParams.get("P_FREE_DAYS"));
		assertTrue(baseParams.containsKey("SUBREPORT_DEMURRAGE_MASTER"));
		assertTrue(baseParams.containsKey("SUBREPORT_DEMURRAGE_DTL"));
	}

	@Test
	void printDemurrageCalculation_defaultsDiscountToZero() throws Exception {
		Map<String, Object> baseParams = new java.util.HashMap<>();
		when(printService.buildBaseParams(BL_POID, "100-144")).thenReturn(baseParams);
		when(printService.load(anyString())).thenReturn(null);
		when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[]{1});

		service.printDemurrageCalculation(BL_POID, TO_DATE, null, null);

		assertEquals("0", baseParams.get("P_DISCOUNT"));
		// the report reads 0 as "keep the free days of the line tariff"
		assertEquals("0", baseParams.get("P_FREE_DAYS"));
	}

	@Test
	void printDemurrageCalculation_zeroFreeDaysIsNotAnOverride() throws Exception {
		Map<String, Object> baseParams = new java.util.HashMap<>();
		when(printService.buildBaseParams(BL_POID, "100-144")).thenReturn(baseParams);
		when(printService.load(anyString())).thenReturn(null);
		when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[]{1});

		service.printDemurrageCalculation(BL_POID, TO_DATE, null, 0);

		assertEquals("0", baseParams.get("P_FREE_DAYS"));
	}

	@Test
	void getBlDetails_clearsCalculatedColumns() {
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", null)));

		DemurrageEnquiryResponseDto response = service.getBlDetails(BL_POID);

		DemurrageEnquiryContainerDto row = response.getContainers().get(0);
		assertNull(row.getDmToDate());
		assertNull(row.getDmDays());
		assertNull(row.getDmChargeAmt());
		assertEquals("DOISSUED", response.getDoStatus());
		assertTrue(response.getCharges().isEmpty());
	}

	@Test
	void applyDate_passesTheRequestedFreeDaysToTheCalculationAndShowsThemOnTheRow() {
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", null)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, 7))
				.thenReturn(calc(LocalDate.of(2025, 7, 21), TO_DATE, 8L, new BigDecimal("800")));

		DemurrageEnquiryResponseDto response = service.applyDate(request(TO_DATE, BigDecimal.ZERO, 7));

		assertEquals(Integer.valueOf(7), response.getFreeDays());
		DemurrageEnquiryContainerDto row = response.getContainers().get(0);
		// the tariff resolved 5 free days, the request asked for 7
		assertEquals(7L, row.getFreeDays());
		assertEquals(8L, row.getDmDays());
		assertEquals(0, new BigDecimal("800").compareTo(row.getDmChargeAmt()));
	}

	/**
	 * The procedure reads 0 as "keep the free days of the container / line tariff", so a zero on the
	 * request must not be pushed down as an override - it would look like a deliberate 0 free days.
	 */
	@Test
	void applyDate_zeroFreeDaysKeepsTheTariffFreeDays() {
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", null)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 10L, new BigDecimal("1000")));

		DemurrageEnquiryResponseDto response = service.applyDate(request(TO_DATE, BigDecimal.ZERO, 0));

		assertNull(response.getFreeDays());
		// the free days of the tariff survive on the row
		assertEquals(5L, response.getContainers().get(0).getFreeDays());
		assertEquals(10L, response.getContainers().get(0).getDmDays());
	}

	/**
	 * The breakdown explains the containers, not the charge: it fills the Remarks of the Containers
	 * tab and leaves the calculated demurrage row empty.
	 */
	@Test
	void applyDate_fillsTheSlabBreakdownOnTheContainersOnly() {
		when(enquiryRepository.loadContainers(BL_POID))
				.thenReturn(List.of(container("CONT001", null), container("CONT002", null)));
		when(enquiryRepository.calculateContainerDemurrage(eq(BL_POID), anyString(), eq(TO_DATE), any()))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 10L, new BigDecimal("500")));
		when(enquiryRepository.findContainerDemurrageRemarks(BL_POID, TO_DATE, null))
				.thenReturn(Map.of("CONT001", "5 Days x 10.000", "CONT002", "5 Days x 20.000"));
		when(enquiryRepository.findDemurrageChargeConfig(100L))
				.thenReturn(DemurrageChargeConfigDto.builder().chargePoid(94L).taxApplicable("N").build());

		DemurrageEnquiryResponseDto response = service.applyDate(request(TO_DATE, BigDecimal.ZERO));

		assertEquals("5 Days x 10.000", response.getContainers().get(0).getRemarks());
		assertEquals("5 Days x 20.000", response.getContainers().get(1).getRemarks());
		DemurrageEnquiryChargeDto charge = response.getCharges().stream()
				.filter(row -> "SHDEMURRAGE".equals(row.getChargeType()))
				.findFirst()
				.orElseThrow();
		assertNull(charge.getRemarks());
	}

	/**
	 * A container inside its free days is not charged, so there is no slab to explain - and the
	 * breakdown is not even read when the whole BL is uncharged.
	 */
	@Test
	void applyDate_withinFreeDays_readsNoBreakdown() {
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", null)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 8, 1), TO_DATE, -4L, new BigDecimal("500")));

		DemurrageEnquiryResponseDto response = service.applyDate(request(TO_DATE, BigDecimal.ZERO));

		assertNull(response.getContainers().get(0).getRemarks());
		verify(enquiryRepository, never()).findContainerDemurrageRemarks(any(), any(), any());
	}

	/**
	 * The breakdown explains the amount, it is not the amount: losing it must not lose the enquiry.
	 */
	@Test
	void applyDate_withoutABreakdown_stillReturnsTheAmounts() {
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", null)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 10L, new BigDecimal("1000")));
		when(enquiryRepository.findContainerDemurrageRemarks(BL_POID, TO_DATE, null)).thenReturn(Map.of());

		DemurrageEnquiryResponseDto response = service.applyDate(request(TO_DATE, BigDecimal.ZERO));

		assertNull(response.getContainers().get(0).getRemarks());
		assertEquals(0, new BigDecimal("1000").compareTo(response.getTotalDemurrageAmount()));
	}

	/**
	 * The late collection fee only starts to bite on arrival + SHIPLATEDOCOLLECTION - 1, the day the
	 * query's own filter starts to hold, and the screen says so on the row.
	 */
	@Test
	void applyDate_lateCollectionChargeCarriesTheDateItStartsToApply() {
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", null)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 0L, BigDecimal.ZERO));
		when(enquiryRepository.findPortCharges(BL_POID, 100L)).thenReturn(List.of(PortChargeRowDto.builder()
				.chargeTypeApplicable("LATECOLLECTIONIMP")
				.chargeApplicable("PERBL")
				.chargeCodePoid(1124L)
				.amountOther(new BigDecimal("30"))
				.taxApplicable("N")
				.applicableFrom(LocalDate.of(2026, 5, 13))
				.build()));

		DemurrageEnquiryResponseDto response = service.applyDate(request(TO_DATE, BigDecimal.ZERO));

		DemurrageEnquiryChargeDto charge = response.getCharges().get(0);
		assertEquals("Applicable from 13-MAY-26 onwards", charge.getRemarks());
		assertEquals(0, new BigDecimal("30").compareTo(charge.getAmount()));
	}

	/** Revalidation is not date driven, so there is nothing to say about when it starts. */
	@Test
	void applyDate_revalidationChargeCarriesNoDateRemark() {
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", null)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 5L, new BigDecimal("100")));
		when(enquiryRepository.findPortCharges(BL_POID, 100L)).thenReturn(List.of(PortChargeRowDto.builder()
				.chargeTypeApplicable("REVALIDATEIMP")
				.chargeApplicable("PERQUENTITY")
				.chargeCodePoid(1126L)
				.amount20(new BigDecimal("4"))
				.amount40(new BigDecimal("4"))
				.taxApplicable("N")
				.build()));
		when(enquiryRepository.getContainerSize("22G1")).thenReturn("20");

		DemurrageEnquiryResponseDto response = service.applyDate(request(TO_DATE, BigDecimal.ZERO));

		DemurrageEnquiryChargeDto charge = response.getCharges().stream()
				.filter(row -> "REVALIDATEIMP".equals(row.getChargeType()))
				.findFirst()
				.orElseThrow();
		assertNull(charge.getRemarks());
	}

	@Test
	void calculateSelectedContainers_calculatesTheRequestedRowsOnly() {
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(
				List.of(container("CONT001", null), container("CONT002", null), container("CONT003", null)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 10L, new BigDecimal("1000")));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT003", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 4L, new BigDecimal("400")));

		DemurrageContainerCalcResponseDto response = service.calculateSelectedContainers(
				rowRequest(new BigDecimal("10"), null, row("CONT001", null), row("CONT003", null)));

		assertEquals(List.of("CONT001", "CONT003"), response.getContainers().stream()
				.map(DemurrageEnquiryContainerDto::getContainerNo).toList());
		assertEquals(0, new BigDecimal("900").compareTo(response.getContainers().get(0).getDmChargeAmt()));
		assertEquals(0, new BigDecimal("1000")
				.compareTo(response.getContainers().get(0).getDmChargeAmtBeforeDiscount()));
		assertEquals(0, new BigDecimal("360").compareTo(response.getContainers().get(1).getDmChargeAmt()));
		// the rows of the request only, CONT002 is not in the total
		assertEquals(0, new BigDecimal("1260").compareTo(response.getTotalDemurrageAmount()));
		verify(enquiryRepository, never()).calculateContainerDemurrage(BL_POID, "CONT002", TO_DATE, null);
	}

	@Test
	void calculateSelectedContainers_unknownBl_throwsNotFound() {
		when(enquiryRepository.blExists(9999L)).thenReturn(false);

		DemurrageContainerCalcRequestDto request = DemurrageContainerCalcRequestDto.builder()
				.blPoid(9999L)
				.containers(List.of(row("CONT001", null)))
				.build();

		assertThrows(ResourceNotFoundException.class, () -> service.calculateSelectedContainers(request));
	}

	/** A row that is not on the BL cannot be calculated - the procedure would return nothing for it. */
	@Test
	void calculateSelectedContainers_containerOfAnotherBl_isRejected() {
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", null)));

		DemurrageContainerCalcRequestDto request =
				rowRequest(BigDecimal.ZERO, null, row("CONT001", null), row("CONT999", null));

		ValidationException failure =
				assertThrows(ValidationException.class, () -> service.calculateSelectedContainers(request));
		assertTrue(failure.getMessage().contains("CONT999"));
		verify(enquiryRepository, never()).calculateContainerDemurrage(any(), anyString(), any(), any());
	}

	/** The Empty In date of the BL wins over the To Date of the request, as it does on Apply Date. */
	@Test
	void calculateSelectedContainers_containerReturnedEmpty_isChargedUpToEmptyInDate() {
		LocalDate emptyIn = LocalDate.of(2025, 7, 20);
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", emptyIn)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", emptyIn, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), emptyIn, 2L, new BigDecimal("200")));

		DemurrageContainerCalcResponseDto response =
				service.calculateSelectedContainers(rowRequest(BigDecimal.ZERO, null, row("CONT001", null)));

		DemurrageEnquiryContainerDto calculated = response.getContainers().get(0);
		assertEquals(emptyIn, calculated.getDmToDate());
		assertEquals(2L, calculated.getDmDays());
	}

	/** The free days of a row replace the ones of the request, which replace the ones of the tariff. */
	@Test
	void calculateSelectedContainers_rowFreeDaysWinOverTheRequestFreeDays() {
		when(enquiryRepository.loadContainers(BL_POID))
				.thenReturn(List.of(container("CONT001", null), container("CONT002", null)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, 14))
				.thenReturn(calc(LocalDate.of(2025, 7, 26), TO_DATE, 3L, new BigDecimal("300")));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT002", TO_DATE, 7))
				.thenReturn(calc(LocalDate.of(2025, 7, 21), TO_DATE, 8L, new BigDecimal("800")));

		DemurrageContainerCalcResponseDto response = service.calculateSelectedContainers(
				rowRequest(BigDecimal.ZERO, 7, row("CONT001", 14), row("CONT002", null)));

		assertEquals(Integer.valueOf(7), response.getFreeDays());
		assertEquals(14L, response.getContainers().get(0).getFreeDays());
		assertEquals(7L, response.getContainers().get(1).getFreeDays());
	}

	/** Zero is not an override, on the row no more than on the request: the tariff free days stay. */
	@Test
	void calculateSelectedContainers_zeroFreeDaysKeepsTheTariffFreeDays() {
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", null)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 10L, new BigDecimal("1000")));

		DemurrageContainerCalcResponseDto response =
				service.calculateSelectedContainers(rowRequest(BigDecimal.ZERO, 0, row("CONT001", 0)));

		assertNull(response.getFreeDays());
		assertEquals(5L, response.getContainers().get(0).getFreeDays());
	}

	/** Rows calculated with other free days, or up to another date, cannot share one breakdown. */
	@Test
	void calculateSelectedContainers_readsTheBreakdownPerToDateAndFreeDays() {
		LocalDate rowToDate = LocalDate.of(2025, 8, 4);
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(
				List.of(container("CONT001", null), container("CONT002", null), container("CONT003", null)));
		when(enquiryRepository.calculateContainerDemurrage(eq(BL_POID), anyString(), any(), any()))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 10L, new BigDecimal("500")));
		when(enquiryRepository.findContainerDemurrageRemarks(BL_POID, TO_DATE, 14))
				.thenReturn(Map.of("CONT001", "3 Days x 10.000"));
		when(enquiryRepository.findContainerDemurrageRemarks(BL_POID, TO_DATE, null))
				.thenReturn(Map.of("CONT002", "10 Days x 20.000"));
		when(enquiryRepository.findContainerDemurrageRemarks(BL_POID, rowToDate, null))
				.thenReturn(Map.of("CONT003", "17 Days x 20.000"));

		DemurrageContainerCalcResponseDto response = service.calculateSelectedContainers(
				rowRequest(BigDecimal.ZERO, null,
						row("CONT001", 14), row("CONT002", null), row("CONT003", rowToDate, null)));

		assertEquals("3 Days x 10.000", response.getContainers().get(0).getRemarks());
		assertEquals("10 Days x 20.000", response.getContainers().get(1).getRemarks());
		assertEquals("17 Days x 20.000", response.getContainers().get(2).getRemarks());
	}

	/** The same container twice is one row, not two calls of the procedure. */
	@Test
	void calculateSelectedContainers_ignoresADuplicatedContainer() {
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", null)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 10L, new BigDecimal("1000")));

		DemurrageContainerCalcResponseDto response = service.calculateSelectedContainers(
				rowRequest(BigDecimal.ZERO, null, row("CONT001", null), row("CONT001", null)));

		assertEquals(1, response.getContainers().size());
		assertEquals(0, new BigDecimal("1000").compareTo(response.getTotalDemurrageAmount()));
		verify(enquiryRepository, Mockito.times(1))
				.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, null);
	}

	@Test
	void calculateSelectedContainers_withinFreeDays_returnsZeroDaysAndAmount() {
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", null)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 8, 1), TO_DATE, -4L, new BigDecimal("500")));

		DemurrageContainerCalcResponseDto response =
				service.calculateSelectedContainers(rowRequest(BigDecimal.ZERO, null, row("CONT001", null)));

		DemurrageEnquiryContainerDto calculated = response.getContainers().get(0);
		assertEquals(0L, calculated.getDmDays());
		assertEquals(0, BigDecimal.ZERO.compareTo(calculated.getDmChargeAmt()));
		assertNull(calculated.getRemarks());
		assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalDemurrageAmount()));
		verify(enquiryRepository, never()).findContainerDemurrageRemarks(any(), any(), any());
	}

	/** The To Date of a row replaces the one of the request, which replaces the current date. */
	@Test
	void calculateSelectedContainers_rowToDateWinsOverTheRequestToDate() {
		LocalDate rowToDate = LocalDate.of(2025, 8, 4);
		when(enquiryRepository.loadContainers(BL_POID))
				.thenReturn(List.of(container("CONT001", null), container("CONT002", null)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", rowToDate, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), rowToDate, 17L, new BigDecimal("1700")));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT002", TO_DATE, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 10L, new BigDecimal("1000")));

		DemurrageContainerCalcResponseDto response = service.calculateSelectedContainers(
				rowRequest(BigDecimal.ZERO, null, row("CONT001", rowToDate, null), row("CONT002", null)));

		// the header keeps the To Date of the request, the rows show the one they were calculated up to
		assertEquals(TO_DATE, response.getToDate());
		assertEquals(rowToDate, response.getContainers().get(0).getDmToDate());
		assertEquals(17L, response.getContainers().get(0).getDmDays());
		assertEquals(TO_DATE, response.getContainers().get(1).getDmToDate());
		assertEquals(0, new BigDecimal("2700").compareTo(response.getTotalDemurrageAmount()));
	}

	/** The Empty In date of the BL wins over the To Date of the row too. */
	@Test
	void calculateSelectedContainers_rowToDateDoesNotOverrideTheEmptyInDate() {
		LocalDate emptyIn = LocalDate.of(2025, 7, 20);
		when(enquiryRepository.loadContainers(BL_POID)).thenReturn(List.of(container("CONT001", emptyIn)));
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", emptyIn, null))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), emptyIn, 2L, new BigDecimal("200")));

		DemurrageContainerCalcResponseDto response = service.calculateSelectedContainers(
				rowRequest(BigDecimal.ZERO, null, row("CONT001", LocalDate.of(2025, 8, 4), null)));

		assertEquals(emptyIn, response.getContainers().get(0).getDmToDate());
		assertEquals(2L, response.getContainers().get(0).getDmDays());
	}

	private DemurrageContainerCalcRequestDto rowRequest(BigDecimal discount, Integer freeDays,
														DemurrageContainerCalcRowDto... rows) {
		return DemurrageContainerCalcRequestDto.builder()
				.blPoid(BL_POID)
				.toDate(TO_DATE)
				.discountPercentage(discount)
				.freeDays(freeDays)
				.containers(List.of(rows))
				.build();
	}

	private DemurrageContainerCalcRowDto row(String containerNo, Integer freeDays) {
		return row(containerNo, null, freeDays);
	}

	private DemurrageContainerCalcRowDto row(String containerNo, LocalDate toDate, Integer freeDays) {
		return DemurrageContainerCalcRowDto.builder()
				.containerNo(containerNo)
				.toDate(toDate)
				.freeDays(freeDays)
				.build();
	}

	private DemurrageEnquiryRequestDto request(LocalDate toDate, BigDecimal discount, Integer freeDays) {
		return DemurrageEnquiryRequestDto.builder()
				.blPoid(BL_POID)
				.toDate(toDate)
				.discountPercentage(discount)
				.freeDays(freeDays)
				.build();
	}

	private DemurrageEnquiryRequestDto request(LocalDate toDate, BigDecimal discount) {
		return DemurrageEnquiryRequestDto.builder()
				.blPoid(BL_POID)
				.toDate(toDate)
				.discountPercentage(discount)
				.build();
	}

	private DemurrageEnquiryContainerDto container(String containerNo, LocalDate emptyIn) {
		return DemurrageEnquiryContainerDto.builder()
				.detRowId(1L)
				.blPoid(BL_POID)
				.containerNo(containerNo)
				.containerSocYn("N")
				.equipmentIsoType("22G1")
				.freeDays(5L)
				.dmFrmDate(LocalDate.of(2025, 7, 19))
				.emptyIn(emptyIn)
				.build();
	}

	private ContainerDemurrageCalcDto calc(LocalDate fromDate, LocalDate toDate, Long days, BigDecimal amount) {
		return ContainerDemurrageCalcDto.builder()
				.fromDate(fromDate)
				.toDate(toDate)
				.days(days)
				.amount(amount)
				.build();
	}
}
