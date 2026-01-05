package com.asg.shipping.dayCloseShiping.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.shipping.common.repository.GlobalCurrencyDenominationRepository;
import com.asg.shipping.dayCloseShiping.dto.DayCloseDenominationDto;
import com.asg.shipping.dayCloseShiping.dto.DayCloseDto;
import com.asg.shipping.dayCloseShiping.dto.DayCloseHdrDto;
import com.asg.shipping.dayCloseShiping.dto.DayCloseSummaryProjection;
import com.asg.shipping.dayCloseShiping.dto.DayCloseSummaryProjectionImpl;
import com.asg.shipping.dayCloseShiping.entity.ArShDayEndCloseDtl;
import com.asg.shipping.dayCloseShiping.entity.ArShDayEndCloseHdr;
import com.asg.shipping.dayCloseShiping.repository.ArShDayEndCloseDtlRepository;
import com.asg.shipping.dayCloseShiping.repository.ArShDayEndCloseHdrRepository;
import com.asg.shipping.dayCloseShiping.repository.ArShReceiptHdrRepository;
import com.asg.shipping.dayCloseShiping.util.DayCloseMapper;

@ExtendWith(MockitoExtension.class)
class DayCloseServiceImplTest {

	@Spy
	@InjectMocks
	private DayCloseServiceImpl service;

	@Mock
	private ArShDayEndCloseHdrRepository hdrRepo;
	@Mock
	private ArShDayEndCloseDtlRepository dtlRepo;
	@Mock
	private GlobalCurrencyDenominationRepository denomRepo;
	@Mock
	private ArShReceiptHdrRepository receiptHdrRepository;
	@Mock
	private JdbcTemplate jdbcTemplate;
	@Mock
	private DocumentSearchService documentService;
	@Mock
	private DayCloseMapper mapper;

	@Test
	void getDayClose_success() {

		ArShDayEndCloseHdr hdr = new ArShDayEndCloseHdr();
		hdr.setTransactionPoid(1L);
		hdr.setDeleted("N");

		when(hdrRepo.findById(1L)).thenReturn(Optional.of(hdr));
		when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of(new ArShDayEndCloseDtl()));
		when(mapper.mapToDto(hdr)).thenReturn(new DayCloseDto());
		when(mapper.mapDtlListToDto(any())).thenReturn(List.of());

		DayCloseDto result = service.getDayClose(1L, 1L, 1L);

