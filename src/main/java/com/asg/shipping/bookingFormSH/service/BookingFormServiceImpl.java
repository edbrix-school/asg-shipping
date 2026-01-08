package com.asg.shipping.bookingFormSH.service;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.CallableStatement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.bookingFormSH.dto.BookingFormCargoDetailDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormChargesDetailDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormContainerDetailDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormCreateDTO;
import com.asg.shipping.bookingFormSH.dto.BookingFormDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormUpdateDTO;
import com.asg.shipping.bookingFormSH.entity.ShipMateCargoDtl;
import com.asg.shipping.bookingFormSH.entity.ShipMateChargesDtl;
import com.asg.shipping.bookingFormSH.entity.ShipMateContainerDtl;
import com.asg.shipping.bookingFormSH.entity.ShipMateHdr;
import com.asg.shipping.bookingFormSH.repository.ShipMateCargoDtlRepository;
import com.asg.shipping.bookingFormSH.repository.ShipMateChargesDtlRepository;
import com.asg.shipping.bookingFormSH.repository.ShipMateContainerDtlRepository;
import com.asg.shipping.bookingFormSH.repository.ShipMateHdrRepository;
import com.asg.shipping.bookingFormSH.util.BookingFormMapper;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.exceptions.ValidationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingFormServiceImpl implements BookingFormService {

	private final ShipMateHdrRepository headerRepository;
	private final ShipMateCargoDtlRepository cargoDtlRepository;
	private final ShipMateChargesDtlRepository chargesDtlRepository;
	private final ShipMateContainerDtlRepository containerDtlRepository;
	private final BookingFormLovService lovService;
	private final DocumentSearchService documentService;
	private final BookingFormMapper mapper;
	private final JdbcTemplate jdbcTemplate;

	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> searchBookingForm(String docId, FilterRequestDto request, Pageable pageable) {
		log.info("Searching booking form records with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(),
				pageable.getPageSize());

		String operator = documentService.resolveOperator(request);
		String isDeleted = documentService.resolveIsDeleted(request);
		List<FilterDto> filters = documentService.resolveFilters(request);

		RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted, "DOC_REF",
				"TRANSACTION_POID");

		Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

		return PaginationUtil.wrapPage(page, raw.displayFields());
	}

	@Override
	@Transactional(readOnly = true)
	public BookingFormDto getBookingForm(Long id) {
		log.info("Getting booking form with id: {}", id);

		Long groupPoid = UserContext.getGroupPoid();
		Long companyPoid = UserContext.getCompanyPoid();

		ShipMateHdr entity = headerRepository
				.findByTransactionPoidAndGroupPoidAndCompanyPoid(id, groupPoid, companyPoid)
				.orElseThrow(() -> new ResourceNotFoundException("Booking Form", "transactionPoid", id.toString()));

		if ("Y".equals(entity.getDeleted())) {
			throw new ResourceNotFoundException("Booking Form", "transactionPoid", id.toString());
		}

		// Load detail tables
		List<ShipMateCargoDtl> cargoDetails = cargoDtlRepository.findByTransactionPoidOrderByDetRowId(id);
		List<ShipMateChargesDtl> chargesDetails = chargesDtlRepository.findByTransactionPoidOrderByDetRowId(id);
		List<ShipMateContainerDtl> containerDetails = containerDtlRepository.findByTransactionPoidOrderByDetRowId(id);
		BookingFormDto dto = mapper.mapToDto(entity);

		// Map detail tables
		dto.setCargoDetails(mapper.mapCargoDtlListToDto(cargoDetails));
		dto.setChargesDetails(mapper.mapChargesDtlListToDto(chargesDetails));
		dto.setContainerDetails(mapper.mapContainerDtlListToDto(containerDetails));
		// Enrich with LOV data
		enrichLovDetails(dto);
		return dto;
	}

	@Override
	@Transactional
	public BookingFormDto createBookingForm(BookingFormCreateDTO createDTO) {
		log.info("Creating new booking form");

		Long groupPoid = UserContext.getGroupPoid();
		Long companyPoid = UserContext.getCompanyPoid();

		validateLineMate(createDTO.getLinePoid(), null, groupPoid, companyPoid);

		ShipMateHdr entity = new ShipMateHdr();
		mapper.mapCreateDTOToEntity(createDTO, entity, groupPoid, companyPoid);

		entity = headerRepository.save(entity);

		if (createDTO.getContainerDetails() != null) {
			for (BookingFormContainerDetailDto containerDto : createDTO.getContainerDetails()) {
				if (containerDto.getContainerNo() != null && containerDto.getContainerNo().length() >= 3) {
					validateContainerLoad(containerDto.getContainerNo(), entity.getLinePoid(),
							entity.getTransactionPoid(), groupPoid, companyPoid);
				}
			}
		}

		// Save detail tables
		saveDetailTables(entity.getTransactionPoid(), createDTO.getCargoDetails(), createDTO.getChargesDetails(),
				createDTO.getContainerDetails());

		// Call PROC_SHIP_BL_PAGE_SAVE_AFTER after save (for split booking allocation)
		Long userPoid = UserContext.getUserPoid();
		callProcShipBlPageSaveAfter(groupPoid, companyPoid, entity.getTransactionPoid(), null, "ALLOCATESPLITBOOKING",
				userPoid);

		// Reload and return
		return getBookingForm(entity.getTransactionPoid());
	}

	@Override
	@Transactional
	public void updateBookingForm(Long id, BookingFormUpdateDTO updateDTO) {
		log.info("Updating booking form with id: {}", id);

		Long groupPoid = UserContext.getGroupPoid();
		Long companyPoid = UserContext.getCompanyPoid();
		System.err.println("companyPoid --->" + companyPoid);

		ShipMateHdr entity = headerRepository
				.findByTransactionPoidAndGroupPoidAndCompanyPoid(id, groupPoid, companyPoid)
				.orElseThrow(() -> new ResourceNotFoundException("Booking Form", "transactionPoid", id.toString()));

		if ("Y".equals(entity.getDeleted())) {
			throw new ResourceNotFoundException("Booking Form", "transactionPoid", id.toString());
		}

		// Validate LINE_POID and MATE relationship before save
		if (updateDTO.getLinePoid() != null) {
			validateLineMate(updateDTO.getLinePoid(), id, groupPoid, companyPoid);
		}

		// Update header
		mapper.mapUpdateDTOToEntity(updateDTO, entity);

		// Validate BOOKING_ISSUE_NO uniqueness if changed
		if (updateDTO.getBookingIssueNo() != null
				&& !updateDTO.getBookingIssueNo().equals(entity.getBookingIssueNo())) {
			if (headerRepository.existsByBookingIssueNoAndNotDeletedExcludingPoid(updateDTO.getBookingIssueNo(), id)) {
				throw new ValidationException("Booking Issue Number already exists: " + updateDTO.getBookingIssueNo());
			}
			entity.setBookingIssueNo(updateDTO.getBookingIssueNo());
		}

		// Validate containers before saving details
		if (updateDTO.getContainerDetails() != null) {
			Long linePoid = updateDTO.getLinePoid() != null ? updateDTO.getLinePoid() : entity.getLinePoid();
			for (BookingFormContainerDetailDto containerDto : updateDTO.getContainerDetails()) {
				if (containerDto.getContainerNo() != null && containerDto.getContainerNo().length() >= 3) {
					validateContainerLoad(containerDto.getContainerNo(), linePoid, id, groupPoid, companyPoid);
				}
			}
		}

		// Delete existing detail records
		cargoDtlRepository.deleteByTransactionPoid(id);
		chargesDtlRepository.deleteByTransactionPoid(id);
		containerDtlRepository.deleteByTransactionPoid(id);

		// Save updated detail tables
		saveDetailTables(id, updateDTO.getCargoDetails(), updateDTO.getChargesDetails(),
				updateDTO.getContainerDetails());

		// Call PROC_SHIP_BL_PAGE_SAVE_AFTER after save
		Long userPoid = UserContext.getUserPoid();
		callProcShipBlPageSaveAfter(groupPoid, companyPoid, id, null, "ALLOCATESPLITBOOKING", userPoid);
	}

	@Override
	@Transactional
	public void deleteBookingForm(Long id) {
		log.info("Deleting booking form with id: {}", id);

		Long groupPoid = UserContext.getGroupPoid();
		Long companyPoid = UserContext.getCompanyPoid();

		ShipMateHdr entity = headerRepository
				.findByTransactionPoidAndGroupPoidAndCompanyPoid(id, groupPoid, companyPoid)
				.orElseThrow(() -> new ResourceNotFoundException("Booking Form", "transactionPoid", id.toString()));

		entity.setDeleted("Y");
		entity.setLastModifiedBy(getCurrentUser());
		entity.setLastModifiedDate(LocalDateTime.now());
		headerRepository.save(entity);
	}

	@Override
	@Transactional
	public String generateCoprarBooking(Long transactionPoid) {
		log.info("Generating COPRAR booking file for transaction: {}", transactionPoid);

		Long userPoid = UserContext.getUserPoid();

		try {
			String sql = "{call proc_coprar_BOOKING_V2(?, ?, ?)}";
			String res = jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
				CallableStatement cs = connection.prepareCall(sql);
				cs.setLong(1, transactionPoid);
				cs.setLong(2, userPoid);
				cs.registerOutParameter(3, Types.VARCHAR);
				cs.execute();
				String status = cs.getString(3);
				cs.close();
				return status;
			});
			if (!res.toLowerCase().startsWith("error"))
				return generateCoprarFile(transactionPoid, userPoid);
			return res;
		} catch (Exception e) {
			log.error("Error generating COPRAR booking file for transaction: {}", transactionPoid, e);
			throw new ValidationException("Error generating COPRAR booking file: " + e.getMessage());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public String getEmptyShipper(Long companyPoid) {
		log.info("Getting empty shipper for company: {}", companyPoid);

		try {
			String sql = "{? = call FUNC_MATE_EMPTY_SHIPPER(?)}";
			return jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
				CallableStatement cs = connection.prepareCall(sql);
				cs.registerOutParameter(1, Types.VARCHAR);
				cs.setLong(2, companyPoid);
				cs.execute();
				String result = cs.getString(1);
				cs.close();
				return result;
			});
		} catch (Exception e) {
			log.error("Error getting empty shipper for company: {}", companyPoid, e);
			throw new ValidationException("Error getting empty shipper: " + e.getMessage());
		}
	}

	@Override
	@Transactional
	public String processEmptyContainerLoad(Long transactionPoid) {
		log.info("Processing empty container load for transaction: {}", transactionPoid);

		Long groupPoid = UserContext.getGroupPoid();
		Long userPoid = UserContext.getUserPoid();
		Long companyPoid = UserContext.getCompanyPoid();

		try {
			String sql = "{call PROC_SHIP_EMPTY_CONTAINER_LOAD(?, ?, ?, ?, ?)}";
			return jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
				CallableStatement cs = connection.prepareCall(sql);
				cs.setLong(1, groupPoid);
				cs.setLong(2, userPoid);
				cs.setLong(3, companyPoid);
				cs.setLong(4, transactionPoid);
				cs.registerOutParameter(5, Types.VARCHAR);
				cs.execute();
				String status = cs.getString(5);
				cs.close();
				return status;
			});
		} catch (Exception e) {
			log.error("Error processing empty container load for transaction: {}", transactionPoid, e);
			throw new ValidationException("Error processing empty container load: " + e.getMessage());
		}
	}

	private void validateLineMate(Long linePoid, Long transactionPoid, Long groupPoid, Long companyPoid) {
		try {
			String sql = "{call PROC_SHIP_MATE_VLD_LINE_MATE(?, ?, ?, ?, ?)}";
			jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
				CallableStatement cs = connection.prepareCall(sql);
				cs.setLong(1, groupPoid);
				cs.setLong(2, companyPoid);
				cs.setLong(3, linePoid);
				cs.registerOutParameter(4, Types.VARCHAR);
				cs.setObject(5, transactionPoid);
				cs.execute();
				String status = cs.getString(4);
				cs.close();
				if (status == null || status.contains("ERROR")) {
					throw new ValidationException("Line and Mate validation failed: " + status);
				}
				return null;
			});
		} catch (ValidationException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error validating line and mate for linePoid: {}, transactionPoid: {}", linePoid, transactionPoid,
					e);
			throw new ValidationException("Error validating line and mate: " + e.getMessage());
		}
	}

	public String generateCoprarFile(Long bookingTransactionPoid, Long loginUserPoid) {
		try {

			String timestamp = new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date());

			String fileName = "COPRANBAH_" + timestamp + "_" + bookingTransactionPoid + ".TXT";

			String directory = "/win3mount/EMAILS_FROM_ORACLE";

			Path dirPath = Paths.get(directory);
			if (!Files.exists(dirPath)) {
				Files.createDirectories(dirPath);
			}

			Path filePath = Paths.get(directory, fileName);

			List<String> ediLines = jdbcTemplate.query("""
					SELECT TAB_TEXT
					FROM EDI_COPRAR_CREATED
					WHERE BOOKING_TRANSACTION_POID = ?
					ORDER BY BOOKING_TRANSACTION_POID, SEQNO
					""", (rs, rowNum) -> rs.getString("TAB_TEXT"), bookingTransactionPoid);

			try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {

				for (String line : ediLines) {
					writer.write(line);
					writer.newLine();
				}
			}

			queueMail(loginUserPoid, fileName);

			return "Copran generated, Sent Mail...";

		} catch (Exception e) {
			return "ERROR : " + e.getMessage();
		}
	}

	private void queueMail(Long userPoid, String fileName) {
		jdbcTemplate.update("""
				INSERT INTO GLOBAL_MAIL_SENDING_QUEUE
				(RECEVER_EMAIL_ID, MESSAGE_SUBJECT, MESSGE_BODY,
				 SENT_STATUS, QUEUE_DATE, SENDING_POID,
				 ATTACHED_FILE_NAME, LINK_TRANSACTION_POID)
				SELECT NVL(USER_EMAIL,'OPS@SHIPPINGBAHRAIN.COM'),
				       'COPRAN EXPORT BOOKING EDI ' || TO_CHAR(SYSDATE,'DD-MON-RRRR'),
				       'COPRAN MATE BOOKING ' || TO_CHAR(SYSDATE,'DD-MON-RRRR'),
				       'N', SYSDATE, ?, ?, NULL
				FROM GLOBAL_USERS
				WHERE USER_POID = ?
				""", userPoid, fileName, userPoid);
	}

	/**
	 * Validate container load using PROC_SHIP_VLD_CONTAINER_LOAD
	 */
	private void validateContainerLoad(String containerNo, Long linePoid, Long transactionPoid, Long groupPoid,
			Long companyPoid) {
		try {
			String sql = "{call PROC_SHIP_VLD_CONTAINER_LOAD(?, ?, ?, ?, ?, ?)}";
			jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
				CallableStatement cs = connection.prepareCall(sql);
				cs.setLong(1, groupPoid);
				cs.setLong(2, companyPoid);
				cs.setString(3, containerNo);
				cs.setLong(4, linePoid);
				cs.registerOutParameter(5, Types.VARCHAR);
				cs.setLong(6, transactionPoid);
				cs.execute();
				String status = cs.getString(5);
				cs.close();
				if (status == null || status.contains("ERROR")) {
					throw new ValidationException(
							"Container load validation failed for container " + containerNo + ": " + status);
				}
				return null;
			});
		} catch (ValidationException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error validating container load for containerNo: {}, linePoid: {}, transactionPoid: {}",
					containerNo, linePoid, transactionPoid, e);
			throw new ValidationException("Error validating container load: " + e.getMessage());
		}
	}

	/**
	 * Call PROC_SHIP_BL_PAGE_SAVE_AFTER stored procedure
	 */
	@Transactional
	private void callProcShipBlPageSaveAfter(Long groupPoid, Long companyPoid, Long transactionPoid,
			Long splitBookingNo, String actionType, Long userPoid) {
		try {
			String sql = "{call PROC_SHIP_BL_PAGE_SAVE_AFTER(?, ?, ?, ?, ?, ?)}";
			jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
				CallableStatement cs = connection.prepareCall(sql);
				cs.setLong(1, groupPoid);
				cs.setLong(2, companyPoid);
				cs.setObject(3, splitBookingNo != null ? splitBookingNo : transactionPoid);
				cs.setObject(4, splitBookingNo != null ? transactionPoid : null);
				cs.setString(5, actionType != null ? actionType : "ALLOCATESPLITBOOKING");
				cs.setLong(6, userPoid);
				cs.execute();
				cs.close();
				return null;
			});
			log.debug("Successfully called PROC_SHIP_BL_PAGE_SAVE_AFTER for transaction: {}", transactionPoid);
		} catch (Exception e) {
			log.error("Error calling PROC_SHIP_BL_PAGE_SAVE_AFTER for transaction: {}", transactionPoid, e);
		}
	}

	/**
	 * Save detail tables
	 */
	private void saveDetailTables(Long transactionPoid, List<BookingFormCargoDetailDto> cargoDetails,
			List<BookingFormChargesDetailDto> chargesDetails, List<BookingFormContainerDetailDto> containerDetails) {
		// Save cargo details
		if (cargoDetails != null && !cargoDetails.isEmpty()) {
			Long maxDetRowId = cargoDtlRepository.getMaxDetRowId(transactionPoid);
			for (BookingFormCargoDetailDto dto : cargoDetails) {
				ShipMateCargoDtl entity = mapper.mapCargoDtlFromDto(dto, transactionPoid);
				if (entity != null && entity.getDetRowId() == null) {
					entity.setDetRowId(++maxDetRowId);
				}
				cargoDtlRepository.save(entity);
			}
		}

		// Save charges details
		if (chargesDetails != null && !chargesDetails.isEmpty()) {
			Long maxDetRowId = chargesDtlRepository.getMaxDetRowId(transactionPoid);
			for (BookingFormChargesDetailDto dto : chargesDetails) {
				ShipMateChargesDtl entity = mapper.mapChargesDtlFromDto(dto, transactionPoid);
				if (entity != null && entity.getDetRowId() == null) {
					entity.setDetRowId(++maxDetRowId);
				}
				chargesDtlRepository.save(entity);
			}
		}

		// Save container details
		if (containerDetails != null && !containerDetails.isEmpty()) {
			Long maxDetRowId = containerDtlRepository.getMaxDetRowId(transactionPoid);
			for (BookingFormContainerDetailDto dto : containerDetails) {
				ShipMateContainerDtl entity = mapper.mapContainerDtlFromDto(dto, transactionPoid);
				if (entity != null && entity.getDetRowId() == null) {
					entity.setDetRowId(++maxDetRowId);
				}
				containerDtlRepository.save(entity);
			}
		}
	}


	private void enrichLovDetails(BookingFormDto dto) {
		if (dto.getQuotationTransactionPoid() != null) {
			lovService.getQuotaionLov(dto.getQuotationTransactionPoid()).stream().findFirst()
					.ifPresent(dto::setQuotationTransactionPoidDet);
		}
		if (dto.getVesselPoid() != null) {
			lovService.getVesselMasterLov(dto.getVesselPoid()).stream().findFirst().ifPresent(dto::setVesselPoidDet);
		}
		if (dto.getLinePoid() != null) {
			lovService.getLineMasterLov(dto.getLinePoid()).stream().findFirst().ifPresent(dto::setLinePoidDet);
		}
		if (dto.getSalesmanPoid() != null) {
			lovService.getSalesmanLov(dto.getSalesmanPoid()).stream().findFirst().ifPresent(dto::setSalesmanPoidDet);
		}
		if (dto.getComodityPoid() != null) {
			lovService.getCommodityMasterLov(dto.getComodityPoid()).stream().findFirst()
					.ifPresent(dto::setComodityPoidDet);
		}
		if (dto.getPlaceOfRecieptPoid() != null) {
			lovService.getPortMasterLov(dto.getPlaceOfRecieptPoid()).stream().findFirst()
					.ifPresent(dto::setPlaceOfRecieptPoidDet);
		}
		if (dto.getPlaceOfDelieveryPoid() != null) {
			lovService.getPortMasterLov(dto.getPlaceOfDelieveryPoid()).stream().findFirst()
					.ifPresent(dto::setPlaceOfDelieveryPoidDet);
		}
		if (dto.getPortOfLoadingPoid() != null) {
			lovService.getPortMasterLov(dto.getPortOfLoadingPoid()).stream().findFirst()
					.ifPresent(dto::setPortOfLoadingPoidDet);
		}
		if (dto.getPortOfDischargePoid() != null) {
			lovService.getPortMasterLov(dto.getPortOfDischargePoid()).stream().findFirst()
					.ifPresent(dto::setPortOfDischargePoidDet);
		}
		if (dto.getMateLoadVoyagePoid() != null) {
			lovService.getVoyageMasterLov(dto.getMateLoadVoyagePoid()).stream().findFirst()
					.ifPresent(dto::setMateLoadVoyagePoidDet);
		}

		if (dto.getChargesDetails() != null) {
			dto.getChargesDetails().forEach(charge -> {
				if (charge.getChargePoid() != null) {
					lovService.getChargeMasterLov(charge.getChargePoid()).stream().findFirst()
							.ifPresent(charge::setChargePoidDet);
				}
				if (charge.getPaidAtPortPoid() != null) {
					lovService.getPortMasterLov(charge.getPaidAtPortPoid()).stream().findFirst()
							.ifPresent(charge::setPaidAtPortPoidDet);
				}
			});
		}
		if (dto.getContainerDetails() != null) {
			dto.getContainerDetails().forEach(container -> {
				if (container.getComodityPoid() != null) {
					lovService.getCommodityMasterLov(container.getComodityPoid()).stream().findFirst()
							.ifPresent(container::setComodityPoidDet);
				}
				if (container.getDestinationPortPoid() != null) {
					lovService.getPortMasterLov(container.getDestinationPortPoid()).stream().findFirst()
							.ifPresent(container::setDestinationPortPoidDet);

				}
			});
		}
	}

}
