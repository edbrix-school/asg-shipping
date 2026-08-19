package com.asg.shipping.demurrageenquiryblwise.service.impl;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.DateUtil;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.demurrageenquiryblwise.dto.ContainerDemurrageCalcDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageChargeConfigDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageEnquiryChargeDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageEnquiryContainerDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageEnquiryRequestDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageEnquiryResponseDto;
import com.asg.shipping.demurrageenquiryblwise.dto.ManifestChargeRowDto;
import com.asg.shipping.demurrageenquiryblwise.dto.PortChargeRowDto;
import com.asg.shipping.demurrageenquiryblwise.repository.DemurrageEnquiryBlWiseRepository;
import com.asg.shipping.demurrageenquiryblwise.service.DemurrageEnquiryBlWiseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemurrageEnquiryBlWiseServiceImpl implements DemurrageEnquiryBlWiseService {

	private static final String DOC_ID = "100-144";
	private static final String DEMURRAGE_CALC_REPORT = "Shipping/SH/LINE_DEMURRAGE_CALC.jrxml";
	private static final String DEMURRAGE_MASTER_SUBREPORT = "Shipping/SH/LINE_DEMURRAGE_MASTER.jrxml";
	private static final String DEMURRAGE_DTL_SUBREPORT = "Shipping/SH/LINE_DEMURRAGE_DTL.jrxml";
	/**
	 * LINE_DEMURRAGE_DTL parses the parameter with
	 * {@code to_date(SUBSTR(P_TILL_DATE,1,10),'RRRR-MM-DD')}, so it has to arrive as yyyy-MM-dd -
	 * the format the legacy screen produced from the ADF date binding.
	 */
	private static final DateTimeFormatter REPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	/** The screen prints the late collection date the way Oracle's DD-MON-YY does: 13-MAY-26. */
	private static final DateTimeFormatter REMARKS_DATE_FORMAT =
			DateTimeFormatter.ofPattern("dd-MMM-yy", java.util.Locale.ENGLISH);

	private static final String BL_LOV = "ALLBLNUMBER";
	private static final String CHARGE_LOV = "CHARGE_MASTER";
	private static final String TAX_LOV = "TAX_MASTER";

	private static final String CHARGE_TYPE_BL = "BLCHARGE";
	private static final String CHARGE_TYPE_DEMURRAGE = "SHDEMURRAGE";
	private static final String APPLICABLE_PER_BL = "PERBL";
	private static final String APPLICABLE_PER_QUANTITY = "PERQUENTITY";
	private static final String SIZE_20 = "20";

	private static final BigDecimal HUNDRED = new BigDecimal("100");
	private static final int AMOUNT_SCALE = 3;

	private final DemurrageEnquiryBlWiseRepository enquiryRepository;
	private final DocumentSearchService documentSearchService;
	private final LovDataService lovService;
	private final PrintService printService;
	private final DataSource dataSource;

	@Override
	public DemurrageEnquiryResponseDto getBlDetails(Long blPoid) {
		validateBl(blPoid);

		List<DemurrageEnquiryContainerDto> containers = enquiryRepository.loadContainers(blPoid);
		// The demurrage columns stay empty until the enquiry is applied for a To Date.
		containers.forEach(container -> {
			container.setDmToDate(null);
			container.setDmDays(null);
			container.setDmChargeAmt(null);
			container.setDmChargeAmtBeforeDiscount(null);
			container.setRemarks(null);
		});

		DemurrageEnquiryResponseDto response = DemurrageEnquiryResponseDto.builder()
				.blPoid(blPoid)
				.doStatus(enquiryRepository.findDoStatus(blPoid))
				.toDate(DateUtil.getCurrentDateInUserTimeZone())
				.discountPercentage(BigDecimal.ZERO)
				.containers(containers)
				.charges(List.of())
				.totalDemurrageAmount(money(BigDecimal.ZERO))
				.receiptAmount(money(BigDecimal.ZERO))
				.totalTaxAmount(money(BigDecimal.ZERO))
				.totalAmountWithVat(money(BigDecimal.ZERO))
				.build();

		enrichLovDetails(response);
		return response;
	}

	@Override
	public DemurrageEnquiryResponseDto applyDate(DemurrageEnquiryRequestDto request) {
		Long blPoid = request.getBlPoid();
		validateBl(blPoid);

		LocalDate toDate = request.getToDate() != null ? request.getToDate() : DateUtil.getCurrentDateInUserTimeZone();
		BigDecimal discount = request.getDiscountPercentage() != null ? request.getDiscountPercentage() : BigDecimal.ZERO;
		// Only a positive value overrides the tariff, so 0 and null are the same request.
		Integer freeDays = request.getFreeDays() != null && request.getFreeDays() > 0 ? request.getFreeDays() : null;
		Long companyPoid = UserContext.getCompanyPoid();
		if (companyPoid == null) {
			// RTN_GLOBAL_PARAMETER fails with ORA-01400 on a null company, which would take the
			// demurrage and the port charges down with it.
			throw new ValidationException("Company context is missing, the charges cannot be resolved");
		}

		log.info("Demurrage enquiry for BL: {}, toDate: {}, discount: {}%, freeDays: {}",
				blPoid, toDate, discount, freeDays);

		List<DemurrageEnquiryContainerDto> containers = enquiryRepository.loadContainers(blPoid);
		BigDecimal totalDemurrage = calculateContainers(blPoid, containers, toDate, discount, freeDays);
		fillDemurrageBreakdown(blPoid, containers, toDate, freeDays);

		List<DemurrageEnquiryChargeDto> charges = buildCharges(blPoid, companyPoid, containers, totalDemurrage);

		BigDecimal receiptAmount = charges.stream()
				.map(DemurrageEnquiryChargeDto::getAmount)
				.filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalTaxAmount = charges.stream()
				.map(DemurrageEnquiryChargeDto::getTaxAmount)
				.filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		DemurrageEnquiryResponseDto response = DemurrageEnquiryResponseDto.builder()
				.blPoid(blPoid)
				.doStatus(enquiryRepository.findDoStatus(blPoid))
				.toDate(toDate)
				.discountPercentage(discount)
				.freeDays(freeDays)
				.containers(containers)
				.charges(charges)
				.totalDemurrageAmount(money(totalDemurrage))
				.receiptAmount(money(receiptAmount))
				.totalTaxAmount(money(totalTaxAmount))
				.totalAmountWithVat(money(receiptAmount.add(totalTaxAmount)))
				.build();

		enrichLovDetails(response);
		return response;
	}

	@Override
	public byte[] printDemurrageCalculation(Long blPoid, LocalDate toDate, BigDecimal discountPercentage,
											Integer freeDays) throws Exception {
		validateBl(blPoid);

		LocalDate tillDate = toDate != null ? toDate : DateUtil.getCurrentDateInUserTimeZone();
		BigDecimal discount = discountPercentage != null ? discountPercentage : BigDecimal.ZERO;
		// The report reads 0 the way the procedure does: keep the free days of the line tariff.
		int appliedFreeDays = freeDays != null && freeDays > 0 ? freeDays : 0;

		// The enquiry has no document of its own - the BL is the key of the report.
		Map<String, Object> params = printService.buildBaseParams(blPoid, DOC_ID);
		params.put("DOC_KEY_POID", String.valueOf(blPoid));
		params.put("P_TILL_DATE", tillDate.format(REPORT_DATE_FORMAT));
		params.put("P_DISCOUNT", discount.toPlainString());
		params.put("P_FREE_DAYS", String.valueOf(appliedFreeDays));
		params.put("SUBREPORT_DEMURRAGE_MASTER", printService.load(DEMURRAGE_MASTER_SUBREPORT));
		params.put("SUBREPORT_DEMURRAGE_DTL", printService.load(DEMURRAGE_DTL_SUBREPORT));

		JasperReport mainReport = printService.load(DEMURRAGE_CALC_REPORT);
		return printService.fillReportToPdf(mainReport, params, dataSource);
	}

	@Override
	public Map<String, Object> list(FilterRequestDto filters, Pageable pageable, LocalDate startDate, LocalDate endDate) {
		String operator = documentSearchService.resolveOperator(filters);
		String isDeleted = documentSearchService.resolveIsDeleted(filters);
		List<FilterDto> filterList = documentSearchService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate, endDate);

		RawSearchResult raw = documentSearchService.search(
				DOC_ID,
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

	/**
	 * Recalculates every container up to the applied To Date and returns the total demurrage. A
	 * container that has already been returned empty is only charged up to its Empty In date.
	 * {@code freeDays}, when given, replaces the free days of every container.
	 */
	private BigDecimal calculateContainers(Long blPoid, List<DemurrageEnquiryContainerDto> containers,
										   LocalDate toDate, BigDecimal discount, Integer freeDays) {
		BigDecimal totalDemurrage = BigDecimal.ZERO;

		for (DemurrageEnquiryContainerDto container : containers) {
			LocalDate effectiveToDate = container.getEmptyIn() != null ? container.getEmptyIn() : toDate;
			container.setDmToDate(effectiveToDate);
			if (freeDays != null) {
				// The column has to show the free days the amount was calculated with.
				container.setFreeDays(freeDays.longValue());
			}

			ContainerDemurrageCalcDto calc = enquiryRepository.calculateContainerDemurrage(
					blPoid, container.getContainerNo(), effectiveToDate, freeDays);

			if (calc == null || calc.getDays() == null || calc.getDays() <= 0) {
				// Still inside the free days or the container was returned before the period started.
				container.setDmDays(0L);
				container.setDmChargeAmt(money(BigDecimal.ZERO));
				container.setDmChargeAmtBeforeDiscount(money(BigDecimal.ZERO));
				continue;
			}

			BigDecimal grossAmount = calc.getAmount() != null ? calc.getAmount() : BigDecimal.ZERO;
			BigDecimal netAmount = applyDiscount(grossAmount, discount);

			container.setDmFrmDate(calc.getFromDate() != null ? calc.getFromDate() : container.getDmFrmDate());
			container.setDmDays(calc.getDays());
			container.setDmChargeAmtBeforeDiscount(money(grossAmount));
			container.setDmChargeAmt(money(netAmount));

			totalDemurrage = totalDemurrage.add(netAmount);
		}

		return totalDemurrage;
	}

	private BigDecimal applyDiscount(BigDecimal amount, BigDecimal discount) {
		if (amount == null || discount == null || discount.compareTo(BigDecimal.ZERO) == 0) {
			return amount != null ? amount : BigDecimal.ZERO;
		}
		BigDecimal discountAmount = amount.multiply(discount).divide(HUNDRED, AMOUNT_SCALE, RoundingMode.HALF_UP);
		return amount.subtract(discountAmount);
	}

	/**
	 * Builds the Charges tab: the charges of the BL manifest that are not received or invoiced yet,
	 * the calculated demurrage and the late collection / revalidation port charges.
	 */
	private List<DemurrageEnquiryChargeDto> buildCharges(Long blPoid, Long companyPoid,
														 List<DemurrageEnquiryContainerDto> containers,
														 BigDecimal totalDemurrage) {
		List<DemurrageEnquiryChargeDto> charges = new ArrayList<>();
		long serialNumber = 0;

		for (ManifestChargeRowDto manifestCharge : enquiryRepository.findManifestCharges(blPoid)) {
			BigDecimal amount = nullSafe(manifestCharge.getAmount());
			BigDecimal taxAmount = nullSafe(manifestCharge.getTaxAmount());
			charges.add(DemurrageEnquiryChargeDto.builder()
					.detRowId(++serialNumber)
					.blPoid(manifestCharge.getBlPoid())
					.chargePoid(manifestCharge.getChargePoid())
					.chargesDetRowId(manifestCharge.getDetRowId())
					.chargeType(CHARGE_TYPE_BL)
					.invoiceType(manifestCharge.getInvoiceType())
					.amount(money(amount))
					.taxPoid(manifestCharge.getTaxPoid())
					.taxPercentage(manifestCharge.getTaxPercentage())
					.taxAmount(money(taxAmount))
					.totalAmount(money(amount.add(taxAmount)))
					.build());
		}

		DemurrageEnquiryChargeDto demurrageCharge =
				buildDemurrageCharge(blPoid, companyPoid, totalDemurrage, containers);
		if (demurrageCharge != null) {
			demurrageCharge.setDetRowId(++serialNumber);
			charges.add(demurrageCharge);
		}

		for (PortChargeRowDto portCharge : enquiryRepository.findPortCharges(blPoid, companyPoid)) {
			DemurrageEnquiryChargeDto charge = buildPortCharge(blPoid, portCharge, containers);
			if (charge != null) {
				charge.setDetRowId(++serialNumber);
				charges.add(charge);
			}
		}

		return charges;
	}

	private DemurrageEnquiryChargeDto buildDemurrageCharge(Long blPoid, Long companyPoid, BigDecimal totalDemurrage,
														   List<DemurrageEnquiryContainerDto> containers) {
		if (totalDemurrage == null || totalDemurrage.compareTo(BigDecimal.ZERO) == 0) {
			return null;
		}

		DemurrageChargeConfigDto config = enquiryRepository.findDemurrageChargeConfig(companyPoid);
		if (config == null || config.getChargePoid() == null) {
			log.warn("Demurrage charge is not mapped on the SHDEMURRAGE parameter, the demurrage row is skipped");
			return null;
		}

		boolean taxed = isTaxed(config.getTaxPoid(), config.getTaxApplicable());
		BigDecimal taxAmount = taxOf(totalDemurrage, config.getTaxPoid(), config.getTaxPercentage(),
				config.getTaxApplicable());

		return DemurrageEnquiryChargeDto.builder()
				.blPoid(blPoid)
				.chargePoid(config.getChargePoid())
				.chargesDetRowId(0L)
				.chargeType(CHARGE_TYPE_DEMURRAGE)
				.remarks(demurrageRemarks(containers))
				.amount(money(totalDemurrage))
				.taxPoid(taxed ? config.getTaxPoid() : null)
				.taxPercentage(taxed ? config.getTaxPercentage() : null)
				.taxAmount(money(taxAmount))
				.totalAmount(money(totalDemurrage.add(taxAmount)))
				.build();
	}

	private DemurrageEnquiryChargeDto buildPortCharge(Long blPoid, PortChargeRowDto portCharge,
													  List<DemurrageEnquiryContainerDto> containers) {
		BigDecimal amount;

		if (APPLICABLE_PER_BL.equalsIgnoreCase(portCharge.getChargeApplicable())) {
			amount = nullSafe(portCharge.getAmountOther());
		} else if (APPLICABLE_PER_QUANTITY.equalsIgnoreCase(portCharge.getChargeApplicable())) {
			amount = perQuantityAmount(portCharge, containers);
		} else {
			return null;
		}

		if (amount.compareTo(BigDecimal.ZERO) == 0) {
			return null;
		}

		boolean taxed = isTaxed(portCharge.getTaxPoid(), portCharge.getTaxApplicable());
		BigDecimal taxAmount = taxOf(amount, portCharge.getTaxPoid(), portCharge.getTaxPercentage(),
				portCharge.getTaxApplicable());

		return DemurrageEnquiryChargeDto.builder()
				.blPoid(blPoid)
				.chargePoid(portCharge.getChargeCodePoid())
				.chargesDetRowId(0L)
				.chargeType(portCharge.getChargeTypeApplicable())
				.remarks(applicableFromRemark(portCharge.getApplicableFrom()))
				.amount(money(amount))
				.taxPoid(taxed ? portCharge.getTaxPoid() : null)
				.taxPercentage(taxed ? portCharge.getTaxPercentage() : null)
				.taxAmount(money(taxAmount))
				.totalAmount(money(amount.add(taxAmount)))
				.build();
	}

	/**
	 * Reads the slab breakdown of every container in one go and hangs it on the rows that carry
	 * demurrage. A container still inside its free days keeps an empty Remarks - there is no slab to
	 * explain - and a breakdown that cannot be read leaves the amounts untouched.
	 */
	private void fillDemurrageBreakdown(Long blPoid, List<DemurrageEnquiryContainerDto> containers,
										LocalDate toDate, Integer freeDays) {
		boolean anythingCharged = containers.stream()
				.anyMatch(container -> container.getDmDays() != null && container.getDmDays() > 0);
		if (!anythingCharged) {
			return;
		}

		Map<String, String> remarks = enquiryRepository.findContainerDemurrageRemarks(blPoid, toDate, freeDays);
		containers.stream()
				.filter(container -> container.getDmDays() != null && container.getDmDays() > 0)
				.forEach(container -> container.setRemarks(remarks.get(container.getContainerNo())));
	}

	/**
	 * Remarks of the calculated demurrage row: the breakdown of every container it bills, prefixed
	 * with the container it belongs to, since the charge is one row for the whole BL.
	 */
	private static String demurrageRemarks(List<DemurrageEnquiryContainerDto> containers) {
		String remarks = containers.stream()
				.filter(container -> container.getRemarks() != null && !container.getRemarks().isBlank())
				.map(container -> container.getContainerNo() + ": " + container.getRemarks())
				.collect(Collectors.joining("; "));
		return remarks.isEmpty() ? null : remarks;
	}

	/**
	 * Remarks of a late collection charge: the day the charge starts to apply, as the screen words it.
	 * The revalidation rows are not date driven and carry no date, so they keep an empty Remarks.
	 */
	private static String applicableFromRemark(LocalDate applicableFrom) {
		if (applicableFrom == null) {
			return null;
		}
		return "Applicable from " + REMARKS_DATE_FORMAT.format(applicableFrom).toUpperCase(java.util.Locale.ENGLISH)
				+ " onwards";
	}

	/**
	 * Per quantity charges are billed per container: the 20' rate for every 20' container and the
	 * 40' rate for every other container. Like the legacy screen, only the containers that actually
	 * carry demurrage are counted - a container still inside its free days is not charged.
	 */
	private BigDecimal perQuantityAmount(PortChargeRowDto portCharge, List<DemurrageEnquiryContainerDto> containers) {
		Map<String, String> sizeByIsoType = new HashMap<>();
		long count20 = 0;
		long count40 = 0;

		for (DemurrageEnquiryContainerDto container : containers) {
			BigDecimal demurrage = container.getDmChargeAmt();
			if (demurrage == null || demurrage.compareTo(BigDecimal.ZERO) == 0) {
				continue;
			}
			String isoType = container.getEquipmentIsoType();
			String size = sizeByIsoType.computeIfAbsent(isoType, enquiryRepository::getContainerSize);
			if (SIZE_20.equalsIgnoreCase(size)) {
				count20++;
			} else {
				count40++;
			}
		}

		return nullSafe(portCharge.getAmount20()).multiply(BigDecimal.valueOf(count20))
				.add(nullSafe(portCharge.getAmount40()).multiply(BigDecimal.valueOf(count40)));
	}

	/**
	 * A charge carries its tax master even when the rate is zero - the legacy screen shows the tax
	 * POID and percentage of every taxable charge, not only of the ones that produce an amount.
	 */
	private boolean isTaxed(Long taxPoid, String taxApplicable) {
		return taxPoid != null && "Y".equalsIgnoreCase(taxApplicable);
	}

	private BigDecimal taxOf(BigDecimal amount, Long taxPoid, BigDecimal taxPercentage, String taxApplicable) {
		if (taxPercentage == null || !isTaxed(taxPoid, taxApplicable)) {
			return BigDecimal.ZERO;
		}
		return amount.multiply(taxPercentage).divide(HUNDRED, AMOUNT_SCALE, RoundingMode.HALF_UP);
	}

	private void enrichLovDetails(DemurrageEnquiryResponseDto response) {
		List<DemurrageEnquiryContainerDto> containers =
				response.getContainers() != null ? response.getContainers() : List.of();
		List<DemurrageEnquiryChargeDto> charges =
				response.getCharges() != null ? response.getCharges() : List.of();

		List<Long> blPoids = Stream.concat(
				Stream.of(response.getBlPoid()),
				Stream.concat(
						containers.stream().map(DemurrageEnquiryContainerDto::getBlPoid),
						charges.stream().map(DemurrageEnquiryChargeDto::getBlPoid)
				)
		).filter(Objects::nonNull).distinct().collect(Collectors.toList());

		List<Long> chargePoids = charges.stream().map(DemurrageEnquiryChargeDto::getChargePoid)
				.filter(Objects::nonNull).distinct().collect(Collectors.toList());

		List<Long> taxPoids = charges.stream().map(DemurrageEnquiryChargeDto::getTaxPoid)
				.filter(Objects::nonNull).distinct().collect(Collectors.toList());

		Map<Long, LovGetListDto> blMap = lovService.getDetailsByPoidsAndLovName(blPoids, BL_LOV);
		Map<Long, LovGetListDto> chargeMap = lovService.getDetailsByPoidsAndLovName(chargePoids, CHARGE_LOV);
		Map<Long, LovGetListDto> taxMap = lovService.getDetailsByPoidsAndLovName(taxPoids, TAX_LOV);

		response.setBlDet(lovOf(blMap, response.getBlPoid()));
		containers.forEach(container -> container.setBlDet(lovOf(blMap, container.getBlPoid())));
		charges.forEach(charge -> {
			charge.setBlDet(lovOf(blMap, charge.getBlPoid()));
			charge.setChargeDet(lovOf(chargeMap, charge.getChargePoid()));
			charge.setTaxDet(lovOf(taxMap, charge.getTaxPoid()));
		});
	}

	private static LovGetListDto lovOf(Map<Long, LovGetListDto> lovMap, Long poid) {
		return poid != null ? lovMap.get(poid) : null;
	}

	private void validateBl(Long blPoid) {
		if (!enquiryRepository.blExists(blPoid)) {
			throw new ResourceNotFoundException("BL", "blPoid", String.valueOf(blPoid));
		}
	}

	private static BigDecimal nullSafe(BigDecimal value) {
		return value != null ? value : BigDecimal.ZERO;
	}

	/**
	 * Every monetary figure leaves the enquiry on the same scale the screen shows (3 decimals), so
	 * 56 and 192080 come back as 56.000 and 192080.000 rather than on whatever scale Oracle happened
	 * to return.
	 */
	private static BigDecimal money(BigDecimal value) {
		return value != null ? value.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP) : null;
	}
}
