package com.asg.shipping.dayCloseShiping.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.common.repository.GlobalCurrencyDenominationRepository;
import com.asg.shipping.dayCloseShiping.dto.DayCloseDenominationDto;
import com.asg.shipping.dayCloseShiping.dto.DayCloseDto;
import com.asg.shipping.dayCloseShiping.dto.DayCloseHdrDto;
import com.asg.shipping.dayCloseShiping.dto.DayCloseSummaryProjection;
import com.asg.shipping.dayCloseShiping.entity.ArShDayEndCloseDtl;
import com.asg.shipping.dayCloseShiping.entity.ArShDayEndCloseHdr;
import com.asg.shipping.dayCloseShiping.repository.ArShDayEndCloseDtlRepository;
import com.asg.shipping.dayCloseShiping.repository.ArShDayEndCloseHdrRepository;
import com.asg.shipping.dayCloseShiping.repository.ArShReceiptHdrRepository;
import com.asg.shipping.dayCloseShiping.util.DayCloseMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DayCloseServiceImpl implements DayCloseService {

	private final ArShDayEndCloseHdrRepository hdrRepo;
	private final ArShDayEndCloseDtlRepository dtlRepo;
	private final GlobalCurrencyDenominationRepository denomRepo;
	private final ArShReceiptHdrRepository receiptHdrRepository;
	private final JdbcTemplate jdbcTemplate;
	private final DocumentSearchService documentService;
	private final DayCloseMapper mapper;

	@Override
	public DayCloseDto getDayClose(Long transactionPoid, Long groupPoid, Long companyPoid) {

		ArShDayEndCloseHdr hdr = hdrRepo.findById(transactionPoid).filter(h -> !"Y".equals(h.getDeleted())).orElseThrow(
				() -> new ResourceNotFoundException("Day Close", "transactionPoid", transactionPoid.toString()));

		List<ArShDayEndCloseDtl> details = dtlRepo.findByTransactionPoid(transactionPoid);

		DayCloseDto dto = mapper.mapToDto(hdr);
		dto.setDenominations(mapper.mapDtlListToDto(details));

		return dto;
	}

	@Override
	public DayCloseDto createDayClose(DayCloseDto dto, Long groupPoid, Long companyPoid, Long userPoid) {

		DayCloseHdrDto header = dto.getHeader();

		if (header.getTransactionDate() != null
				&& hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoid(header.getTransactionDate(), groupPoid,
						companyPoid) > 0) {

			throw new ValidationException("Transaction date already closed: " + header.getTransactionDate());
		}

		validateAmounts(dto);

		ArShDayEndCloseHdr hdr = new ArShDayEndCloseHdr();
		mapper.mapCreateDTOToEntity(header, hdr, groupPoid, companyPoid);

		hdr = hdrRepo.save(hdr);

		saveDenominations(hdr.getTransactionPoid(), dto.getDenominations());

		callProcGlChoIntoChqMainShip(hdr.getTransactionPoid(), hdr.getTransactionDate(), UserContext.getDocumentId(),
				hdr.getDocRef(), groupPoid, companyPoid, userPoid);

		return getDayClose(hdr.getTransactionPoid(), groupPoid, companyPoid);
	}

	@Override
	public DayCloseSummaryProjection getNewDayCloseData(Long groupPoid, Long companyPoid, String transactionDate) {

		return receiptHdrRepository.fetchNewDayCloseSummary(groupPoid, companyPoid, transactionDate)
				.orElseThrow(() -> new ResourceNotFoundException("Day Close", "transactionDate", transactionDate));
	}

	@Override
	public List<Map<String, Object>> getDenominations(String currencyCode) {

		return denomRepo.findByCurrencyCodeOrderBySeqNo(currencyCode).stream().map(d -> {
			Map<String, Object> map = new HashMap<>();
			map.put("denomination", new BigDecimal(d.getCurrencyAmount()));
			map.put("currencyType", d.getCurrencyType());
			return map;
		}).toList();
	}

	@Override
	public DayCloseDto updateDayClose(DayCloseDto request, Long transactionPoid, Long groupPoid, Long companyPoid,
			Long userPoid) {

		validateAmounts(request);

		ArShDayEndCloseHdr hdr = new ArShDayEndCloseHdr();
		hdr.setTransactionPoid(transactionPoid);

		mapper.mapCreateDTOToEntity(request.getHeader(), hdr, groupPoid, companyPoid);
		hdrRepo.save(hdr);

		saveDenominations(transactionPoid, request.getDenominations());

		return getDayClose(transactionPoid, groupPoid, companyPoid);
	}

	@Override
	public Map<String, Object> searchDayClose(String docId, FilterRequestDto request, Pageable pageable) {

		String operator = documentService.resolveOperator(request);
		String isDeleted = documentService.resolveIsDeleted(request);
		List<FilterDto> filters = documentService.resolveFilters(request);

		RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted, "LOCATION_CODE",
				"TRANSACTION_POID");

		Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

		return PaginationUtil.wrapPage(page, raw.displayFields());
	}

	private void saveDenominations(Long transactionPoid, List<DayCloseDenominationDto> details) {

		if (details == null || details.isEmpty()) {
			return;
		}

		Long maxDetRowId = dtlRepo.getMaxDetRowId(transactionPoid);

		for (DayCloseDenominationDto dto : details) {
			ArShDayEndCloseDtl entity = mapper.mapDtlFromDto(dto, transactionPoid, null);

			if (entity.getDetRowId() == null) {
				entity.setDetRowId(++maxDetRowId);
			}
			dtlRepo.save(entity);
		}
	}

	private String callProcGlChoIntoChqMainShip(Long transactionPoid, LocalDate transactionDate, String docId,
			String docRef, Long groupPoid, Long companyPoid, Long userPoid) {

		SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
				.withProcedureName("PROC_GL_CHO_INTO_CHQ_MAIN_SHIP_TEST_1");

		Map<String, Object> inParams = Map.of("P_CHO_ID", transactionPoid, "P_CHO_DT", transactionDate, "P_DOC_ID",
				docId, "P_DOC_REF", docRef, "P_LOGIN_GROUP_POID", groupPoid, "P_LOGIN_COMPANY_POID", companyPoid,
				"P_LOGIN_USER_POID", userPoid);

		Map<String, Object> out = call.execute(inParams);
		return (String) out.get("P_STATUS");
	}

	private void validateAmounts(DayCloseDto request) {

		DayCloseHdrDto header = request.getHeader();
		List<DayCloseDenominationDto> details = request.getDenominations();

		BigDecimal cash = header.getCashAmount();
		BigDecimal cheque = header.getChequeAmount();
		BigDecimal total = header.getTotalAmount();

		if (cash != null && cheque != null && total != null && cash.add(cheque).compareTo(total) != 0) {

			throw new ValidationException("Receipt Total and Cheque, Cash total amount not match.");
		}

		if (details == null || details.isEmpty()) {
			return;
		}

		BigDecimal denomTotal = details.stream().map(this::calculateDenominationAmount).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		if (cash != null && denomTotal.compareTo(cash) != 0) {
			throw new ValidationException("Receipt total and Denomination total amount not match.");
		}
	}

	private BigDecimal calculateDenominationAmount(DayCloseDenominationDto dto) {

		if (dto == null) {
			return BigDecimal.ZERO;
		}

		if (dto.getCashAmount() != null) {
			return dto.getCashAmount();
		}

		if (dto.getDenomination() != null && dto.getNoOfTran() != null) {
			return dto.getDenomination().multiply(BigDecimal.valueOf(dto.getNoOfTran()));
		}

		return BigDecimal.ZERO;
	}
}
