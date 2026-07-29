package com.asg.shipping.receipts.service.impl;

import com.asg.common.lib.dto.*;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ReceiptsServiceImpl implements ReceiptsService {

	private static final String ACTION_NOCHANGES = "NOCHANGES";
	private static final String ACTION_ISCREATED = "ISCREATED";
	private static final String ACTION_ISUPDATED = "ISUPDATED";
	private static final String ACTION_ISDELETED = "ISDELETED";
	private static final String FILTER_TRANSACTION_DATE = "TRANSACTION_DATE";

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
	private final com.asg.common.lib.service.LovDataService lovService;

	@PersistenceContext
	private final EntityManager entityManager;

	@Override
	public ReceiptsBlDetailsDto getReceipt(Long transactionPoid) {
		log.info("Getting receipt with id: {}", transactionPoid);

		ArShReceiptHdr hdr = getReceiptHdr(transactionPoid);
		List<ArShReceiptContainerDtl> containers = containerRepository.findByIdTransactionPoid(transactionPoid);
		List<ArShReceiptChargesDtl> charges = chargesRepository.findByIdTransactionPoid(transactionPoid);
		List<ArShReceiptPymtDetails> payments = paymentRepository.findByIdTransactionPoid(transactionPoid);

		ReceiptsBlDetailsDto dto = mapper.mapBlDetailsEntityToDto(hdr, containers, charges, payments);

		// --- Collect all poids per LOV name ---
		List<ReceiptContainerDto> cntDtos = dto.getContainer() != null ? dto.getContainer() : List.of();
		List<ReceiptCharges> chgDtos = dto.getCharges() != null ? dto.getCharges() : List.of();
		List<ReceiptPaymentDetailDto> pymtDtos = dto.getPaymentDetail() != null ? dto.getPaymentDetail() : List.of();

		List<Long> allBlPoids = Stream.concat(
				Stream.of(dto.getBlPoid()),
				Stream.concat(
						cntDtos.stream().map(ReceiptContainerDto::getBlPoid),
						chgDtos.stream().map(ReceiptCharges::getBlPoid)
				)
		).filter(Objects::nonNull).distinct().collect(Collectors.toList());

		List<Long> allChargePoids = chgDtos.stream().map(ReceiptCharges::getChargePoid)
				.filter(Objects::nonNull).distinct().collect(Collectors.toList());

		List<Long> allTaxPoids = Stream.concat(
				cntDtos.stream().map(ReceiptContainerDto::getCntTaxPoid),
				chgDtos.stream().map(ReceiptCharges::getTaxPoid)
		).filter(Objects::nonNull).distinct().collect(Collectors.toList());

		List<Long> companyPoids = dto.getCompanyPoid() != null ? List.of(dto.getCompanyPoid()) : List.of();
		List<Long> printCustomerPoids = dto.getPrintDoCustomerPoid() != null ? List.of(dto.getPrintDoCustomerPoid()) : List.of();
		List<Long> chequeCompanyPoids = dto.getChequeCompany() != null ? List.of(dto.getChequeCompany()) : List.of();

		List<Long> bankPoids = pymtDtos.stream().map(ReceiptPaymentDetailDto::getBankPoid)
				.filter(Objects::nonNull).distinct().collect(Collectors.toList());
		List<Long> ttBankPoids = pymtDtos.stream().map(ReceiptPaymentDetailDto::getTtBankPoid)
				.filter(Objects::nonNull).distinct().collect(Collectors.toList());

		CompletableFuture<Map<Long, LovGetListDto>> blFuture =
				CompletableFuture.supplyAsync(() ->
						lovService.getDetailsByPoidsAndLovName(allBlPoids, "IMPORTBLNUMBER")
				);

		CompletableFuture<Map<Long, LovGetListDto>> chargeFuture =
				CompletableFuture.supplyAsync(() ->
						lovService.getDetailsByPoidsAndLovName(allChargePoids, "CHARGE_MASTER")
				);

		CompletableFuture<Map<Long, LovGetListDto>> taxFuture =
				CompletableFuture.supplyAsync(() ->
						lovService.getDetailsByPoidsAndLovName(allTaxPoids, "TAX_MASTER")
				);

		CompletableFuture<Map<Long, LovGetListDto>> companyFuture =
				CompletableFuture.supplyAsync(() ->
						lovService.getDetailsByPoidsAndLovName(companyPoids, "COMPANY")
				);

		CompletableFuture<Map<Long, LovGetListDto>> printCustomerFuture =
				CompletableFuture.supplyAsync(() ->
						lovService.getDetailsByPoidsAndLovName(printCustomerPoids, "IMPORT_RECEIPT_CUSTOMER_PRINT")
				);

		CompletableFuture<Map<Long, LovGetListDto>> chequeCompanyFuture =
				CompletableFuture.supplyAsync(() ->
						lovService.getDetailsByPoidsAndLovName(chequeCompanyPoids, "SHIP_DIVISION_PRINT")
				);

		CompletableFuture<Map<Long, LovGetListDto>> bankFuture =
				CompletableFuture.supplyAsync(() ->
						lovService.getDetailsByPoidsAndLovName(bankPoids, "ARCUSTBANKRCPT")
				);

		CompletableFuture<Map<Long, LovGetListDto>> ttBankFuture =
				CompletableFuture.supplyAsync(() ->
						lovService.getDetailsByPoidsAndLovName(ttBankPoids, "SHIP_REC_BANK_MASTER_ALL_COMPANY")
				);

		try {
			CompletableFuture.allOf(
					blFuture,
					chargeFuture,
					taxFuture,
					companyFuture,
					printCustomerFuture,
					chequeCompanyFuture,
					bankFuture,
					ttBankFuture
			).join();
		} catch (CompletionException e) {
			Throwable cause = e.getCause();
			throw (cause instanceof RuntimeException) ? (RuntimeException) cause : new RuntimeException(cause);
		}

		Map<Long, LovGetListDto> blMap = blFuture.join();
		Map<Long, LovGetListDto> chargeMap = chargeFuture.join();
		Map<Long, LovGetListDto> taxMap = taxFuture.join();
		Map<Long, LovGetListDto> companyMap = companyFuture.join();
		Map<Long, LovGetListDto> printCustomerMap = printCustomerFuture.join();
		Map<Long, LovGetListDto> chequeCompanyMap = chequeCompanyFuture.join();
		Map<Long, LovGetListDto> bankMap = bankFuture.join();
		Map<Long, LovGetListDto> ttBankMap = ttBankFuture.join();

		// --- Apply from maps ---
		dto.setBlDet(blMap.get(dto.getBlPoid()));
		dto.setCompanyDet(companyMap.get(dto.getCompanyPoid()));
		dto.setPrintDoCustomerDet(printCustomerMap.get(dto.getPrintDoCustomerPoid()));
		dto.setChequeCompanyDet(chequeCompanyMap.get(dto.getChequeCompany()));

		cntDtos.forEach(c -> {
			c.setBlDet(blMap.get(c.getBlPoid()));
			c.setTaxDet(taxMap.get(c.getCntTaxPoid()));
		});

		chgDtos.forEach(c -> {
			c.setBlDet(blMap.get(c.getBlPoid()));
			c.setChargeDet(chargeMap.get(c.getChargePoid()));
			c.setTaxDet(taxMap.get(c.getTaxPoid()));
		});

		pymtDtos.forEach(p -> {
			p.setBankDet(bankMap.get(p.getBankPoid()));
			p.setTtBankDet(ttBankMap.get(p.getTtBankPoid()));
		});

		return dto;
	}

	@Override
	public ReceiptSaveResponseDto createReceipt(ReceiptsCreateDto createDto) {
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
		hdr.setPrintStatus("N");
		hdr.setRcptAmount(createDto.getAmount() != null ? BigDecimal.valueOf(createDto.getAmount()) : BigDecimal.ZERO);
		hdr = hdrRepository.saveAndFlush(hdr);
		log.info("Receipt created with id: {}", hdr.getTransactionPoid());

		saveDetailRecords(hdr.getTransactionPoid(), createDto);

		entityManager.refresh(hdr);
		loggingService.createLogSummaryEntry(UserContext.getDocumentId(), hdr.getTransactionPoid().toString(), String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), hdr.getDocRef()));

		// Call post-save procedure
		procRepository.afterSave(hdr.getGroupPoid(), hdr.getCompanyPoid(), hdr.getTransactionPoid(), 0L, "INSERT", null);

		// Call demurrage form printing procedure
		procRepository.doCntPrintAfter(hdr.getGroupPoid(), hdr.getCompanyPoid(), hdr.getTransactionPoid(), 0L, "ARSHRCPTPRINTUPDATE", null);

		// Call receipt validation procedure
		procRepository.receiptValidate(hdr.getTransactionPoid(), hdr.getCompanyPoid(), null);

		// Call GL ledger posting and invoice creation procedure
		String glStatus = procRepository.glLedgerPostShRcpInv(hdr.getGroupPoid(), hdr.getCompanyPoid(), 1L, "300-102", hdr.getTransactionPoid(), createDto.getDocRef());
        if (glStatus != null && glStatus.toUpperCase().contains("ERROR")) {
            throw new ValidationException(glStatus);
        }

		return ReceiptSaveResponseDto.builder()
				.docRef(hdr.getDocRef())
				.transactionPoid(hdr.getTransactionPoid())
				.message("Receipt Created Successfully")
				.build();
	}

	@Override
	public ReceiptSaveResponseDto updateReceipt(Long transactionPoid, ReceiptsUpdateDto updateDto) {
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
						.amount(updateDto.getAmount())
						.build()
		);
		updated.setTransactionPoid(transactionPoid);
		updated.setPrintStatus("N");
		updated.setRcptAmount(updateDto.getAmount() != null ? BigDecimal.valueOf(updateDto.getAmount()) : BigDecimal.ZERO);
		hdrRepository.save(updated);

		updateDetailRecords(transactionPoid, updateDto);

		loggingService.logChanges(oldReceipt, updated, ArShReceiptHdr.class, UserContext.getDocumentId(), transactionPoid.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

		procRepository.afterSave(updated.getGroupPoid(), updated.getCompanyPoid(), transactionPoid, 0L, "UPDATE", null);

		procRepository.doCntPrintAfter(updated.getGroupPoid(), updated.getCompanyPoid(), transactionPoid, 0L, "ARSHRCPTPRINTUPDATE", null);

		procRepository.receiptValidate(transactionPoid, updated.getCompanyPoid(), null);

		String glStatus = procRepository.glLedgerPostShRcpInv(updated.getGroupPoid(), updated.getCompanyPoid(), 1L, "300-102", transactionPoid, updateDto.getDocRef());
        if (glStatus != null && glStatus.toUpperCase().contains("ERROR")) {
            throw new ValidationException(glStatus);
        }

		return ReceiptSaveResponseDto.builder()
				.docRef(updated.getDocRef())
				.transactionPoid(transactionPoid)
				.message("Receipt Updated Successfully")
				.build();
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
	public Map<String, Object> list(FilterRequestDto filters, Pageable pageable, LocalDate startDate, LocalDate endDate) {
		String operator = documentSearchService.resolveOperator(filters);
		String isDeleted = documentSearchService.resolveIsDeleted(filters);
		List<FilterDto> filterList = documentSearchService.resolveDateFilters(filters, FILTER_TRANSACTION_DATE, startDate, endDate);

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
		// Step 1: Fetch BL header, containers, and charges on the main thread
		// (EntityManager is not thread-safe; must stay on the transactional thread)
		ReceiptBlAutoPopulateDto blAutoPopulateDto = procRepository.autoPopulateFields(blPoid);
		if (blAutoPopulateDto == null) {
			throw new ResourceNotFoundException("BL", "transactionPoid", blPoid);
		}
		List<ReceiptAutoPopulateContainerDto> containers = autoPopulateRepository.findAvailableContainersForBl(blPoid, transactionPoid);
		List<ReceiptAutoPopulateChargeDto> charges = autoPopulateRepository.findAvailableChargesForBl(blPoid, transactionPoid);

		// Step 2: Collect all poids per LOV name for batch fetching
		List<Long> allBlPoids = Stream.concat(
				Stream.of(blAutoPopulateDto.getBlPoid()),
				Stream.concat(
						containers.stream().map(ReceiptAutoPopulateContainerDto::getBlPoid),
						charges.stream().map(ReceiptAutoPopulateChargeDto::getBlPoid)
				)
		).filter(Objects::nonNull).distinct().collect(Collectors.toList());

		List<Long> companyPoids = Stream.of(blAutoPopulateDto.getCompanyPoid())
				.filter(Objects::nonNull).collect(Collectors.toList());

		List<Long> printCustomerPoids = Stream.of(blAutoPopulateDto.getPrintCustomerPoid())
				.filter(Objects::nonNull).map(BigDecimal::longValue).collect(Collectors.toList());

		List<Long> chequeCompanyPoids = Stream.of(blAutoPopulateDto.getChequeCompanyPoid())
				.filter(Objects::nonNull).map(BigDecimal::longValue).collect(Collectors.toList());

		List<Long> chargePoids = charges.stream().map(ReceiptAutoPopulateChargeDto::getChargePoid)
				.filter(Objects::nonNull).distinct().collect(Collectors.toList());

		List<Long> taxPoids = charges.stream().map(ReceiptAutoPopulateChargeDto::getTaxPoid)
				.filter(Objects::nonNull).distinct().collect(Collectors.toList());

		// Step 3: Capture UserContext (ThreadLocal) on the calling thread before spawning async threads
		com.asg.common.lib.security.model.CustomAuthDetails authDetails = UserContext.getCurrentUser();

		// Step 4: Batch fetch all LOV maps in parallel using JdbcTemplate (thread-safe)
		// Each async task propagates the captured auth context into its thread
		CompletableFuture<Map<Long, LovGetListDto>> blLovFuture =
				CompletableFuture.supplyAsync(() -> { UserContext.setCurrentUser(authDetails); return lovService.getDetailsByPoidsAndLovName(allBlPoids, "IMPORTBLNUMBER"); });
		CompletableFuture<Map<Long, LovGetListDto>> companyLovFuture =
				CompletableFuture.supplyAsync(() -> { UserContext.setCurrentUser(authDetails); return lovService.getDetailsByPoidsAndLovName(companyPoids, "COMPANY"); });
		CompletableFuture<Map<Long, LovGetListDto>> printCustomerLovFuture =
				CompletableFuture.supplyAsync(() -> { UserContext.setCurrentUser(authDetails); return lovService.getDetailsByPoidsAndLovName(printCustomerPoids, "IMPORT_RECEIPT_CUSTOMER_PRINT"); });
		CompletableFuture<Map<Long, LovGetListDto>> chequeCompanyLovFuture =
				CompletableFuture.supplyAsync(() -> { UserContext.setCurrentUser(authDetails); return lovService.getDetailsByPoidsAndLovName(chequeCompanyPoids, "SHIP_DIVISION_PRINT"); });
		CompletableFuture<Map<Long, LovGetListDto>> chargeLovFuture =
				CompletableFuture.supplyAsync(() -> { UserContext.setCurrentUser(authDetails); return lovService.getDetailsByPoidsAndLovName(chargePoids, "CHARGE_MASTER"); });
		CompletableFuture<Map<Long, LovGetListDto>> taxLovFuture =
				CompletableFuture.supplyAsync(() -> { UserContext.setCurrentUser(authDetails); return lovService.getDetailsByPoidsAndLovName(taxPoids, "TAX_MASTER"); });

		CompletableFuture.allOf(blLovFuture, companyLovFuture, printCustomerLovFuture, chequeCompanyLovFuture, chargeLovFuture, taxLovFuture).join();

		Map<Long, LovGetListDto> blLovMap = blLovFuture.join();
		Map<Long, LovGetListDto> companyLovMap = companyLovFuture.join();
		Map<Long, LovGetListDto> printCustomerLovMap = printCustomerLovFuture.join();
		Map<Long, LovGetListDto> chequeCompanyLovMap = chequeCompanyLovFuture.join();
		Map<Long, LovGetListDto> chargeLovMap = chargeLovFuture.join();
		Map<Long, LovGetListDto> taxLovMap = taxLovFuture.join();

		// Step 5: Enrich from maps — no per-item LOV calls
		blAutoPopulateDto.setBlDet(blLovMap.get(blAutoPopulateDto.getBlPoid()));
		blAutoPopulateDto.setCompanyDet(companyLovMap.get(blAutoPopulateDto.getCompanyPoid()));
		blAutoPopulateDto.setPrintCustomerDet(blAutoPopulateDto.getPrintCustomerPoid() != null ? printCustomerLovMap.get(blAutoPopulateDto.getPrintCustomerPoid().longValue()) : null);
		blAutoPopulateDto.setChequeCompanyDet(blAutoPopulateDto.getChequeCompanyPoid() != null ? chequeCompanyLovMap.get(blAutoPopulateDto.getChequeCompanyPoid().longValue()) : null);

		containers.forEach(c -> c.setBlDet(blLovMap.get(c.getBlPoid())));

		charges.forEach(charge -> {
			charge.setBlDet(blLovMap.get(charge.getBlPoid()));
			charge.setChargeDet(chargeLovMap.get(charge.getChargePoid()));
			charge.setTaxDet(taxLovMap.get(charge.getTaxPoid()));
		});

		return ReceiptAutoPopulateDto.builder()
				.blDetails(blAutoPopulateDto)
				.container(containers)
				.charges(charges)
				.build();
	}

	@Override
	public ReceiptCalculateDemurrageResponseDto calculateDemurrage(ReceiptCalculateDemurrageRequestDto requestDto) {
		log.info("Calculating demurrage for BL: {}, total containers: {}", requestDto.getBlPoid(), requestDto.getContainers().size());

		List<ReceiptCalculateDemurrageResponseDto.ContainerResult> containerResults = new ArrayList<>();
		BigDecimal totalDemAmount = BigDecimal.ZERO;
		Long companyPoid = UserContext.getCompanyPoid();

		// 1. Calculate individual container demurrage and summary sum
		for (ReceiptCalculateDemurrageRequestDto.ContainerRequest container : requestDto.getContainers()) {
			if (container.getToDate() != null && container.getFromDate() != null &&
					container.getToDate().isBefore(container.getFromDate())) {
				throw new ValidationException("To Date cannot be before From Date for container " + container.getContainerNo());
			}

			BigDecimal containerDemurrage = BigDecimal.ZERO;
			Long days = 0L;

			if (container.getToDate() != null) {
				// Legacy logic: fetch Arrival Date for the stored procedure
				LocalDate arrivalDate = autoPopulateRepository.findArrivalDate(requestDto.getBlPoid(), container.getContainerNo());
				BigDecimal linePoid = autoPopulateRepository.findLinePoidByBlPoid(requestDto.getBlPoid());

				containerDemurrage = procRepository.calculateDemurrageAmount(
						requestDto.getTransactionPoid(),
						requestDto.getBlPoid(),
						container.getContainerNo(),
						container.getEquipmentIsoType(),
						linePoid != null ? linePoid.longValue() : null,
						arrivalDate, 
						container.getToDate(),
						container.getExtraFreeDays()
				);

				if (container.getFromDate() != null) {
					days = java.time.temporal.ChronoUnit.DAYS.between(container.getFromDate(), container.getToDate()) + 1;
				}
			}

			totalDemAmount = totalDemAmount.add(containerDemurrage != null ? containerDemurrage : BigDecimal.ZERO);

			containerResults.add(ReceiptCalculateDemurrageResponseDto.ContainerResult.builder()
					.containerNo(container.getContainerNo())
					.demurrageAmount(containerDemurrage != null ? containerDemurrage : BigDecimal.ZERO)
					.demurrageDays(days != null ? days : 0L)
					.build());
		}

		// 2. Fetch Demurrage Tax and update container rows
		TaxConfig demTaxInfo = procRepository.getDemurrageTaxInfo(companyPoid);
		BigDecimal totalDemTaxAmount = BigDecimal.ZERO;
		if (demTaxInfo != null && "Y".equals(demTaxInfo.getTaxApplicable()) && demTaxInfo.getPercentage() != null) {
			totalDemTaxAmount = totalDemAmount.multiply(demTaxInfo.getPercentage()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
		}

		for (ReceiptCalculateDemurrageResponseDto.ContainerResult result : containerResults) {
			BigDecimal containerTax = BigDecimal.ZERO;
			if (demTaxInfo != null && demTaxInfo.getPercentage() != null) {
				containerTax = (result.getDemurrageAmount() != null) ?
						result.getDemurrageAmount().multiply(demTaxInfo.getPercentage()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
				result.setTaxPoid(demTaxInfo.getTaxPoid());
				result.setTaxPercentage(demTaxInfo.getPercentage());
			}
			result.setTaxAmount(containerTax);
		}

		List<ReceiptCalculateDemurrageResponseDto.ChargeDetail> charges = new ArrayList<>();
		
		charges.add(ReceiptCalculateDemurrageResponseDto.ChargeDetail.builder()
				.chargeType("SHDEMURRAGE")
				.chargePoid(demTaxInfo != null && demTaxInfo.getParameterValue() != null ? Long.parseLong(demTaxInfo.getParameterValue()) : null)
				.amount(totalDemAmount)
				.taxPoid(demTaxInfo != null ? demTaxInfo.getTaxPoid() : null)
				.taxPercentage(demTaxInfo != null ? demTaxInfo.getPercentage() : null)
				.taxAmount(totalDemTaxAmount)
				.build());

		List<ChargeDto> allCombinedCharges = procRepository.getCombinedCharges(requestDto.getBlPoid(), companyPoid);
		if (allCombinedCharges != null) {
			for (ChargeDto charge : allCombinedCharges) {
				// Standard charges logic using representative container context or per-BL logic
				ReceiptCalculateDemurrageResponseDto.ChargeDetail detail = processLateCharge(charge, requestDto.getContainers().size());
				if (detail.getAmount().compareTo(BigDecimal.ZERO) > 0) {
					charges.add(detail);
				}
			}
		}

		// 4. Final Grand Total and LOV enrichment
		BigDecimal grandTotal = totalDemAmount.add(totalDemTaxAmount);

		List<Long> demChargePoids = charges.stream().map(ReceiptCalculateDemurrageResponseDto.ChargeDetail::getChargePoid)
				.filter(Objects::nonNull).distinct().collect(Collectors.toList());
		List<Long> demTaxPoids = Stream.concat(
				charges.stream().map(ReceiptCalculateDemurrageResponseDto.ChargeDetail::getTaxPoid),
				containerResults.stream().map(ReceiptCalculateDemurrageResponseDto.ContainerResult::getTaxPoid)
		).filter(Objects::nonNull).distinct().collect(Collectors.toList());

		Map<Long, LovGetListDto> demChargeMap = lovService.getDetailsByPoidsAndLovName(demChargePoids, "CHARGE_MASTER");
		Map<Long, LovGetListDto> demTaxMap = lovService.getDetailsByPoidsAndLovName(demTaxPoids, "TAX_MASTER");

		charges.forEach(detail -> {
			detail.setChargeDet(demChargeMap.get(detail.getChargePoid()));
			detail.setTaxDet(demTaxMap.get(detail.getTaxPoid()));
		});

		containerResults.forEach(row -> row.setTaxDet(demTaxMap.get(row.getTaxPoid())));

		for (int i = 1; i < charges.size(); i++) {
			grandTotal = grandTotal.add(charges.get(i).getAmount()).add(charges.get(i).getTaxAmount());
		}

		return ReceiptCalculateDemurrageResponseDto.builder()
				.containerResults(containerResults)
				.charges(charges)
				.totalAmount(grandTotal)
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
			mainReport = printService.load("Shipping/SH/SH_RECEIPT.jrxml");
        }
        return printService.fillReportToPdf(mainReport,params,dataSource);

    }


	private ReceiptCalculateDemurrageResponseDto.ChargeDetail processLateCharge(ChargeDto charge, int containerCount) {
		String chargeApplicable = charge.getChargeApplicable();
		BigDecimal amount = BigDecimal.ZERO;

		if ("PERBL".equals(chargeApplicable)) {
			amount = charge.getAmountOther();
		} else if ("PERQUENTITY".equals(chargeApplicable)) {
			// Using base 20 calculation as default sum if no specific mix is provided
			amount = charge.getAmount20().multiply(BigDecimal.valueOf(containerCount));
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
