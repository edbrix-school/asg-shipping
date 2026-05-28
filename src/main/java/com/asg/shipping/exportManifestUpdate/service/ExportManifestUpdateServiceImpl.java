package com.asg.shipping.exportManifestUpdate.service;

import javax.sql.DataSource;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.common.service.LovService;
import com.asg.shipping.exportManifestUpdate.dto.*;
import com.asg.shipping.exportManifestUpdate.entity.*;
import com.asg.shipping.exportManifestUpdate.mapper.ExportManifestUpdateMapper;
import com.asg.shipping.exportManifestUpdate.repository.*;
import com.asg.shipping.address.entity.AddressDetails;
import com.asg.shipping.address.entity.AddressDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import net.sf.jasperreports.engine.JasperReport;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;
import static com.asg.common.lib.security.util.UserContext.getGroupPoid;
import static com.asg.common.lib.security.util.UserContext.getCompanyPoid;
import static com.asg.common.lib.security.util.UserContext.getUserPoid;

/**
 * Service implementation for Export Manifest BL operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExportManifestUpdateServiceImpl implements ExportManifestBlService {

    private static final String DOC_ID = "100-352";

    private final ExportShipBlManifestHdrRepository hdrRepository;
    private final ExportShipBlManifestGeneralDtlRepository generalDtlRepository;
    private final ExportShipBlManifestContainerDtlRepository containerDtlRepository;
    private final ExportShipBlManifestCargoDtlRepository cargoDtlRepository;
    private final ExportShipBlManifestChargesDtlRepository chargesDtlRepository;
    private final ExportManifestBlCustomRepository customBLRepository;
    private final ExportManifestUpdateMapper mapper;
    private final DocumentSearchService documentSearchService;
    private final LovService lovService;
	private final PrintService printService;
	private final DataSource dataSource;
    private final AddressDetailsRepository addressDetailsRepository;

    // ========== Header Operations ==========

    @Override
    @Transactional(readOnly = true)
    public ExportManifestBlResponse getExportBlById(Long transactionPoid) {
        log.info("Getting Export BL by ID: {}", transactionPoid);
        
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        
        // Query filters: DELETED = 'N' (NULL excluded), BL_TYPE = 'EXPORT', GROUP_POID and COMPANY_POID match
        ExportShipBlManifestHdr entity = hdrRepository
                .findExportBlByTransactionPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new RuntimeException("Export BL not found with ID: " + transactionPoid));
        
        ExportManifestBlResponse response = mapper.mapToResponse(entity);

        // Enrich with LOV data
        enrichHeaderWithLovData(response, entity);

        // Retrieve and set general cargo details
        response.setGeneralCargoDetails(getGeneralCargoDetails(transactionPoid));

        // Simple single-string view: all DESC / MARK rows joined via native query on string column
        response.setSimpleCargoDescription(joinDescriptionStrings(
                cargoDtlRepository.findDescriptionStringsByType(transactionPoid, "DESC")));
        response.setSimpleCargoMarks(joinDescriptionStrings(
                cargoDtlRepository.findDescriptionStringsByType(transactionPoid, "MARK")));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchExportBls(FilterRequestDto filters, Pageable pageable) {
        log.info("Searching Export BLs with filters: {}", filters);
        
        // Resolve filter components from FilterRequestDto
        String operator = documentSearchService.resolveOperator(filters);
        String isDeleted = documentSearchService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentSearchService.resolveFilters(filters);
        
        // Call DocumentSearchService.search with docId "100-352"
        // This will use the document configuration from the database (SQL query, display fields, etc.)
        RawSearchResult raw = documentSearchService.search(
                DOC_ID,
                filterList,
                operator,
                pageable,
                isDeleted,
                "BL_NUMBER",           // label field for display
                "TRANSACTION_POID"      // value field (primary key)
        );
        
        // Convert RawSearchResult to Page
        Page<Map<String, Object>> page = new PageImpl<>(
                raw.records(),
                pageable,
                raw.totalRecords()
        );
        
        // Wrap with pagination and display fields
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public ExportManifestBlResponse createExportBl(ExportManifestBlRequest request) {
        log.info("Creating Export BL with BL Number: {}", request.getBlNumber());
        
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        String userId = getCurrentUser();
        
        // Validate required fields
        if (request.getVoyageTransactionPoid() == null) {
            throw new RuntimeException("Voyage Transaction POID is required");
        }
        if (request.getSalesmanPoid() == null) {
            throw new RuntimeException("Salesman POID is required");
        }
        
        // Validate BL number uniqueness
        if (request.getBlNumber() != null && !request.getBlNumber().trim().isEmpty()) {
            String status = customBLRepository.validateBlNumberDuplicate(
                    request.getBlNumber().trim(), "NEWRECORD", "INSERTING");
            if (status == null || !status.startsWith("SUCCESS")) {
                throw new RuntimeException("Export BL number already exists: " + request.getBlNumber());
            }
        }
        
        // Create entity
        ExportShipBlManifestHdr entity = ExportShipBlManifestHdr.builder()
                .groupPoid(groupPoid)
                .companyPoid(companyPoid)
                .transactionDate(LocalDate.now())
                .blType("EXPORT")
                .blStatus("OPEN")
                .blOrginalPrint("N")
                .releasedStatus("NONE")
                .holdCanDo("NONE")
                .holdReason("5")
                .canSentQueue("N")
                .deleted("N")
                .blIssueType(request.getBlIssueType() != null ? request.getBlIssueType() : "1")
                .bookedByPp(request.getBookedByPp() != null ? request.getBookedByPp() : "N")
                .allInOneFreight(request.getAllInOneFreight() != null ? request.getAllInOneFreight() : "Y")
                .build();
        
        // Map request fields to entity
        mapper.mapRequestToEntity(request, entity, groupPoid, companyPoid, userId);
        
        // Generate docRef if not provided
        if (entity.getDocRef() == null || entity.getDocRef().trim().isEmpty()) {
            entity.setDocRef(entity.getBlNumber() != null ? entity.getBlNumber() : "BL-" + System.currentTimeMillis());
        }
        
        // Generate uniqueBlno
        if (entity.getBlNumber() != null) {
            entity.setUniqueBlno(entity.getBlNumber());
        }
        
        // Save entity
        ExportShipBlManifestHdr saved = hdrRepository.save(entity);
        
        log.info("Successfully created Export BL with ID: {}", saved.getTransactionPoid());
        ExportManifestBlResponse response = mapper.mapToResponse(saved);
        
        // Enrich with LOV data
        enrichHeaderWithLovData(response, saved);
        
        return response;
    }

    @Override
    public ExportManifestBlResponse updateExportBl(Long transactionPoid, ExportManifestBlRequest request) {
        log.info("Updating Export BL with ID: {}", transactionPoid);
        
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        String userId = getCurrentUser();
        
        // Get existing entity (DELETED = 'N', BL_TYPE = 'EXPORT')
        ExportShipBlManifestHdr entity = hdrRepository
                .findExportBlByTransactionPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new RuntimeException("Export BL not found with ID: " + transactionPoid));
        
        // Validate BL number uniqueness if changed
        if (request.getBlNumber() != null && !request.getBlNumber().trim().equals(entity.getBlNumber())) {
            String status = customBLRepository.validateBlNumberDuplicate(
                    request.getBlNumber().trim(), entity.getBlNumber(), "UPDATING");
            if (status == null || !status.startsWith("SUCCESS")) {
                throw new RuntimeException("Export BL number already exists: " + request.getBlNumber());
            }
        }
        
        // Store old cargo type for comparison
        String oldCargoType = entity.getCargoType();
        
        // Map request fields to entity
        mapper.mapRequestToEntity(request, entity, groupPoid, companyPoid, userId);
        
        // Update cargo type in container inventory if changed
        if (request.getCargoType() != null && !request.getCargoType().equals(oldCargoType)) {
            // This would typically be handled by the trigger, but we can also do it here
            // The trigger SHIP_BL_MANIFEST_HDR_TRG handles this
        }
        
        // Clean EDI fields if BL type is not EXPORT
        if (entity.getBlType() != null && !entity.getBlType().equals("EXPORT")) {
            // Trigger handles this, but we can also do it here
            if (entity.getShipperEdiName() != null) {
                entity.setShipperEdiName(entity.getShipperEdiName().replace("\n", "").replace("\r", "").trim());
            }
            // Similar for other EDI fields
        }
        
        // Save entity
        ExportShipBlManifestHdr saved = hdrRepository.save(entity);
        
        log.info("Successfully updated Export BL with ID: {}", saved.getTransactionPoid());
        ExportManifestBlResponse response = mapper.mapToResponse(saved);
        
        // Enrich with LOV data
        enrichHeaderWithLovData(response, saved);
        
        return response;
    }

    private void validateHeaderExists(Long transactionPoid) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        if (hdrRepository.findExportBlByTransactionPoid(transactionPoid, groupPoid, companyPoid).isEmpty()) {
            throw new RuntimeException("Export BL not found with ID: " + transactionPoid);
        }
    }

    private ActionType resolveAction(ActionType actionType) {
        return actionType != null ? actionType : ActionType.NOCHANGES;
    }


    @Override
    public ExportManifestUpdateResponse updateExportBlCombined(Long transactionPoid, ExportManifestUpdateRequest request) {
        log.info("Updating Export BL combined for ID: {}", transactionPoid);

        // --- Simple string saves run FIRST so subsequent list fetches reflect the new data ---
        String simpleCargoDescResponse = null;
        if (request.getSimpleCargoDescription() != null) {
            simpleCargoDescResponse = saveSimpleCargoDescription(transactionPoid, request.getSimpleCargoDescription());
        }

        String simpleCargoMarksResponse = null;
        if (request.getSimpleCargoMarks() != null) {
            simpleCargoMarksResponse = saveSimpleCargoMarks(transactionPoid, request.getSimpleCargoMarks());
        }

        // --- Header ---
        // With @JsonUnwrapped, header is always non-null but fields may all be null if FE sent nothing.
        // Use voyageTransactionPoid as a meaningful presence check.
        ExportManifestBlResponse headerResponse = null;
        ExportManifestBlRequest headerReq = request.getHeader();
        if (headerReq != null && headerReq.getVoyageTransactionPoid() != null) {
            headerResponse = updateExportBl(transactionPoid, headerReq);
        } else {
            headerResponse = getExportBlById(transactionPoid);
        }

        // --- General cargo ---
        List<GeneralCargoDetailDto> generalCargoResponse = null;
        if (request.getGeneralCargoDetails() != null) {
            generalCargoResponse = updateGeneralCargoDetails(transactionPoid, request.getGeneralCargoDetails());
        } else {
            generalCargoResponse = getGeneralCargoDetails(transactionPoid);
        }

        // --- Container details ---
        List<ContainerDetailDto> containerResponse = null;
        if (request.getContainerDetails() != null) {
            containerResponse = updateContainerDetails(transactionPoid, request.getContainerDetails());
        } else {
            containerResponse = getContainerDetails(transactionPoid);
        }

        // --- Cargo description list (fetch reflects simple save above if list not provided) ---
        List<CargoDescriptionDto> cargoDescResponse = null;
        if (request.getCargoDescription() != null) {
            cargoDescResponse = updateCargoDescription(transactionPoid, request.getCargoDescription());
        } else {
            cargoDescResponse = getCargoDescription(transactionPoid);
        }

        // --- Cargo marks list (fetch reflects simple save above if list not provided) ---
        List<CargoMarksDto> cargoMarksResponse = null;
        if (request.getCargoMarks() != null) {
            cargoMarksResponse = updateCargoMarks(transactionPoid, request.getCargoMarks());
        } else {
            cargoMarksResponse = getCargoMarks(transactionPoid);
        }

        // --- Charge details ---
        Map<String, Object> chargeResponse = null;
        if (request.getChargeDetails() != null) {
            chargeResponse = updateChargeDetails(transactionPoid, request.getChargeDetails());
        } else {
            chargeResponse = getChargeDetails(transactionPoid);
        }

        ExportManifestUpdateResponse response = new ExportManifestUpdateResponse();
        response.setHeader(headerResponse);
        response.setGeneralCargoDetails(generalCargoResponse);
        response.setContainerDetails(containerResponse);
        response.setCargoDescription(cargoDescResponse);
        response.setCargoMarks(cargoMarksResponse);
        response.setSimpleCargoDescription(simpleCargoDescResponse);
        response.setSimpleCargoMarks(simpleCargoMarksResponse);
        response.setChargeDetails(chargeResponse);

        return response;
    }

    // ========== Simple single-string join helpers ==========

    /** Joins raw description strings fetched via native query — most reliable approach. */
    private String joinDescriptionStrings(List<String> rows) {
        if (rows == null || rows.isEmpty()) return null;
        return rows.stream()
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(" "));
    }

    /** Joins DTO list — used when the list is already fetched (e.g. getCargoContainerDetails). */
    private String joinCargoDescriptions(List<CargoDescriptionDto> list) {
        if (list == null || list.isEmpty()) return null;
        return list.stream()
                .map(CargoDescriptionDto::getCargoDescription)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private String joinCargoMarks(List<CargoMarksDto> list) {
        if (list == null || list.isEmpty()) return null;
        return list.stream()
                .map(CargoMarksDto::getCargoDescription)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(" "));
    }

    // ========== Simple single-string save helpers ==========

    private String saveSimpleCargoDescription(Long transactionPoid, String text) {
        log.info("Saving simple cargo description for Export BL: {}", transactionPoid);
        cargoDtlRepository.deleteAllByTransactionPoidAndDescriptionType(transactionPoid, "DESC");
        if (text != null && !text.trim().isEmpty()) {
            ExportShipBlManifestCargoDtl entity = new ExportShipBlManifestCargoDtl();
            entity.setTransactionPoid(transactionPoid);
            entity.setDetRowId(1L);
            entity.setDescriptionType("DESC");
            entity.setCargoDescription(text);
            cargoDtlRepository.save(entity);
        }
        return text;
    }

    private String saveSimpleCargoMarks(Long transactionPoid, String text) {
        log.info("Saving simple cargo marks for Export BL: {}", transactionPoid);
        cargoDtlRepository.deleteAllByTransactionPoidAndDescriptionType(transactionPoid, "MARK");
        if (text != null && !text.trim().isEmpty()) {
            ExportShipBlManifestCargoDtl entity = new ExportShipBlManifestCargoDtl();
            entity.setTransactionPoid(transactionPoid);
            entity.setDetRowId(1L);
            entity.setDescriptionType("MARK");
            entity.setCargoDescription(text);
            cargoDtlRepository.save(entity);
        }
        return text;
    }

    @Override
    public void deleteExportBl(Long transactionPoid) {
        log.info("Deleting Export BL with ID: {}", transactionPoid);
        
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        
        ExportShipBlManifestHdr entity = hdrRepository
                .findExportBlByTransactionPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new RuntimeException("Export BL not found with ID: " + transactionPoid));
        
        entity.setDeleted("Y");
        hdrRepository.save(entity);
        
        log.info("Successfully deleted Export BL with ID: {}", transactionPoid);
    }

    // ========== General Cargo Details Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<GeneralCargoDetailDto> getGeneralCargoDetails(Long transactionPoid) {
        log.info("Getting general cargo details for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        List<ExportShipBlManifestGeneralDtl> entities = generalDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        List<GeneralCargoDetailDto> dtos = mapper.mapGeneralCargoListToDto(entities);
        enrichGeneralCargoDetailsWithLovData(dtos);
        return dtos;
    }

    @Override
    public List<GeneralCargoDetailDto> updateGeneralCargoDetails(Long transactionPoid, List<GeneralCargoDetailDto> request) {
        log.info("Updating general cargo details for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        String userId = getCurrentUser();
        
        if (request != null) {
            List<Long> deleteIds = new ArrayList<>();
            for (GeneralCargoDetailDto dto : request) {
                ActionType action = resolveAction(dto.getActionType());
                Long detRowId = dto.getDetRowId();

                switch (action) {
                    case ISDELETED:
                        if (detRowId != null) deleteIds.add(detRowId);
                        break;
                    case ISCREATED:
                        if (dto.getComodityPoid() == null && (dto.getCargoDescription() == null || dto.getCargoDescription().isBlank())) break;
                        detRowId = generalDtlRepository.getNextDetRowId(transactionPoid);
                        ExportShipBlManifestGeneralDtl newEntity = mapper.mapGeneralCargoToEntity(dto, transactionPoid, detRowId, userId);
                        generalDtlRepository.save(newEntity);
                        break;
                    case ISUPDATED:
                        if (detRowId != null) {
                            ExportShipBlManifestGeneralDtl entity = generalDtlRepository.findById(new ExportShipBlManifestGeneralDtlId(transactionPoid, detRowId))
                                .orElseThrow(() -> new RuntimeException("General cargo detail not found"));
                            if (dto.getComodityPoid() != null) entity.setComodityPoid(dto.getComodityPoid());
                            if (dto.getCargoDescription() != null) entity.setCargoDescription(dto.getCargoDescription());
                            if (dto.getQuantity() != null) entity.setQuantity(dto.getQuantity());
                            if (dto.getGrsVolume() != null) entity.setGrsVolume(dto.getGrsVolume());
                            if (dto.getGrsWeight() != null) entity.setGrsWeight(dto.getGrsWeight());
                            if (dto.getNetVolume() != null) entity.setNetVolume(dto.getNetVolume());
                            if (dto.getNetWeight() != null) entity.setNetWeight(dto.getNetWeight());
                            if (dto.getTareWeight() != null) entity.setTareWeight(dto.getTareWeight());
                            if (dto.getNoOfPacks() != null) entity.setNoOfPacks(dto.getNoOfPacks());
                            if (dto.getPackUnit() != null) entity.setPackUnit(dto.getPackUnit());
                            if (dto.getDestinationPortPoid() != null) entity.setDestinationPortPoid(dto.getDestinationPortPoid());
                            generalDtlRepository.save(entity);
                        }
                        break;
                }
            }
            if (!deleteIds.isEmpty()) {
                generalDtlRepository.deleteByTransactionPoidAndDetRowIds(transactionPoid, deleteIds);
            }
        }
        recalculateHeaderTotals(transactionPoid);
        return getGeneralCargoDetails(transactionPoid);
    }

    // ========== Container Details Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<ContainerDetailDto> getContainerDetails(Long transactionPoid) {
        log.info("Getting container details for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        List<ExportShipBlManifestContainerDtl> entities = containerDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        List<ContainerDetailDto> dtos = mapper.mapContainerListToDto(entities);
        enrichContainerDetailsWithLovData(dtos);
        return dtos;
    }

    @Override
    public List<ContainerDetailDto> updateContainerDetails(Long transactionPoid, List<ContainerDetailDto> request) {
        log.info("Updating container details for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        String userId = getCurrentUser();
        
        if (request != null) {
            List<Long> deleteIds = new ArrayList<>();
            for (ContainerDetailDto dto : request) {
                ActionType action = resolveAction(dto.getActionType());
                Long detRowId = dto.getDetRowId();

                switch (action) {
                    case ISDELETED:
                        if (detRowId != null) deleteIds.add(detRowId);
                        break;
                    case ISCREATED:
                        detRowId = containerDtlRepository.getNextDetRowId(transactionPoid);
                        ExportShipBlManifestContainerDtl newEntity = mapper.mapContainerToEntity(dto, transactionPoid, detRowId, userId);
                        containerDtlRepository.save(newEntity);
                        break;
                    case ISUPDATED:
                        if (detRowId != null) {
                            ExportShipBlManifestContainerDtl entity = containerDtlRepository.findById(new ExportShipBlManifestContainerDtlId(transactionPoid, detRowId))
                                .orElseThrow(() -> new RuntimeException("Container detail not found"));
                            if (dto.getContainerNo() != null) entity.setContainerNo(dto.getContainerNo());
                            if (dto.getEquipmentIsoType() != null) entity.setEquipmentIsoType(dto.getEquipmentIsoType());
                            if (dto.getEquipmentSealNo() != null) entity.setEquipmentSealNo(dto.getEquipmentSealNo());
                            if (dto.getComodityPoid() != null) entity.setComodityPoid(dto.getComodityPoid());
                            if (dto.getQuantity() != null) entity.setQuantity(dto.getQuantity());
                            if (dto.getGrsVolume() != null) entity.setGrsVolume(dto.getGrsVolume());
                            if (dto.getGrsWeight() != null) entity.setGrsWeight(dto.getGrsWeight());
                            if (dto.getNetVolume() != null) entity.setNetVolume(dto.getNetVolume());
                            if (dto.getNetWeight() != null) entity.setNetWeight(dto.getNetWeight());
                            if (dto.getTareWeight() != null) entity.setTareWeight(dto.getTareWeight());
                            if (dto.getNoOfPacks() != null) entity.setNoOfPacks(dto.getNoOfPacks());
                            if (dto.getPackUnit() != null) entity.setPackUnit(dto.getPackUnit());
                            if (dto.getDestinationPortPoid() != null) entity.setDestinationPortPoid(dto.getDestinationPortPoid());
                            containerDtlRepository.save(entity);
                        }
                        break;
                }
            }
            if (!deleteIds.isEmpty()) {
                containerDtlRepository.deleteByTransactionPoidAndDetRowIds(transactionPoid, deleteIds);
            }
        }
        recalculateHeaderTotals(transactionPoid);
        return getContainerDetails(transactionPoid);
    }

    // ========== Cargo Description and Marks Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<CargoDescriptionDto> getCargoDescription(Long transactionPoid) {
        log.info("Getting cargo description for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        List<ExportShipBlManifestCargoDtl> entities = cargoDtlRepository.findCargoRowsByType(transactionPoid, "DESC");
        return mapper.mapCargoDescriptionListToDto(entities);
    }

    @Override
    public List<CargoDescriptionDto> updateCargoDescription(Long transactionPoid, List<CargoDescriptionDto> request) {
        log.info("Updating cargo description for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        String userId = getCurrentUser();
        
        if (request != null) {
            List<Long> deleteIds = new ArrayList<>();
            for (CargoDescriptionDto dto : request) {
                ActionType action = resolveAction(dto.getActionType());
                Long detRowId = dto.getDetRowId();

                switch (action) {
                    case ISDELETED:
                        if (detRowId != null) deleteIds.add(detRowId);
                        break;
                    case ISCREATED:
                        detRowId = cargoDtlRepository.getNextDetRowId(transactionPoid, "DESC");
                        ExportShipBlManifestCargoDtl newEntity = mapper.mapCargoDescriptionToEntity(dto, transactionPoid, detRowId, userId);
                        newEntity.setDescriptionType("DESC");
                        cargoDtlRepository.save(newEntity);
                        break;
                    case ISUPDATED:
                        if (detRowId != null) {
                            ExportShipBlManifestCargoDtl entity = cargoDtlRepository.findById(new ExportShipBlManifestCargoDtlId(transactionPoid, detRowId, "DESC"))
                                .orElseThrow(() -> new RuntimeException("Cargo description detail not found"));
                            if (dto.getCargoDescription() != null) entity.setCargoDescription(dto.getCargoDescription());
                            cargoDtlRepository.save(entity);
                        }
                        break;
                }
            }
            if (!deleteIds.isEmpty()) {
                cargoDtlRepository.deleteByTransactionPoidAndDescriptionTypeAndDetRowIds(transactionPoid, "DESC", deleteIds);
            }
        }
        return getCargoDescription(transactionPoid);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportManifestCargoContainerResponse getCargoContainerDetails(Long transactionPoid) {
        log.info("Getting cargo and container details for Export BL: {}", transactionPoid);

        List<CargoDescriptionDto> descList = getCargoDescription(transactionPoid);
        List<CargoMarksDto> marksList = getCargoMarks(transactionPoid);

        ExportManifestCargoContainerResponse response = new ExportManifestCargoContainerResponse();
        response.setCargoDescription(descList);
        response.setCargoMarks(marksList);
        response.setContainerDetails(getContainerDetails(transactionPoid));

        // Simple single-string view: native query on string column — most reliable
        response.setSimpleCargoDescription(joinDescriptionStrings(
                cargoDtlRepository.findDescriptionStringsByType(transactionPoid, "DESC")));
        response.setSimpleCargoMarks(joinDescriptionStrings(
                cargoDtlRepository.findDescriptionStringsByType(transactionPoid, "MARK")));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CargoMarksDto> getCargoMarks(Long transactionPoid) {
        log.info("Getting cargo marks for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        List<ExportShipBlManifestCargoDtl> entities = cargoDtlRepository.findCargoRowsByType(transactionPoid, "MARK");
        return mapper.mapCargoMarksListToDto(entities);
    }

    @Override
    public List<CargoMarksDto> updateCargoMarks(Long transactionPoid, List<CargoMarksDto> request) {
        log.info("Updating cargo marks for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        String userId = getCurrentUser();
        
        if (request != null) {
            List<Long> deleteIds = new ArrayList<>();
            for (CargoMarksDto dto : request) {
                ActionType action = resolveAction(dto.getActionType());
                Long detRowId = dto.getDetRowId();

                switch (action) {
                    case ISDELETED:
                        if (detRowId != null) deleteIds.add(detRowId);
                        break;
                    case ISCREATED:
                        detRowId = cargoDtlRepository.getNextDetRowId(transactionPoid, "MARK");
                        ExportShipBlManifestCargoDtl newEntity = mapper.mapCargoMarksToEntity(dto, transactionPoid, detRowId, userId);
                        newEntity.setDescriptionType("MARK");
                        cargoDtlRepository.save(newEntity);
                        break;
                    case ISUPDATED:
                        if (detRowId != null) {
                            ExportShipBlManifestCargoDtl entity = cargoDtlRepository.findById(new ExportShipBlManifestCargoDtlId(transactionPoid, detRowId, "MARK"))
                                .orElseThrow(() -> new RuntimeException("Cargo marks detail not found"));
                            if (dto.getCargoDescription() != null) entity.setCargoDescription(dto.getCargoDescription());
                            cargoDtlRepository.save(entity);
                        }
                        break;
                }
            }
            if (!deleteIds.isEmpty()) {
                cargoDtlRepository.deleteByTransactionPoidAndDescriptionTypeAndDetRowIds(transactionPoid, "MARK", deleteIds);
            }
        }
        return getCargoMarks(transactionPoid);
    }

    // ========== Charge Details Operations ==========

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getChargeDetails(Long transactionPoid) {
        log.info("Getting charge details for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        List<ExportShipBlManifestChargesDtl> entities = chargesDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        List<ChargeDetailDto> dtos = mapper.mapChargeListToDto(entities);
        enrichChargeDetailsWithLovData(dtos);
        
        Map<String, Object> response = new HashMap<>();
        response.put("chargeDetails", dtos);
        
        BigDecimal totalBuyAmount = BigDecimal.ZERO;
        BigDecimal totalSaleAmount = BigDecimal.ZERO;
        
        for (ChargeDetailDto dto : dtos) {
            if (dto.getBuyAmount() != null) totalBuyAmount = totalBuyAmount.add(dto.getBuyAmount());
            if (dto.getSaleAmount() != null) totalSaleAmount = totalSaleAmount.add(dto.getSaleAmount());
        }
        
        response.put("totalBuyAmount", totalBuyAmount);
        response.put("totalSaleAmount", totalSaleAmount);
        
        return response;
    }

    @Override
    public Map<String, Object> updateChargeDetails(Long transactionPoid, List<ChargeDetailDto> request) {
        log.info("Updating charge details for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        String userId = getCurrentUser();
        
        if (request != null) {
            List<Long> deleteIds = new ArrayList<>();
            for (ChargeDetailDto dto : request) {
                ActionType action = resolveAction(dto.getActionType());
                Long detRowId = dto.getDetRowId();

                switch (action) {
                    case ISDELETED:
                        if (detRowId != null) deleteIds.add(detRowId);
                        break;
                    case ISCREATED:
                        if (dto.getChargePoid() == null) break;
                        detRowId = chargesDtlRepository.getNextDetRowId(transactionPoid);
                        ExportShipBlManifestChargesDtl newEntity = mapper.mapChargeToEntity(dto, transactionPoid, detRowId, userId);
                        chargesDtlRepository.save(newEntity);
                        break;
                    case ISUPDATED:
                        if (detRowId != null) {
                            ExportShipBlManifestChargesDtl entity = chargesDtlRepository.findById(new ExportShipBlManifestChargesDtlId(transactionPoid, detRowId))
                                .orElseThrow(() -> new RuntimeException("Charge detail not found"));
                            
                            if (dto.getChargePoid() != null) entity.setChargePoid(dto.getChargePoid());
                            if (dto.getChargeType() != null) entity.setChargeType(dto.getChargeType());
                            if (dto.getFreightType() != null) entity.setFreightType(dto.getFreightType());
                            if (dto.getChargeBasisOn() != null) entity.setChargeBasisOn(dto.getChargeBasisOn());
                            if (dto.getCurrencyCode() != null) entity.setCurrencyCode(dto.getCurrencyCode());
                            if (dto.getCurrencyExchange() != null) entity.setCurrencyExchange(dto.getCurrencyExchange());
                            if (dto.getBuyPercharge() != null) entity.setBuyPercharge(dto.getBuyPercharge());
                            if (dto.getPerQuantityAmount() != null) entity.setPerQuantityAmount(dto.getPerQuantityAmount());
                            if (dto.getQuantity() != null) entity.setQuantity(dto.getQuantity());
                            if (dto.getPaidAtPortPoid() != null) entity.setPaidAtPortPoid(dto.getPaidAtPortPoid());
                            if (dto.getReceiptInvoicePoid() != null) entity.setReceiptInvoicePoid(dto.getReceiptInvoicePoid());
                            if (dto.getTaxPoid() != null) entity.setTaxPoid(dto.getTaxPoid());
                            if (dto.getTaxPercentage() != null) entity.setTaxPercentage(dto.getTaxPercentage());
                            if (dto.getTaxAmount() != null) entity.setTaxAmount(dto.getTaxAmount());
                            
                            chargesDtlRepository.save(entity);
                        }
                        break;
                }
            }
            if (!deleteIds.isEmpty()) {
                chargesDtlRepository.deleteByTransactionPoidAndDetRowIds(transactionPoid, deleteIds);
            }
        }
        recalculateHeaderTotals(transactionPoid);
        return getChargeDetails(transactionPoid);
    }

    private void recalculateHeaderTotals(Long transactionPoid) {
        ExportShipBlManifestHdr header = hdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new RuntimeException("Header not found"));
        
        // Calculate totals from general cargo details
        Object[] generalTotals = generalDtlRepository.calculateTotals(transactionPoid);
        if (generalTotals != null && generalTotals.length >= 5) {
            header.setTotalVolume((BigDecimal) generalTotals[0]);
            header.setTotalNetVolume((BigDecimal) generalTotals[1]);
            header.setTotalWeight((BigDecimal) generalTotals[2]);
            header.setTotalNetWeight((BigDecimal) generalTotals[3]);
            header.setTotalNoOfPacks((BigDecimal) generalTotals[4]);
        }
        
        // Also consider container totals if needed
        Object[] containerTotals = containerDtlRepository.calculateTotals(transactionPoid);
        if (containerTotals != null && containerTotals.length >= 5) {
            // Merge with general totals or use container totals
        }
        
        hdrRepository.save(header);
    }

    // ========== Special Operations (to be continued in next part) ==========

    @Override
    public Map<String, Object> loadBooking(Long transactionPoid, LoadBookingRequest request) {
        // TODO: Implement load booking from MATE
        throw new UnsupportedOperationException("Load booking not yet implemented");
    }

    @Override
    public byte[] generateBlPrint(Long transactionPoid, GenerateBlPrintRequest request,String docId) throws Exception {
    	
    	log.info("Bl print : {}", transactionPoid);
    	
    	Long groupPoid=UserContext.getGroupPoid();
    	Long companyPoid=UserContext.getCompanyPoid();
    	ExportShipBlManifestHdr entity = hdrRepository
                .findExportBlByTransactionPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new RuntimeException("Export BL not found with ID: " + transactionPoid));
    	if(entity.getBlOrginalPrint()!=null && entity.getBlOrginalPrint().equalsIgnoreCase("Y")) {
    		throw new RuntimeException("BL already printed");
    	}
    	
    	String jrxmlFile= customBLRepository.getBlPrintReport(groupPoid, companyPoid, docId, transactionPoid, "BL_PRINT");
    	Map<String, Object> params = printService.buildBaseParams(transactionPoid, "100-140");
		JasperReport mainReport = printService.load("Shipping/"+jrxmlFile);
		params.put("DRAFT_ORIGINAL", request.getDraftOriginal());
		return printService.fillReportToPdf(mainReport, params, dataSource);
    	
    }

    @Override
    public byte[] generateManifest(Long transactionPoid, GenerateManifestRequest request, String docId) throws Exception {
    	Map<String, Object> params = printService.buildBaseParams(transactionPoid, docId);
		params.put("P_FREIGHTCARGO", request.getFreightCargo().toString().toUpperCase());
        params.put("SUBREPORT_MARK_INFO", printService.load("Shipping/SH/Cargo/Mark_Info_Subreport1.jrxml"));
	    params.put("SUBREPORT_CONTAINER_INFO", printService.load("Shipping/SH/Cargo/Container_Info_Subreport1.jrxml"));
	    params.put("SUBREPORT_DESCRIPTION_INFO", printService.load("Shipping/SH/Cargo/Description_Info_Subreport1.jrxml"));
	    params.put("SUBREPORT_FREIGHT_DETAIL", printService.load("Shipping/SH/Cargo/Freight_Detail_Subreport1.jrxml"));
	    JasperReport mainReport = printService.load("Shipping/SH/Cargo/Manifest_Cargo_WithCharges.jrxml");
	    return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public byte[] generateDetentionStorage(Long transactionPoid,String docId) throws Exception {
    	Map<String, Object> params = printService.buildBaseParams(transactionPoid, docId);
		JasperReport mainReport = printService.load("Shipping/SH/Container_Detention_details.jrxml");
		return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public void exportEdi(Long transactionPoid) {
        log.info("Exporting EDI for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        Long userPoid = UserContext.getUserPoid();
        
        customBLRepository.exportEdi(transactionPoid, userPoid);
    }

    @Override
    public ValidationResponse validate(Long transactionPoid, ExportManifestBlRequest request) {
        ValidationResponse response = new ValidationResponse();
        
        // Validate required fields
        if (request.getVoyageTransactionPoid() == null) {
            response.addError("Voyage Transaction POID is required");
        }
        if (request.getSalesmanPoid() == null) {
            response.addError("Salesman POID is required");
        }
        
        // Validate BL number uniqueness if provided
        if (request.getBlNumber() != null && !request.getBlNumber().trim().isEmpty()) {
            String oldBlNumber = transactionPoid != null ? 
                    hdrRepository.findById(transactionPoid).map(ExportShipBlManifestHdr::getBlNumber).orElse(null) : null;
            String status = customBLRepository.validateBlNumberDuplicate(
                    request.getBlNumber().trim(), oldBlNumber != null ? oldBlNumber : "NEWRECORD", 
                    transactionPoid != null ? "UPDATING" : "INSERTING");
            if (status == null || !status.startsWith("SUCCESS")) {
                response.addError("BL number already exists: " + request.getBlNumber());
            }
        }
        
        return response;
    }

    @Override
    public void afterSave(Long transactionPoid) {
        log.info("Processing after save for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();
        
        customBLRepository.processAfterSave(groupPoid, companyPoid, transactionPoid, null, "AUTOSUMWEIGHTPEXPORT", userPoid);
    }

    @Override
    public BlStatusResponse getBlStatus(Long transactionPoid) {
        log.info("Getting BL status for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();
        
        String status = customBLRepository.getBlStatus(groupPoid, companyPoid, userPoid, transactionPoid);
        
        BlStatusResponse response = new BlStatusResponse();
        response.setStatus(status);
        response.setDisplayInfo(status); // Can be enhanced with more detailed status info
        
        return response;
    }

    @Override
    public Map<String, Object> quotationAfterBrowse(Long transactionPoid, QuotationAfterBrowseRequest request) {
        log.info("Processing quotation after browse for Export BL: {}", transactionPoid);
        
        Long actualTransactionPoid = transactionPoid != null ? transactionPoid : 0L; // Use 0 for new records
        
        customBLRepository.processQuotationAfterBrowse(
                getGroupPoid(),
                getCompanyPoid(),
                getUserPoid(),
                DOC_ID,
                actualTransactionPoid,
                "SHIP_QUOTATION_EXPORT",
                request.getQuotationTransactionPoid()
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("fieldsUpdated", Arrays.asList("salesmanPoid", "portOfLoadingPoid", "portOfDischargePoid"));
        
        return result;
    }

    // ========== LOV Enrichment Methods ==========

    /**
     * Enrich header DTO with LOV data for all POID fields
     */
    private void enrichHeaderWithLovData(ExportManifestBlResponse dto, ExportShipBlManifestHdr entity) {
        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        Long userPoid = getUserPoid();

        try {
            // Voyage
            if (entity.getVoyageTransactionPoid() != null) {
                dto.setVoyageTransactionDet(lovService.getLovItemByPoid(
                        entity.getVoyageTransactionPoid(), "VESSAL_VOYAGE", groupPoid, companyPoid, userPoid));
            }

            // Quotation
            if (entity.getQuotationTransactionPoid() != null) {
                dto.setQuotationTransactionDet(lovService.getLovItemByPoid(
                        entity.getQuotationTransactionPoid(), "SHIP_QUOTATION_EXPORT", groupPoid, companyPoid, userPoid));
            }

            // Salesman
            if (entity.getSalesmanPoid() != null) {
                dto.setSalesmanDet(lovService.getLovItemByPoid(
                        entity.getSalesmanPoid(), "SALESMAN", groupPoid, companyPoid, userPoid));
            }

            // Commodity
            if (entity.getComodityPoid() != null) {
                dto.setComodityDet(lovService.getLovItemByPoid(
                        entity.getComodityPoid(), "COMODITY", groupPoid, companyPoid, userPoid));
            }

            // Ports
            if (entity.getPlaceOfIssuePoid() != null) {
                dto.setPlaceOfIssueDet(lovService.getLovItemByPoid(
                        entity.getPlaceOfIssuePoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getPlaceOfRecieptPoid() != null) {
                dto.setPlaceOfRecieptDet(lovService.getLovItemByPoid(
                        entity.getPlaceOfRecieptPoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getPlaceOfDelieveryPoid() != null) {
                dto.setPlaceOfDelieveryDet(lovService.getLovItemByPoid(
                        entity.getPlaceOfDelieveryPoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getPortOfLoadingPoid() != null) {
                dto.setPortOfLoadingDet(lovService.getLovItemByPoid(
                        entity.getPortOfLoadingPoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getPortOfDischargePoid() != null) {
                dto.setPortOfDischargeDet(lovService.getLovItemByPoid(
                        entity.getPortOfDischargePoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
            }

            // Customers (Shipper, Consignee, Notify, Booking Party)
            if (entity.getShipperPoid() != null) {
                dto.setShipperDet(lovService.getLovItemByPoid(
                        entity.getShipperPoid().longValue(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getShipperAddressPoid() != null) {
                dto.setShipperAddressDet(lovService.getLovItemByPoid(
                        entity.getShipperAddressPoid(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getConsigneePoid() != null) {
                dto.setConsigneeDet(lovService.getLovItemByPoid(
                        entity.getConsigneePoid(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getConsigneeAddressPoid() != null) {
                dto.setConsigneeAddressDet(lovService.getLovItemByPoid(
                        entity.getConsigneeAddressPoid(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getNotifyPoid1() != null) {
                dto.setNotify1Det(lovService.getLovItemByPoid(
                        entity.getNotifyPoid1(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getNotifyAddressPoid1() != null) {
                dto.setNotify1AddressDet(lovService.getLovItemByPoid(
                        entity.getNotifyAddressPoid1(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getNotifyPoid2() != null) {
                dto.setNotify2Det(lovService.getLovItemByPoid(
                        entity.getNotifyPoid2(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getNotifyAddressPoid2() != null) {
                dto.setNotify2AddressDet(lovService.getLovItemByPoid(
                        entity.getNotifyAddressPoid2(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getNotifyPoid3() != null) {
                dto.setNotify3Det(lovService.getLovItemByPoid(
                        entity.getNotifyPoid3(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getNotifyAddressPoid3() != null) {
                dto.setNotify3AddressDet(lovService.getLovItemByPoid(
                        entity.getNotifyAddressPoid3(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getBookingPartyPoid() != null) {
                dto.setBookingPartyDet(lovService.getLovItemByPoid(
                        entity.getBookingPartyPoid(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getCanNotifyCustomerPoid() != null) {
                dto.setCanNotifyCustomerDet(lovService.getLovItemByPoid(
                        entity.getCanNotifyCustomerPoid(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }

            // Company and Division
            if (entity.getDocumentCompanyPoid() != null) {
                dto.setDocumentCompanyDet(lovService.getLovItemByPoid(
                        entity.getDocumentCompanyPoid(), "COMPANY", groupPoid, companyPoid, userPoid));
            }
            if (entity.getDocumentCompanyDivisionPoid() != null) {
                dto.setDocumentCompanyDivisionDet(lovService.getLovItemByPoid(
                        entity.getDocumentCompanyDivisionPoid(), "SHIP_DIVISION_PRINT", groupPoid, companyPoid, userPoid));
            }

            // Agent
            if (entity.getAgentPoid() != null) {
                try {
                    dto.setAgentDet(lovService.getLovItemByPoid(
                            entity.getAgentPoid(), "AGENT_MASTER", groupPoid, companyPoid, userPoid));
                } catch (Exception e) {
                    log.warn("Failed to fetch AGENT_MASTER LOV for agentPoid: {}", entity.getAgentPoid(), e);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch some LOV data for header", e);
        }
    }

    /**
     * Helper to fetch LOV item using an in-request cache map to avoid duplicate calls.
     */
    private LovItem getLovItemWithCache(Map<String, Map<Long, LovItem>> cache, Long poid, String masterType, Long groupPoid, Long companyPoid, Long userPoid) {
        if (poid == null) {
            return null;
        }
        Map<Long, LovItem> innerCache = cache.computeIfAbsent(masterType, k -> new HashMap<>());
        if (!innerCache.containsKey(poid)) {
            try {
                LovItem item = lovService.getLovItemByPoid(poid, masterType, groupPoid, companyPoid, userPoid);
                innerCache.put(poid, item);
            } catch (Exception e) {
                log.warn("Failed to fetch LOV for masterType: {} and poid: {}", masterType, poid, e);
                innerCache.put(poid, null);
            }
        }
        return innerCache.get(poid);
    }

    /**
     * Helper to fetch LOV item by code using an in-request cache map to avoid duplicate calls.
     */
    private LovItem getLovItemByCodeWithCache(Map<String, Map<String, LovItem>> cache, String code, String lovName, Long groupPoid, Long companyPoid, Long userPoid) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        Map<String, LovItem> innerCache = cache.computeIfAbsent(lovName, k -> new HashMap<>());
        if (!innerCache.containsKey(code)) {
            try {
                LovItem item = lovService.getLovItemByCode(code, lovName, groupPoid, companyPoid, userPoid);
                innerCache.put(code, item);
            } catch (Exception e) {
                log.warn("Failed to fetch LOV for lovName: {} and code: {}", lovName, code, e);
                innerCache.put(code, null);
            }
        }
        return innerCache.get(code);
    }

    /**
     * Enrich general cargo details DTOs with LOV data
     */
    private void enrichGeneralCargoDetailsWithLovData(List<GeneralCargoDetailDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        Long userPoid = getUserPoid();
        Map<String, Map<Long, LovItem>> cache = new HashMap<>();

        for (GeneralCargoDetailDto dto : dtos) {
            try {
                if (dto.getComodityPoid() != null) {
                    dto.setComodityDet(getLovItemWithCache(cache, dto.getComodityPoid(), "COMODITY", groupPoid, companyPoid, userPoid));
                }
                if (dto.getDestinationPortPoid() != null) {
                    dto.setDestinationPortDet(getLovItemWithCache(cache, dto.getDestinationPortPoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
                }
            } catch (Exception e) {
                log.warn("Failed to fetch LOV data for general cargo detail with detRowId: {}", dto.getDetRowId(), e);
            }
        }
    }

    /**
     * Enrich container details DTOs with LOV data
     */
    private void enrichContainerDetailsWithLovData(List<ContainerDetailDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        Long userPoid = getUserPoid();
        Map<String, Map<Long, LovItem>> poidCache = new HashMap<>();
        Map<String, Map<String, LovItem>> codeCache = new HashMap<>();

        for (ContainerDetailDto dto : dtos) {
            try {
                // POID-based LOVs
                if (dto.getComodityPoid() != null) {
                    dto.setComodityDet(getLovItemWithCache(poidCache, dto.getComodityPoid(), "COMODITY", groupPoid, companyPoid, userPoid));
                }
                if (dto.getDestinationPortPoid() != null) {
                    dto.setDestinationPortDet(getLovItemWithCache(poidCache, dto.getDestinationPortPoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
                }
                // CODE-based LOVs
                if (dto.getEquipmentIsoType() != null) {
                    dto.setEquipmentIsoTypeDet(getLovItemByCodeWithCache(codeCache, dto.getEquipmentIsoType(), "CONTAINER_TYPE_MASTER", groupPoid, companyPoid, userPoid));
                }
                if (dto.getImcoClassType() != null) {
                    dto.setImcoClassTypeDet(getLovItemByCodeWithCache(codeCache, dto.getImcoClassType(), "IMCO_CLASS", groupPoid, companyPoid, userPoid));
                }
                if (dto.getOogType() != null) {
                    dto.setOogTypeDet(getLovItemByCodeWithCache(codeCache, dto.getOogType(), "OOG_TYPE", groupPoid, companyPoid, userPoid));
                }
            } catch (Exception e) {
                log.warn("Failed to fetch LOV data for container detail with detRowId: {}", dto.getDetRowId(), e);
            }
        }
    }

    /**
     * Enrich charge details DTOs with LOV data
     */
    private void enrichChargeDetailsWithLovData(List<ChargeDetailDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        Long userPoid = getUserPoid();
        Map<String, Map<Long, LovItem>> cache = new HashMap<>();
        Map<String, Map<String, LovItem>> codeCache = new HashMap<>();

        for (ChargeDetailDto dto : dtos) {
            try {
                if (dto.getChargePoid() != null) {
                    dto.setChargeDet(getLovItemWithCache(cache, dto.getChargePoid(), "CHARGE_MASTER", groupPoid, companyPoid, userPoid));
                }
                if (dto.getPaidAtPortPoid() != null) {
                    dto.setPaidAtPortDet(getLovItemWithCache(cache, dto.getPaidAtPortPoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
                }
                if (dto.getReceiptInvoicePoid() != null) {
                    dto.setReceiptInvoiceDet(getLovItemWithCache(cache, dto.getReceiptInvoicePoid(), "MANIFEST_RECEIPT_INVOICE", groupPoid, companyPoid, userPoid));
                }
                if (dto.getTaxPoid() != null) {
                    dto.setTaxDet(getLovItemWithCache(cache, dto.getTaxPoid(), "TAX_MASTER", groupPoid, companyPoid, userPoid));
                }
                if (dto.getChargeType() != null) {
                    dto.setChargeTypeDet(getLovItemByCodeWithCache(codeCache, dto.getChargeType(), "CHARGE_TYPE", groupPoid, companyPoid, userPoid));
                }
                if (dto.getFreightType() != null) {
                    dto.setFreightTypeDet(getLovItemByCodeWithCache(codeCache, dto.getFreightType(), "SHIP_FREIGHT_TYPE", groupPoid, companyPoid, userPoid));
                }
                if (dto.getChargeBasisOn() != null) {
                    dto.setChargeBasisOnDet(getLovItemByCodeWithCache(codeCache, dto.getChargeBasisOn(), "CONTAINER_TYPE_MASTER", groupPoid, companyPoid, userPoid));
                }
            } catch (Exception e) {
                log.warn("Failed to fetch LOV data for charge detail with detRowId: {}", dto.getDetRowId(), e);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ExportManifestAddressDto getAddressDetails(Long addressMasterPoid, String addressType) {
        log.info("Fetching address details for addressMasterPoid: {} and addressType: {}", addressMasterPoid, addressType);
        
        List<AddressDetails> addressList = addressDetailsRepository.findByAddressMasterPoidAndAddressType(addressMasterPoid, addressType);
        if (addressList == null || addressList.isEmpty()) {
            if (addressType != null && !addressType.equalsIgnoreCase("MAIN")) {
                log.info("Address of type {} not found. Falling back to MAIN.", addressType);
                addressList = addressDetailsRepository.findByAddressMasterPoidAndAddressType(addressMasterPoid, "MAIN");
            }
        }
        
        if (addressList == null || addressList.isEmpty()) {
            log.info("Address of type MAIN not found. Fetching any address for master poid.");
            addressList = addressDetailsRepository.findAll().stream()
                    .filter(addr -> addressMasterPoid.equals(addr.getAddressMasterPoid()))
                    .collect(Collectors.toList());
        }
        
        if (addressList == null || addressList.isEmpty()) {
            throw new RuntimeException("Address details not found for master poid: " + addressMasterPoid);
        }
        
        return mapper.mapAddressToDto(addressList.getFirst());
    }
}

