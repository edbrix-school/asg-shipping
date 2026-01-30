package com.asg.shipping.bookingFormSH.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.PrintService;
import com.asg.shipping.bookingFormSH.dto.BookingFormCargoDetailDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormChargesDetailDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormContainerDetailDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormCreateDTO;
import com.asg.shipping.bookingFormSH.dto.BookingFormDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormUpdateDTO;
import com.asg.shipping.bookingFormSH.entity.ShipMateHdr;
import com.asg.shipping.bookingFormSH.repository.ShipMateCargoDtlRepository;
import com.asg.shipping.bookingFormSH.repository.ShipMateChargesDtlRepository;
import com.asg.shipping.bookingFormSH.repository.ShipMateContainerDtlRepository;
import com.asg.shipping.bookingFormSH.repository.ShipMateHdrRepository;
import com.asg.shipping.bookingFormSH.util.BookingFormMapper;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.exceptions.ValidationException;

import net.sf.jasperreports.engine.JasperReport;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingFormServiceImplTest {

	private static final Long GROUP_POID = 1L;
	private static final Long COMPANY_POID = 2L;
	private static final Long USER_POID = 3L;
	private static final Long TX_POID = 100L;

	@InjectMocks
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
	private BookingFormLovService lovService;
	@Mock
	private DocumentSearchService documentService;
	@Mock
	private BookingFormMapper mapper;
	@Mock
	private JdbcTemplate jdbcTemplate;
	@Mock
	private PrintService printService;
	@Mock
	private DataSource dataSource;
	@Mock
	private JasperReport mainReport;
	@Mock
	private JasperReport subReport;

	private MockedStatic<UserContext> userContext;

	@BeforeEach
	void init() {
		userContext = mockStatic(UserContext.class);
		userContext.when(UserContext::getGroupPoid).thenReturn(GROUP_POID);
		userContext.when(UserContext::getCompanyPoid).thenReturn(COMPANY_POID);
		userContext.when(UserContext::getUserPoid).thenReturn(USER_POID);
	}

	@AfterEach
	void tearDown() {
		userContext.close();
	}

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

		Map<String, Object> result = service.searchBookingForm("DOC", req, pageable);
		assertNotNull(result);
	}

	@Test
	void getBookingForm_success_withLov() {
		ShipMateHdr hdr = new ShipMateHdr();
		hdr.setDeleted("N");
		hdr.setTransactionPoid(TX_POID);
		hdr.setLinePoid(5L);
		hdr.setVesselPoid(10L);
		hdr.setQuotationTransactionPoid(15L);

		when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(TX_POID, GROUP_POID, COMPANY_POID))
				.thenReturn(Optional.of(hdr));

		when(cargoRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
		when(chargesRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
		when(containerRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());

		BookingFormDto dto = new BookingFormDto();
		when(mapper.mapToDto(any(ShipMateHdr.class))).thenReturn(dto);

		when(lovService.getQuotaionLov(anyLong())).thenReturn(List.of());
		when(lovService.getVesselMasterLov(anyLong())).thenReturn(List.of());
		when(lovService.getLineMasterLov(anyLong())).thenReturn(List.of());

		BookingFormDto result = service.getBookingForm(TX_POID);
		assertNotNull(result);
	}

	@Test
	void getBookingForm_deleted() {
		ShipMateHdr hdr = new ShipMateHdr();
		hdr.setDeleted("Y");
		when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(any(), any(), any()))
				.thenReturn(Optional.of(hdr));
		assertThrows(ResourceNotFoundException.class, () -> service.getBookingForm(TX_POID));
	}

	@Test
	void getBookingForm_notFound() {
		when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(any(), any(), any()))
				.thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> service.getBookingForm(TX_POID));
	}

	@Test
	void createBookingForm_success() {
		BookingFormCreateDTO dto = new BookingFormCreateDTO();
		dto.setLinePoid(10L);

		ShipMateHdr savedHdr = new ShipMateHdr();
		savedHdr.setTransactionPoid(TX_POID);
		savedHdr.setDeleted("N");
		savedHdr.setLinePoid(10L);

		when(headerRepository.existsByBookingIssueNoAndNotDeletedExcludingPoid(any(), isNull())).thenReturn(false);
		when(headerRepository.save(any())).thenReturn(savedHdr);

		when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(TX_POID, GROUP_POID, COMPANY_POID))
				.thenReturn(Optional.of(savedHdr));

		when(cargoRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
		when(chargesRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());
		when(containerRepo.findByTransactionPoidOrderByDetRowId(TX_POID)).thenReturn(List.of());

		doNothing().when(mapper).mapCreateDTOToEntity(any(), any(), anyLong(), anyLong());
		when(mapper.mapToDto(savedHdr)).thenReturn(new BookingFormDto());

		mockJdbcCall("Ok");

		BookingFormDto result = service.createBookingForm(dto);
		assertNotNull(result);
	}

	@Test
	void createBookingForm_lineMateValidationError() {
		BookingFormCreateDTO dto = new BookingFormCreateDTO();
		dto.setLinePoid(10L);
		mockJdbcCall("ERROR");

		ValidationException ex = assertThrows(ValidationException.class, () -> service.createBookingForm(dto));
		assertTrue(ex.getMessage().contains("Line and Mate validation failed"));
	}

	@Test
	void createBookingForm_containerValidationError() {
		BookingFormCreateDTO dto = new BookingFormCreateDTO();
		dto.setLinePoid(10L);
		BookingFormContainerDetailDto container = new BookingFormContainerDetailDto();
		container.setContainerNo("ABC");
		dto.setContainerDetails(List.of(container));

		when(headerRepository.existsByBookingIssueNoAndNotDeletedExcludingPoid(any(), isNull())).thenReturn(false);
		when(headerRepository.save(any())).thenReturn(new ShipMateHdr());

		mockJdbcCall("Ok");
		doAnswer(inv -> {
			throw new ValidationException("Container load validation failed");
		}).when(jdbcTemplate).execute(any(ConnectionCallback.class));

		assertThrows(ValidationException.class, () -> service.createBookingForm(dto));
	}

	@Test
	void updateBookingForm_success() {
		BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
		dto.setLinePoid(10L);
		dto.setBookingIssueNo("NEW");
		dto.setCargoDetails(List.of(new BookingFormCargoDetailDto()));
		dto.setChargesDetails(List.of(new BookingFormChargesDetailDto()));
		dto.setContainerDetails(List.of(new BookingFormContainerDetailDto()));

		ShipMateHdr entity = new ShipMateHdr();
		entity.setDeleted("N");
		entity.setLinePoid(5L);
		entity.setBookingIssueNo("OLD");

		when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(any(), any(), any()))
				.thenReturn(Optional.of(entity));
		when(headerRepository.existsByBookingIssueNoAndNotDeletedExcludingPoid("NEW", TX_POID)).thenReturn(false);

		doNothing().when(mapper).mapUpdateDTOToEntity(any(), any());
		mockJdbcCall("Ok");

		service.updateBookingForm(TX_POID, dto);

		assertEquals("NEW", entity.getBookingIssueNo());
		verify(cargoRepo).deleteByTransactionPoid(TX_POID);
		verify(chargesRepo).deleteByTransactionPoid(TX_POID);
		verify(containerRepo).deleteByTransactionPoid(TX_POID);
	}

	@Test
	void updateBookingForm_duplicateBookingIssue() {
		BookingFormUpdateDTO dto = new BookingFormUpdateDTO();
		dto.setBookingIssueNo("DUP");

		ShipMateHdr hdr = new ShipMateHdr();
		hdr.setDeleted("N");
		hdr.setBookingIssueNo("OLD");

		when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(any(), any(), any()))
				.thenReturn(Optional.of(hdr));
		when(headerRepository.existsByBookingIssueNoAndNotDeletedExcludingPoid("DUP", TX_POID)).thenReturn(true);

		assertThrows(ValidationException.class, () -> service.updateBookingForm(TX_POID, dto));
	}

	@Test
	void deleteBookingForm_success() {
		ShipMateHdr hdr = new ShipMateHdr();
		hdr.setDeleted("N");

		when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(any(), any(), any()))
				.thenReturn(Optional.of(hdr));

		service.deleteBookingForm(TX_POID);
		assertEquals("Y", hdr.getDeleted());
	}

	@Test
	void generateCoprarBooking_success() {
		mockJdbcCallWithOutParam("Copran generated, Sent Mail...");
		String result = service.generateCoprarBooking(TX_POID);
		assertEquals("Copran generated, Sent Mail...", result);
	}

	@Test
	void generateCoprarBooking_exception() {
		when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenThrow(new RuntimeException("DB"));
		assertThrows(ValidationException.class, () -> service.generateCoprarBooking(TX_POID));
	}

	@Test
	void getEmptyShipper_success() {
		mockJdbcCallWithOutParam("SHIPPER");
		String result = service.getEmptyShipper(COMPANY_POID);
		assertEquals("SHIPPER", result);
	}

	@Test
	void processEmptyContainerLoad_success() {
		mockJdbcCallWithOutParam("DONE");
		String result = service.processEmptyContainerLoad(TX_POID);
		assertEquals("DONE", result);
	}

	@Test
	void print_shouldGeneratePdfSuccessfully() throws Exception {

		Long transactionPoid = 1L;

		Map<String, Object> params = new HashMap<>();

		byte[] expectedPdf = "PDF_DATA".getBytes();

		when(printService.buildBaseParams(transactionPoid, "100-140")).thenReturn(params);

		when(printService.load("Shipping/SH/Container_mate_receipts.jrxml")).thenReturn(mainReport);

		when(printService.load("Shipping/SH/Container_mate_receipts_subreport1.jrxml")).thenReturn(subReport);

		when(printService.fillReportToPdf(mainReport, params, dataSource)).thenReturn(expectedPdf);

		byte[] result = service.print(transactionPoid);

		assertNotNull(result);
		assertArrayEquals(expectedPdf, result);

		verify(printService).buildBaseParams(transactionPoid, "100-140");
		verify(printService).load("Shipping/SH/Container_mate_receipts.jrxml");
		verify(printService).load("Shipping/SH/Container_mate_receipts_subreport1.jrxml");
		verify(printService).fillReportToPdf(mainReport, params, dataSource);

		assertEquals(subReport, params.get("CONTAINER_MATE_RECEIPTS_SUBREPORT_1"));
	}

	private void mockJdbcCall(String returnValue) {
		lenient().when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenAnswer(inv -> {
			ConnectionCallback<?> cb = inv.getArgument(0);
			Connection conn = mock(Connection.class);
			CallableStatement cs = mock(CallableStatement.class);
			when(conn.prepareCall(any())).thenReturn(cs);
			doReturn(returnValue).when(cs).getString(anyInt());
			return cb.doInConnection(conn);
		});
	}

	private void mockJdbcCallWithOutParam(String returnValue) {
		mockJdbcCall(returnValue);
	}
}
