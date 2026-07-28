package com.asg.shipping.demurrageenquiryblwise;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.shipping.demurrageenquiryblwise.dto.ContainerDemurrageCalcDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageChargeConfigDto;
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
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE))
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
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", emptyIn))
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
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE))
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
		when(enquiryRepository.calculateContainerDemurrage(BL_POID, "CONT001", TO_DATE))
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
		when(enquiryRepository.calculateContainerDemurrage(eq(BL_POID), anyString(), eq(TO_DATE)))
				.thenReturn(calc(LocalDate.of(2025, 7, 19), TO_DATE, 5L, BigDecimal.ZERO));
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
	void printDemurrageCalculation_passesTheParametersTheReportCanParse() throws Exception {
		Map<String, Object> baseParams = new java.util.HashMap<>();
		when(printService.buildBaseParams(BL_POID, "100-144")).thenReturn(baseParams);
		when(printService.load(anyString())).thenReturn(null);
		when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

		service.printDemurrageCalculation(BL_POID, TO_DATE, new BigDecimal("12.5"));

		// LINE_DEMURRAGE_DTL does to_date(SUBSTR(P_TILL_DATE,1,10),'RRRR-MM-DD') - anything but
		// yyyy-MM-dd fails the fill with ORA-01858.
		assertEquals("2025-07-28", baseParams.get("P_TILL_DATE"));
		// ... and TO_NUMBER(P_DISCOUNT), so it has to stay a plain number without a percent sign.
		assertEquals("12.5", baseParams.get("P_DISCOUNT"));
		assertEquals(String.valueOf(BL_POID), baseParams.get("DOC_KEY_POID"));
		assertTrue(baseParams.containsKey("SUBREPORT_DEMURRAGE_MASTER"));
		assertTrue(baseParams.containsKey("SUBREPORT_DEMURRAGE_DTL"));
	}

	@Test
	void printDemurrageCalculation_defaultsDiscountToZero() throws Exception {
		Map<String, Object> baseParams = new java.util.HashMap<>();
		when(printService.buildBaseParams(BL_POID, "100-144")).thenReturn(baseParams);
		when(printService.load(anyString())).thenReturn(null);
		when(printService.fillReportToPdf(any(), any(), any())).thenReturn(new byte[]{1});

		service.printDemurrageCalculation(BL_POID, TO_DATE, null);

		assertEquals("0", baseParams.get("P_DISCOUNT"));
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
