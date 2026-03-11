package com.asg.shipping.receipts.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.service.LoggingService;

import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.receipts.dto.*;
import com.asg.shipping.receipts.entity.ArShReceiptChargesDtl;
import com.asg.shipping.receipts.entity.ArShReceiptContainerDtl;
import com.asg.shipping.receipts.entity.ArShReceiptHdr;
import com.asg.shipping.receipts.entity.ArShReceiptPymtDetails;
import com.asg.shipping.receipts.entity.TransactionDtlId;
import com.asg.shipping.receipts.enums.ButtonType;
import com.asg.shipping.receipts.repository.*;
import com.asg.shipping.receipts.service.ReceiptsService;
import com.asg.shipping.receipts.util.ReceiptsMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
	private final ShipReceiptProcRepository procRepository;
	private final ReceiptAutoPopulateRepository autoPopulateRepository;
	private final PrintService printService;
	private final DataSource dataSource;
	private final DocumentDeleteService documentDeleteService;
	private final LoggingService loggingService;

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

		LocalDate transactionDate = createDto.getTransactionDate() != null 
				? createDto.getTransactionDate()
				: com.asg.common.lib.utility.DateUtil.getCurrentDateInUserTimeZone();


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

		loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), hdr.getTransactionPoid().toString());

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

		ArShReceiptHdr oldReceipt = new ArShReceiptHdr();
		BeanUtils.copyProperties(existingReceipt, oldReceipt);

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

		loggingService.logChanges(oldReceipt, updated, ArShReceiptHdr.class, UserContext.getDocumentId(), transactionPoid.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

		procRepository.afterSave(updated.getGroupPoid(), updated.getCompanyPoid(), transactionPoid, 0L, "UPDATE", null);

		procRepository.doCntPrintAfter(updated.getGroupPoid(), updated.getCompanyPoid(), transactionPoid, 0L, "ARSHRCPTPRINTUPDATE", null);

		procRepository.receiptValidate(transactionPoid, updated.getCompanyPoid(), null);

		procRepository.glLedgerPostShRcpInv(updated.getGroupPoid(), updated.getCompanyPoid(), 1L, "300-103", transactionPoid, updateDto.getDocRef());

		return getReceipt(transactionPoid);
	}

	@Override
	public void deleteReceipt(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
		log.info("Deleting receipt with id: {}", transactionPoid);

		ArShReceiptHdr hdr = getReceiptHdr(transactionPoid);

		LocalDate transactionDate = null;
		if (hdr.getTransactionDate() != null) {
			transactionDate = LocalDate.from(hdr.getTransactionDate());
		}

		documentDeleteService.deleteDocument(
				transactionPoid,
				"AR_SH_RECEIPT_HDR",
				"TRANSACTION_POID",
				deleteReasonDto,
				transactionDate
		);

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
				LocalDate.from(requestDto.getFromDate()),
				LocalDate.from(requestDto.getToDate()),
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

	@Override
	public byte[] receiptAndInvoicePrint(Long transactionPoid, Long blPoid, ButtonType buttonType) throws Exception {
		Map<String, Object> params = printService.buildBaseParams(transactionPoid, "300-103");
		params.put("DOC_BL_POID",blPoid);
		JasperReport mainReport;
		if (ButtonType.Invoice.equals(buttonType)){
			 mainReport = printService.load("Shipping/SH/SH_INVOICE_IMP_EXP.jrxml");

        }else {
			mainReport = printService.load("Shipping/SH/SH_ALL_BILL_RECEIPT.jrxml");
        }
        return printService.fillReportToPdf(mainReport,params,dataSource);

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
				String logDetail = String.format("Row Created on Receipt Container Detail with detRowId: %s", entity.getId().getDetRowId());
				loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
			}
		}

		if (createDto.getCharges() != null && !createDto.getCharges().isEmpty()) {
			long maxDetRowId = chargesRepository.getMaxDetRowId(transactionPoid);
			for (var dto : createDto.getCharges()) {
				ArShReceiptChargesDtl entity = mapper.mapChargesDtoToEntity(dto, transactionPoid, ++maxDetRowId);
				chargesRepository.save(entity);
				String logDetail = String.format("Row Created on Receipt Charges Detail with detRowId: %s", entity.getId().getDetRowId());
				loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
			}
		}

		if (createDto.getPaymentDetail() != null && !createDto.getPaymentDetail().isEmpty()) {
			long maxDetRowId = paymentRepository.getMaxDetRowId(transactionPoid);
			for (var dto : createDto.getPaymentDetail()) {
				ArShReceiptPymtDetails entity = mapper.mapPaymentDtoToEntity(dto, transactionPoid, ++maxDetRowId);
				paymentRepository.save(entity);
				String logDetail = String.format("Row Created on Receipt Payment Detail with detRowId: %s", entity.getId().getDetRowId());
				loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
			}
		}
	}

	private void updateDetailRecords(Long transactionPoid, ReceiptsUpdateDto updateDto) {
		if (updateDto.getContainer() != null && !updateDto.getContainer().isEmpty()) {
			List<ArShReceiptContainerDtl> toUpdate = new ArrayList<>();
			List<LogRequestDto<ArShReceiptContainerDtl>> logRequests = new ArrayList<>();

			for (ReceiptContainerDto dto : updateDto.getContainer()) {
				String action = resolveAction(dto.getActionType());
				switch (action) {
					case ACTION_NOCHANGES -> {
					}
					case ACTION_ISDELETED -> {
						if (dto.getDetRowId() != null) {
							containerRepository.deleteById(new TransactionDtlId(transactionPoid, dto.getDetRowId()));
							String logDetail = String.format("Row Deleted on Receipt Container Detail with detRowId: %s", dto.getDetRowId());
							loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
						}
					}
					case ACTION_ISCREATED -> {
						long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : containerRepository.getMaxDetRowId(transactionPoid) + 1;
						ArShReceiptContainerDtl entity = mapper.mapContainerDtoToEntity(dto, transactionPoid, detRowId);
						containerRepository.save(entity);
						String logDetail = String.format("Row Created on Receipt Container Detail with detRowId: %s", entity.getId().getDetRowId());
						loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
					}
					case ACTION_ISUPDATED -> {
						ArShReceiptContainerDtl entity = containerRepository.findById(new TransactionDtlId(transactionPoid, dto.getDetRowId()))
								.orElseThrow(() -> new ResourceNotFoundException("Container Detail", "detRowId", dto.getDetRowId()));

						ArShReceiptContainerDtl oldEntity = new ArShReceiptContainerDtl();
						BeanUtils.copyProperties(entity, oldEntity);
						mapper.updateContainerEntity(dto, entity);
						toUpdate.add(entity);
						String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, dto.getDetRowId());
						logRequests.add(new LogRequestDto<>(oldEntity, entity, ArShReceiptContainerDtl.class, UserContext.getDocumentId(), transactionPoid.toString(), logDetail));

					}

				}
				if (!toUpdate.isEmpty()) {
					containerRepository.saveAll(toUpdate);
					if (!logRequests.isEmpty()) {
						loggingService.createLogBatch(logRequests);
					}
				}
			}
		}

		if (updateDto.getCharges() != null && !updateDto.getCharges().isEmpty()) {

			List<ArShReceiptChargesDtl> toUpdate = new ArrayList<>();
			List<LogRequestDto<ArShReceiptChargesDtl>> logRequests = new ArrayList<>();
			for (ReceiptCharges dto : updateDto.getCharges()) {
				String action = resolveAction(dto.getActionType());
				switch (action) {
					case ACTION_NOCHANGES -> {
					}
					case ACTION_ISDELETED -> {
						if (dto.getDetRowId() != null) {
							chargesRepository.deleteById(new TransactionDtlId(transactionPoid, dto.getDetRowId()));
							String logDetail = String.format("Row Deleted on Receipt Charges Detail with detRowId: %s", dto.getDetRowId());
							loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
						}
					}
					case ACTION_ISCREATED -> {
						long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : chargesRepository.getMaxDetRowId(transactionPoid) + 1;
						ArShReceiptChargesDtl entity = mapper.mapChargesDtoToEntity(dto, transactionPoid, detRowId);
						chargesRepository.save(entity);
						String logDetail = String.format("Row Created on Receipt Charges Detail with detRowId: %s", entity.getId().getDetRowId());
						loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
					}
					case ACTION_ISUPDATED -> {
						ArShReceiptChargesDtl entity = chargesRepository.findById(new TransactionDtlId(transactionPoid, dto.getDetRowId()))
								.orElseThrow(() -> new ResourceNotFoundException("Charges Detail", "detRowId", dto.getDetRowId()));
						ArShReceiptChargesDtl oldEntity = new ArShReceiptChargesDtl();
						BeanUtils.copyProperties(entity, oldEntity);
						mapper.updateChargesEntity(dto, entity);;
						toUpdate.add(entity);

						String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, dto.getDetRowId());
						logRequests.add(new LogRequestDto<>(oldEntity, entity, ArShReceiptChargesDtl.class, UserContext.getDocumentId(), transactionPoid.toString(), logDetail));
					}
				}
			}

			if (!toUpdate.isEmpty()) {
				chargesRepository.saveAll(toUpdate);
				if (!logRequests.isEmpty()) {
					loggingService.createLogBatch(logRequests);
				}
			}
		}

		if (updateDto.getPaymentDetail() != null && !updateDto.getPaymentDetail().isEmpty()) {
			List<ArShReceiptPymtDetails> toUpdate = new ArrayList<>();
			List<LogRequestDto<ArShReceiptPymtDetails>> logRequests = new ArrayList<>();
			for (ReceiptPaymentDetailDto dto : updateDto.getPaymentDetail()) {
				String action = resolveAction(dto.getActionType());
				switch (action) {
					case ACTION_NOCHANGES -> {
					}
					case ACTION_ISDELETED -> {
						if (dto.getDetRowId() != null) {
							paymentRepository.deleteById(new TransactionDtlId(transactionPoid, dto.getDetRowId()));
							String logDetail = String.format("Row Deleted on Receipt Payment Detail with detRowId: %s", dto.getDetRowId());
							loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
						}
					}
					case ACTION_ISCREATED -> {
						long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : paymentRepository.getMaxDetRowId(transactionPoid) + 1;
						ArShReceiptPymtDetails entity = mapper.mapPaymentDtoToEntity(dto, transactionPoid, detRowId);
						paymentRepository.save(entity);
						String logDetail = String.format("Row Created on Receipt Payment Detail with detRowId: %s", entity.getId().getDetRowId());
						loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
					}
					case ACTION_ISUPDATED -> {
						ArShReceiptPymtDetails entity = paymentRepository.findById(new TransactionDtlId(transactionPoid, dto.getDetRowId()))
								.orElseThrow(() -> new ResourceNotFoundException("Payment Detail", "detRowId", dto.getDetRowId()));
						ArShReceiptPymtDetails oldEntity = new ArShReceiptPymtDetails();
						BeanUtils.copyProperties(entity, oldEntity);
						mapper.updatePaymentEntity(dto, entity);
						toUpdate.add(entity);

						String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, dto.getDetRowId());
						logRequests.add(new LogRequestDto<>(oldEntity, entity, ArShReceiptPymtDetails.class, UserContext.getDocumentId(), transactionPoid.toString(), logDetail));
					}
				}

			}

			if (!toUpdate.isEmpty()) {
				paymentRepository.saveAll(toUpdate);
				if (!logRequests.isEmpty()) {
					loggingService.createLogBatch(logRequests);
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