		assertNotNull(result);
	}

	@Test
	void getDayClose_notFound() {
		when(hdrRepo.findById(1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.getDayClose(1L, 1L, 1L));
	}

	@Test
	void getNewDayCloseData_success() {

		DayCloseSummaryProjection summary = DayCloseSummaryProjectionImpl.builder().transactionDate(LocalDate.now())
				.cashAmount(BigDecimal.TEN).chequeAmount(BigDecimal.ONE).totalAmount(BigDecimal.valueOf(11))
				.chequeCount(1L).build();

		when(receiptHdrRepository.fetchNewDayCloseSummary(1L, 1L, "2024-01-01")).thenReturn(Optional.of(summary));

		DayCloseSummaryProjection result = service.getNewDayCloseData(1L, 1L, "2024-01-01");

		assertEquals(BigDecimal.TEN, result.getCashAmount());
	}

	@Test
	void getNewDayCloseData_notFound() {
		when(receiptHdrRepository.fetchNewDayCloseSummary(any(), any(), any())).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.getNewDayCloseData(1L, 1L, "2024-01-01"));
	}

	@Test
	void getDenominations_success() {

		var denom = mock(com.asg.shipping.common.entity.GlobalCurrencyDenomination.class);
		when(denom.getCurrencyAmount()).thenReturn("100");
		when(denom.getCurrencyType()).thenReturn("NOTE");

		when(denomRepo.findByCurrencyCodeOrderBySeqNo("INR")).thenReturn(List.of(denom));

		List<Map<String, Object>> result = service.getDenominations("INR");

		assertEquals(1, result.size());
		assertEquals(new BigDecimal("100"), result.get(0).get("denomination"));
	}

	@Test
	void searchDayClose_success() {

		FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());
		Pageable pageable = PageRequest.of(0, 10);

		RawSearchResult raw = new RawSearchResult(List.of(Map.of("PORT_POID", 1L, "PORT_NAME", "Port A")),
				Map.of("PORT_POID", "Port ID", "PORT_NAME", "Port Name"), 1L);

		when(documentService.resolveOperator(filters)).thenReturn("OR");
		when(documentService.resolveIsDeleted(filters)).thenReturn("false");
		when(documentService.resolveFilters(filters)).thenReturn(List.of());
		when(documentService.search(anyString(), anyList(), anyString(), eq(pageable), anyString(), anyString(),
				anyString())).thenReturn(raw);

		Map<String, Object> result = service.searchDayClose("DOC1", filters, pageable);

		assertNotNull(result);
	}

	@Test
	void createDayClose_success() {

		try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
				MockedConstruction<SimpleJdbcCall> mockedJdbc = Mockito.mockConstruction(SimpleJdbcCall.class,
						(mock, context) -> {
							when(mock.withProcedureName(any())).thenReturn(mock);
							when(mock.execute(any(Map.class))).thenReturn(Map.of("P_STATUS", "SUCCESS"));
						})) {

			mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

			DayCloseHdrDto hdrDto = DayCloseHdrDto.builder().transactionDate(LocalDate.now()).cashAmount(BigDecimal.TEN)
					.chequeAmount(BigDecimal.ZERO).totalAmount(BigDecimal.TEN).build();

			DayCloseDto dto = new DayCloseDto();
			dto.setHeader(hdrDto);

			when(hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoid(any(), any(), any())).thenReturn(0L);

			ArShDayEndCloseHdr hdr = new ArShDayEndCloseHdr();
			hdr.setTransactionPoid(1L);
			hdr.setTransactionDate(LocalDate.now());
			hdr.setDocRef("DOC_REF_001");

			doNothing().when(mapper).mapCreateDTOToEntity(any(), any(), any(), any());
			when(hdrRepo.save(any())).thenReturn(hdr);
			when(hdrRepo.findById(1L)).thenReturn(Optional.of(hdr));
			when(mapper.mapToDto(any())).thenReturn(new DayCloseDto());
			when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of());
			when(mapper.mapDtlListToDto(any())).thenReturn(List.of());

			DayCloseDto result = service.createDayClose(dto, 1L, 1L, 1L);

			assertNotNull(result);
		}
	}

	@Test
	void createDayClose_amountMismatch_throwsException() {

		DayCloseHdrDto hdrDto = DayCloseHdrDto.builder().cashAmount(BigDecimal.TEN).chequeAmount(BigDecimal.TEN)
				.totalAmount(BigDecimal.ONE).build();

		DayCloseDto dto = new DayCloseDto();
		dto.setHeader(hdrDto);

		assertThrows(ValidationException.class, () -> service.createDayClose(dto, 1L, 1L, 1L));
	}

	@Test
	void createDayClose_duplicateTransactionDate_throwsException() {

		DayCloseHdrDto hdrDto = DayCloseHdrDto.builder().transactionDate(LocalDate.now()).cashAmount(BigDecimal.TEN)
				.chequeAmount(BigDecimal.ZERO).totalAmount(BigDecimal.TEN).build();

		DayCloseDto dto = new DayCloseDto();
		dto.setHeader(hdrDto);

		when(hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoid(any(), any(), any())).thenReturn(1L);

		assertThrows(ValidationException.class, () -> service.createDayClose(dto, 1L, 1L, 1L));
	}

	@Test
	void createDayClose_procedureCallExecuted() {

		try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
				MockedConstruction<SimpleJdbcCall> mockedJdbc = Mockito.mockConstruction(SimpleJdbcCall.class,
						(mock, context) -> {
							when(mock.withProcedureName(any())).thenReturn(mock);
							when(mock.execute(any(Map.class))).thenReturn(Map.of("P_STATUS", "SUCCESS"));
						})) {

			mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

			DayCloseHdrDto hdrDto = DayCloseHdrDto.builder().transactionDate(LocalDate.now()).cashAmount(BigDecimal.TEN)
					.chequeAmount(BigDecimal.ZERO).totalAmount(BigDecimal.TEN).build();

			DayCloseDto dto = new DayCloseDto();
			dto.setHeader(hdrDto);

			ArShDayEndCloseHdr hdr = new ArShDayEndCloseHdr();
			hdr.setTransactionPoid(1L);
			hdr.setTransactionDate(LocalDate.now());
			hdr.setDocRef("DOC_REF");

			when(hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoid(any(), any(), any())).thenReturn(0L);
			doAnswer(invocation -> {
				ArShDayEndCloseHdr e = invocation.getArgument(1);
				e.setDocRef("DOC_REF");
				return null;
			}).when(mapper).mapCreateDTOToEntity(any(), any(), any(), any());

			when(hdrRepo.save(any())).thenReturn(hdr);
			when(hdrRepo.findById(1L)).thenReturn(Optional.of(hdr));
			when(mapper.mapToDto(any())).thenReturn(new DayCloseDto());
			when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of());
			when(mapper.mapDtlListToDto(any())).thenReturn(List.of());

			service.createDayClose(dto, 1L, 1L, 1L);

			assertEquals(1, mockedJdbc.constructed().size());
		}
	}

	@Test
	void updateDayClose_withDenominations_success() {

		DayCloseDenominationDto denom = new DayCloseDenominationDto();
		denom.setDenomination(new BigDecimal("100"));
		denom.setNoOfTran(2L);

		DayCloseHdrDto hdrDto = DayCloseHdrDto.builder().cashAmount(new BigDecimal("200")).chequeAmount(BigDecimal.ZERO)
				.totalAmount(new BigDecimal("200")).build();

		DayCloseDto dto = new DayCloseDto();
		dto.setHeader(hdrDto);
		dto.setDenominations(List.of(denom));

		when(dtlRepo.getMaxDetRowId(1L)).thenReturn(0L);
		when(mapper.mapDtlFromDto(any(), any(), any())).thenReturn(new ArShDayEndCloseDtl());
		when(hdrRepo.findById(1L)).thenReturn(Optional.of(new ArShDayEndCloseHdr()));
		when(mapper.mapToDto(any())).thenReturn(new DayCloseDto());
		when(mapper.mapDtlListToDto(any())).thenReturn(List.of());

		service.updateDayClose(dto, 1L, 1L, 1L, 1L);

		Mockito.verify(dtlRepo, Mockito.atLeastOnce()).save(any());
	}

	@Test
	void createDayClose_denominationMultiplyCovered() {

		try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class);
				MockedConstruction<SimpleJdbcCall> jdbcMock = Mockito.mockConstruction(SimpleJdbcCall.class,
						(mock, context) -> {
							when(mock.withProcedureName(any())).thenReturn(mock);
							when(mock.execute(any(Map.class))).thenReturn(Map.of("P_STATUS", "SUCCESS"));
						})) {

			userContextMock.when(UserContext::getDocumentId).thenReturn("DOC_TEST");

			DayCloseDenominationDto denom = new DayCloseDenominationDto();
			denom.setDenomination(new BigDecimal("100"));
			denom.setNoOfTran(2L);

			DayCloseHdrDto hdrDto = DayCloseHdrDto.builder().transactionDate(LocalDate.now())
					.cashAmount(new BigDecimal("200")).chequeAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("200"))
					.build();

			DayCloseDto dto = new DayCloseDto();
			dto.setHeader(hdrDto);
			dto.setDenominations(List.of(denom));

			when(hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoid(any(), any(), any())).thenReturn(0L);

			ArShDayEndCloseHdr hdr = new ArShDayEndCloseHdr();
			hdr.setTransactionPoid(1L);
			hdr.setTransactionDate(LocalDate.now());
			hdr.setDocRef("DOC1");

			doNothing().when(mapper).mapCreateDTOToEntity(any(), any(), any(), any());
			when(hdrRepo.save(any())).thenReturn(hdr);
			when(hdrRepo.findById(1L)).thenReturn(Optional.of(hdr));
			when(mapper.mapToDto(any())).thenReturn(new DayCloseDto());
			when(dtlRepo.findByTransactionPoid(1L)).thenReturn(List.of());
			when(mapper.mapDtlListToDto(any())).thenReturn(List.of());

			ArShDayEndCloseDtl dtlEntity = new ArShDayEndCloseDtl();
			dtlEntity.setDetRowId(null);

			when(mapper.mapDtlFromDto(any(), any(), any())).thenReturn(dtlEntity);
			when(dtlRepo.getMaxDetRowId(1L)).thenReturn(0L);
			when(dtlRepo.save(any())).thenReturn(dtlEntity);

			DayCloseDto result = service.createDayClose(dto, 1L, 1L, 1L);

			assertNotNull(result);
		}
	}

	@Test
	void createDayClose_denominationMismatch_throwsException() {

		DayCloseDenominationDto denom = new DayCloseDenominationDto();
		denom.setDenomination(new BigDecimal("100"));
		denom.setNoOfTran(1L); // 100

		DayCloseHdrDto hdrDto = DayCloseHdrDto.builder().transactionDate(LocalDate.now())
				.cashAmount(new BigDecimal("200")) // mismatch
				.chequeAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("200")).build();

		DayCloseDto dto = new DayCloseDto();
		dto.setHeader(hdrDto);
		dto.setDenominations(List.of(denom));

		when(hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoid(any(), any(), any())).thenReturn(0L);

		assertThrows(ValidationException.class, () -> service.createDayClose(dto, 1L, 1L, 1L));
	}

}
