package com.asg.shipping.receipts.service.impl;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.receipts.dto.*;
import com.asg.shipping.receipts.entity.ArShReceiptChargesDtl;
import com.asg.shipping.receipts.entity.ArShReceiptContainerDtl;
import com.asg.shipping.receipts.entity.ArShReceiptHdr;
import com.asg.shipping.receipts.entity.ArShReceiptPymtDetails;
import com.asg.shipping.receipts.entity.TransactionDtlId;
import com.asg.shipping.receipts.repository.*;
import com.asg.shipping.receipts.service.ReceiptsService;
import com.asg.shipping.receipts.util.ReceiptsMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ReceiptsServiceImpl implements ReceiptsService {

	private static final String ACTION_NOCHANGES = "NOCHANGES";
	private static final String ACTION_ISCREATED = "ISCREATED";
	private static final String ACTION_ISUPDATED = "ISUPDATED";
	private static final String ACTION_ISDELETED = "ISDELETED";

	private final DocumentSearchService documentSearchService;
	private final ReceiptHdrRepository hdrRepository;
	private final ArShReceiptContainerDtlRepository containerRepository;
	private final ArShReceiptChargesDtlRepository chargesRepository;
	private final ArShReceiptPymtDetailsRepository paymentRepository;
	private final ReceiptsMapper mapper;
	private final ShippingReceiptValidationService validationService;
	private final TransactionDateService transactionDateService;
	private final ShipReceiptProcRepository procRepository;
	private final ReceiptAutoPopulateRepository autoPopulateRepository;

	@Override
	public ReceiptsBlDetailsDto getReceipt(Long transactionPoid) {
		log.info("Getting receipt with id: {}", transactionPoid);

		ArShReceiptHdr hdr = getReceiptHdr(transactionPoid);

		List<ArShReceiptContainerDtl> containers = containerRepository.findByIdTransactionPoid(transactionPoid);
		List<ArShReceiptChargesDtl> charges = chargesRepository.findByIdTransactionPoid(transactionPoid);
		List<ArShReceiptPymtDetails> payments = paymentRepository.findByIdTransactionPoid(transactionPoid);

		return mapper.mapBlDetailsEntityToDto(hdr, containers, charges, payments);
	}

	@Override
	public ReceiptsBlDetailsDto createReceipt(ReceiptsCreateDto createDto) {
		log.info("Creating receipt with docRef: {}", createDto.getDocRef());

		LocalDate transactionDate = createDto.getTransactionDate() == null ? transactionDateService.calculateTransactionDate() : createDto.getTransactionDate();


		validationService.validateReceiptCreation(createDto);



		ArShReceiptHdr hdr = mapper.mapBlDetailsDtoToEntity(
				ReceiptsBlDetailsDto.builder()
						.docRef(createDto.getDocRef())
						.date(transactionDate)
						.blPoid(createDto.getBlPoid())
						.companyPoid(createDto.getCompanyPoid())
						.releaseType(createDto.getReleaseType())
						.originalReleaseType(createDto.getReleaseType())
						.printDoCustomerPoid(createDto.getPrintDoCustomerPoid())
						.chequeCompany(createDto.getChequeCompany())
						.cpr(createDto.getCpr())
						.name(createDto.getName())
						.contact(createDto.getContact())
						.paymentReference(createDto.getPaymentReference())
						.amount(createDto.getAmount())
						.remarks(createDto.getRemarks())
						.token(createDto.getToken())
						.build()
		);
		hdr = hdrRepository.save(hdr);
		log.info("Receipt created with id: {}", hdr.getTransactionPoid());

		saveDetailRecords(hdr.getTransactionPoid(), createDto);

		// Call post-save procedure
		procRepository.afterSave(hdr.getGroupPoid(), hdr.getCompanyPoid(), hdr.getTransactionPoid(), 0L, "INSERT", null);

		// Call demurrage form printing procedure
		procRepository.doCntPrintAfter(hdr.getGroupPoid(), hdr.getCompanyPoid(), hdr.getTransactionPoid(), 0L, "ARSHRCPTPRINTUPDATE", null);

		// Call receipt validation procedure
		procRepository.receiptValidate(hdr.getTransactionPoid(), hdr.getCompanyPoid(), null);

		// Call GL ledger posting and invoice creation procedure
		procRepository.glLedgerPostShRcpInv(hdr.getGroupPoid(), hdr.getCompanyPoid(), 1L, "300-103", hdr.getTransactionPoid(), createDto.getDocRef());

		return getReceipt(hdr.getTransactionPoid());
	}

	@Override
	public ReceiptsBlDetailsDto updateReceipt(Long transactionPoid, ReceiptsUpdateDto updateDto) {
		log.info("Updating receipt with id: {}", transactionPoid);

		ArShReceiptHdr existingReceipt = getReceiptHdr(transactionPoid);

		// Execute update validations
		validationService.validateReceiptUpdate(updateDto, existingReceipt);

		// Preserve original transaction date
		LocalDate originalTransactionDate = existingReceipt.getTransactionDate();

		ArShReceiptHdr updated = mapper.mapBlDetailsDtoToEntity(
				ReceiptsBlDetailsDto.builder()
						.docRef(updateDto.getDocRef())
						.date(originalTransactionDate)
						.blPoid(updateDto.getBlPoid())
						.companyPoid(updateDto.getCompanyPoid())
						.releaseType(updateDto.getReleaseType())
						.originalReleaseType(updateDto.getReleaseType())
						.printDoCustomerPoid(updateDto.getPrintDoCustomerPoid())
						.chequeCompany(updateDto.getChequeCompany())
						.cpr(updateDto.getCpr())
						.name(updateDto.getName())
						.contact(updateDto.getContact())
						.paymentReference(updateDto.getPaymentReference())
						.remarks(updateDto.getRemarks())
						.token(updateDto.getToken())
						.build()
		);
		updated.setTransactionPoid(transactionPoid);
		hdrRepository.save(updated);

		updateDetailRecords(transactionPoid, updateDto);

		procRepository.afterSave(updated.getGroupPoid(), updated.getCompanyPoid(), transactionPoid, 0L, "UPDATE", null);

		procRepository.doCntPrintAfter(updated.getGroupPoid(), updated.getCompanyPoid(), transactionPoid, 0L, "ARSHRCPTPRINTUPDATE", null);

		procRepository.receiptValidate(transactionPoid, updated.getCompanyPoid(), null);

		procRepository.glLedgerPostShRcpInv(updated.getGroupPoid(), updated.getCompanyPoid(), 1L, "300-103", transactionPoid, updateDto.getDocRef());

		return getReceipt(transactionPoid);
	}

	@Override
	public void deleteReceipt(Long transactionPoid) {
		log.info("Deleting receipt with id: {}", transactionPoid);

		ArShReceiptHdr hdr = getReceiptHdr(transactionPoid);

		hdr.setDeleted("Y");
		hdrRepository.save(hdr);
		log.info("Receipt deleted with id: {}", transactionPoid);
	}

	@Override
	public Map<String, Object> list(FilterRequestDto filters, Pageable pageable) {
		String operator = documentSearchService.resolveOperator(filters);
		String isDeleted = documentSearchService.resolveIsDeleted(filters);
		List<FilterDto> filterList = documentSearchService.resolveFilters(filters);

		RawSearchResult raw = documentSearchService.search(
				UserContext.getDocumentId(),
				filterList,
				operator,
				pageable,
				isDeleted,
				"DOC_REF",
				"TRANSACTION_POID"
		);

		Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
		return PaginationUtil.wrapPage(page, raw.displayFields());
	}

	@Override
	public ReceiptAutoPopulateDto autoPopulateFields(Long blPoid, Long transactionPoid) {
		ReceiptBlAutoPopulateDto blAutoPopulateDto = procRepository.autoPopulateFields(blPoid);
		List<ReceiptAutoPopulateContainerDto> containerAutoPopulateDto = autoPopulateRepository.findAvailableContainersForBl(blPoid, transactionPoid);

		List<ReceiptAutoPopulateChargeDto> chargeAutoPopulateDto = autoPopulateRepository.findAvailableChargesForBl(blPoid, transactionPoid);

		return ReceiptAutoPopulateDto.builder()
				.blDetails(blAutoPopulateDto)
				.container(containerAutoPopulateDto)
				.charges(chargeAutoPopulateDto)
				.build();
	}

	@Override
	public ReceiptCalculateDemurrageResponseDto calculateDemurrage(ReceiptCalculateDemurrageRequestDto requestDto) {
		log.info("Calculating demurrage for BL: {}, Container: {}", requestDto.getBlPoid(), requestDto.getContainerNo());

		if (requestDto.getToDate().isBefore(requestDto.getFromDate())) {
			throw new ValidationException("To Date cannot be before From Date");
		}

		BigDecimal linePoid = autoPopulateRepository.findLinePoidByBlPoid(requestDto.getBlPoid());
		BigDecimal demurrageAmount = procRepository.calculateDemurrageAmount(
				requestDto.getTransactionPoid(),
				requestDto.getBlPoid(),
				requestDto.getContainerNo(),
				requestDto.getContainerIsoType(),
				linePoid.longValue(),
				requestDto.getFromDate(),
				requestDto.getToDate(),
				requestDto.getExtraFreeDays()
		);

		BigDecimal demurrageTaxAmount = BigDecimal.ZERO;
		Long demurrageTaxPoid = null;
		BigDecimal demurrageTaxPercentage = null;

		TaxConfig taxConfig = procRepository.getDemurrageTaxInfo(UserContext.getCompanyPoid());
		if (taxConfig == null) {
			taxConfig = TaxConfig.builder()
					.taxApplicable("N")
					.percentage(BigDecimal.ZERO)
					.build();
		}
		if ("Y".equalsIgnoreCase(taxConfig.getTaxApplicable()) && demurrageAmount.compareTo(BigDecimal.ZERO) > 0) {
			demurrageTaxPoid = taxConfig.getTaxPoid();
			demurrageTaxPercentage = taxConfig.getPercentage();
			demurrageTaxAmount = demurrageAmount
					.multiply(demurrageTaxPercentage)
					.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
		}

		String containerSize = procRepository.getContainerSize(requestDto.getContainerIsoType());
		List<ReceiptCalculateDemurrageResponseDto.ChargeDetail> lateCollectionCharges = new ArrayList<>();
		List<ReceiptCalculateDemurrageResponseDto.ChargeDetail> revalidationCharges = new ArrayList<>();

		List<ChargeDto> allCharges = procRepository.getCombinedCharges(requestDto.getBlPoid(), UserContext.getCompanyPoid());
		if (allCharges == null) {
			allCharges = new ArrayList<>();
		}

		for (ChargeDto charge : allCharges) {
			ReceiptCalculateDemurrageResponseDto.ChargeDetail detail = processCharge(charge, containerSize);

			if (detail.getAmount().compareTo(BigDecimal.ZERO) > 0) {
				if (charge.getChargeTypeApplicable() != null && charge.getChargeTypeApplicable().contains("LATECOLLECTION")) {
					lateCollectionCharges.add(detail);
				} else if (charge.getChargeTypeApplicable() != null && charge.getChargeTypeApplicable().contains("REVALIDATE")) {
					revalidationCharges.add(detail);
				}
			}
		}

		BigDecimal totalAmount = demurrageAmount.add(demurrageTaxAmount);
		for (ReceiptCalculateDemurrageResponseDto.ChargeDetail charge : lateCollectionCharges) {
			totalAmount = totalAmount.add(charge.getAmount()).add(charge.getTaxAmount());
		}
		for (ReceiptCalculateDemurrageResponseDto.ChargeDetail charge : revalidationCharges) {
			totalAmount = totalAmount.add(charge.getAmount()).add(charge.getTaxAmount());
		}

		log.info("Demurrage calculated - BL: {}, Demurrage: {}, Tax: {}, Total: {}", 
			requestDto.getBlPoid(), demurrageAmount, demurrageTaxAmount, totalAmount);

		return ReceiptCalculateDemurrageResponseDto.builder()
				.demurrageAmount(demurrageAmount)
				.demurrageTaxAmount(demurrageTaxAmount)
				.demurrageTaxPoid(demurrageTaxPoid)
				.demurrageTaxPercentage(demurrageTaxPercentage)
				.lateCollectionCharges(lateCollectionCharges)
				.revalidationCharges(revalidationCharges)
				.totalAmount(totalAmount)
				.build();
	}

	private ReceiptCalculateDemurrageResponseDto.ChargeDetail processCharge(ChargeDto charge, String containerSize) {
		String chargeApplicable = charge.getChargeApplicable();
		BigDecimal amount = BigDecimal.ZERO;

		if ("PERBL".equals(chargeApplicable)) {
			amount = charge.getAmountOther();
		} else if ("PERQUENTITY".equals(chargeApplicable)) {
			amount = "20".equals(containerSize) ? charge.getAmount20() : charge.getAmount40();
		}

		BigDecimal taxAmount = BigDecimal.ZERO;
		if ("Y".equals(charge.getTaxApplicable()) && charge.getTaxPercentage() != null && amount.compareTo(BigDecimal.ZERO) > 0) {
			taxAmount = amount.multiply(charge.getTaxPercentage())
					.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
		}

		return ReceiptCalculateDemurrageResponseDto.ChargeDetail.builder()
				.chargeType(charge.getChargeTypeApplicable())
				.chargePoid(charge.getChargeCodePoid())
				.amount(amount)
				.taxPoid(charge.getTaxPoid())
				.taxPercentage(charge.getTaxPercentage())
				.taxAmount(taxAmount)
				.build();
	}

	private void saveDetailRecords(Long transactionPoid, ReceiptsCreateDto createDto) {
		if (createDto.getContainer() != null && !createDto.getContainer().isEmpty()) {
			long maxDetRowId = containerRepository.getMaxDetRowId(transactionPoid);
			for (var dto : createDto.getContainer()) {
				ArShReceiptContainerDtl entity = mapper.mapContainerDtoToEntity(dto, transactionPoid, ++maxDetRowId);
				containerRepository.save(entity);
			}
		}

		if (createDto.getCharges() != null && !createDto.getCharges().isEmpty()) {
			long maxDetRowId = chargesRepository.getMaxDetRowId(transactionPoid);
			for (var dto : createDto.getCharges()) {
				ArShReceiptChargesDtl entity = mapper.mapChargesDtoToEntity(dto, transactionPoid, ++maxDetRowId);
				chargesRepository.save(entity);
			}
		}

		if (createDto.getPaymentDetail() != null && !createDto.getPaymentDetail().isEmpty()) {
			long maxDetRowId = paymentRepository.getMaxDetRowId(transactionPoid);
			for (var dto : createDto.getPaymentDetail()) {
				ArShReceiptPymtDetails entity = mapper.mapPaymentDtoToEntity(dto, transactionPoid, ++maxDetRowId);
				paymentRepository.save(entity);
			}
		}
	}

	private void updateDetailRecords(Long transactionPoid, ReceiptsUpdateDto updateDto) {
		if (updateDto.getContainer() != null && !updateDto.getContainer().isEmpty()) {
			for (ReceiptContainerDto dto : updateDto.getContainer()) {
				String action = resolveAction(dto.getActionType());
				switch (action) {
					case ACTION_NOCHANGES -> {
					}
					case ACTION_ISDELETED -> {
						if (dto.getDetRowId() != null) {
							containerRepository.deleteById(new TransactionDtlId(transactionPoid, dto.getDetRowId()));
						}
					}
					case ACTION_ISCREATED -> {
						long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : containerRepository.getMaxDetRowId(transactionPoid) + 1;
						ArShReceiptContainerDtl entity = mapper.mapContainerDtoToEntity(dto, transactionPoid, detRowId);
						containerRepository.save(entity);
					}
					case ACTION_ISUPDATED -> {
						ArShReceiptContainerDtl entity = containerRepository.findById(new TransactionDtlId(transactionPoid, dto.getDetRowId()))
								.orElseThrow(() -> new ResourceNotFoundException("Container Detail", "detRowId", dto.getDetRowId()));
						mapper.updateContainerEntity(dto, entity);
						containerRepository.save(entity);
					}
				}
			}
		}

		if (updateDto.getCharges() != null && !updateDto.getCharges().isEmpty()) {
			for (ReceiptCharges dto : updateDto.getCharges()) {
				String action = resolveAction(dto.getActionType());
				switch (action) {
					case ACTION_NOCHANGES -> {
					}
					case ACTION_ISDELETED -> {
						if (dto.getDetRowId() != null) {
							chargesRepository.deleteById(new TransactionDtlId(transactionPoid, dto.getDetRowId()));
						}
					}
					case ACTION_ISCREATED -> {
						long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : chargesRepository.getMaxDetRowId(transactionPoid) + 1;
						ArShReceiptChargesDtl entity = mapper.mapChargesDtoToEntity(dto, transactionPoid, detRowId);
						chargesRepository.save(entity);
					}
					case ACTION_ISUPDATED -> {
						ArShReceiptChargesDtl entity = chargesRepository.findById(new TransactionDtlId(transactionPoid, dto.getDetRowId()))
								.orElseThrow(() -> new ResourceNotFoundException("Charges Detail", "detRowId", dto.getDetRowId()));
						mapper.updateChargesEntity(dto, entity);
						chargesRepository.save(entity);
					}
				}
			}
		}

		if (updateDto.getPaymentDetail() != null && !updateDto.getPaymentDetail().isEmpty()) {
			for (ReceiptPaymentDetailDto dto : updateDto.getPaymentDetail()) {
				String action = resolveAction(dto.getActionType());
				switch (action) {
					case ACTION_NOCHANGES -> {
					}
					case ACTION_ISDELETED -> {
						if (dto.getDetRowId() != null) {
							paymentRepository.deleteById(new TransactionDtlId(transactionPoid, dto.getDetRowId()));
						}
					}
					case ACTION_ISCREATED -> {
						long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : paymentRepository.getMaxDetRowId(transactionPoid) + 1;
						ArShReceiptPymtDetails entity = mapper.mapPaymentDtoToEntity(dto, transactionPoid, detRowId);
						paymentRepository.save(entity);
					}
					case ACTION_ISUPDATED -> {
						ArShReceiptPymtDetails entity = paymentRepository.findById(new TransactionDtlId(transactionPoid, dto.getDetRowId()))
								.orElseThrow(() -> new ResourceNotFoundException("Payment Detail", "detRowId", dto.getDetRowId()));
						mapper.updatePaymentEntity(dto, entity);
						paymentRepository.save(entity);
					}
				}
			}
		}
	}

	private String resolveAction(String rawAction) {
		String action = (rawAction == null || rawAction.trim().isEmpty()) ? ACTION_NOCHANGES : rawAction.trim().toUpperCase();
		return switch (action) {
			case "ISCREATED", "CREATED", "NEW" -> ACTION_ISCREATED;
			case "ISUPDATED", "UPDATED" -> ACTION_ISUPDATED;
			case "ISDELETED", "DELETED" -> ACTION_ISDELETED;
			default -> ACTION_NOCHANGES;
		};
	}

	private ArShReceiptHdr getReceiptHdr(Long transactionPoid) {
		return hdrRepository.findById(transactionPoid)
				.orElseThrow(() -> new ResourceNotFoundException("Receipt", "transactionPoid", transactionPoid.toString()));
	}

}
