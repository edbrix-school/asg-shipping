package com.asg.shipping.portstoragetariffsmaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.portstoragetariffsmaster.dto.*;
import com.asg.shipping.portstoragetariffsmaster.entity.ShipPortTariffDtl;
import com.asg.shipping.portstoragetariffsmaster.entity.ShipPortTariffDtlId;
import com.asg.shipping.portstoragetariffsmaster.entity.ShipPortTariffHdr;
import com.asg.shipping.portstoragetariffsmaster.repository.ShipPortTariffDtlRepository;
import com.asg.shipping.portstoragetariffsmaster.repository.ShipPortTariffHdrRepository;
import com.asg.shipping.portstoragetariffsmaster.util.PortStorageTariffMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.asg.common.lib.dto.request.LogRequestDto;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Service implementation for Port Storage Tariffs operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortStorageTariffsServiceImpl implements PortStorageTariffsService {

    private final ShipPortTariffHdrRepository tariffHdrRepository;
    private final ShipPortTariffDtlRepository tariffDtlRepository;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final LovDataService lovService;
    private final PortStorageTariffMapper mapper;
    private final LoggingService loggingService;

    private static final String TRANSACTION_POID = "TRANSACTION_POID";
    private static final String TRANSACTIONPOID  = "transactionPoid";
    private static final String TARIFF            = "Tariff";

    private static final String ACTION_ISCREATED  = "ACTION_ISCREATED";
    private static final String ACTION_ISUPDATED  = "ACTION_ISUPDATED";
    private static final String ACTION_ISDELETED  = "ACTION_ISDELETED";
    private static final String ACTION_NOCHANGES  = "ACTION_NOCHANGES";

    private static final String LOG_KEY_ID_FORMAT = "KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s";

    private static final String SLAB1 = "Slab 1";
    private static final String SLAB2 = "Slab 2";
    private static final String SLAB3 = "Slab 3";
    private static final String SLAB4 = "Slab 4";
    private static final String SLAB5 = "Slab 5";
    private static final String SLAB6 = "Slab 6";
    private static final String SLAB7 = "Slab 7";


    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchTariffs(String docId, FilterRequestDto request,
                                             LocalDate periodFrom, LocalDate periodTo,
                                             Pageable pageable) {
        log.info("Searching tariffs with docId: {}, page: {}, size: {}, periodFrom: {}, periodTo: {}",
                docId, pageable.getPageNumber(), pageable.getPageSize(), periodFrom, periodTo);

        // Resolve filter components from FilterRequestDto
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = new ArrayList<>(documentService.resolveFilters(request));
        applyPeriodRangeFilters(filters, periodFrom, periodTo);

        // Call documentService.search with docId, filters, operator, pageable, isDeleted
        // Label field: "DESCRIPTION" (display field)
        // Value field: "TRANSACTION_POID" (primary key)
        RawSearchResult raw = documentService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "DESCRIPTION",   // label field for display
                TRANSACTION_POID    // value field (primary key)
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

    /**
     * Returns tariffs whose period overlaps the selected range (All Records view).
     * Overlap: PERIOD_FROM &lt;= searchTo AND PERIOD_TO &gt;= searchFrom
     */
    private void applyPeriodRangeFilters(List<FilterDto> filters, LocalDate periodFrom, LocalDate periodTo) {
        if (periodFrom == null || periodTo == null) {
            return;
        }
        filters.add(new FilterDto("PERIOD_FROM", "<=" + periodTo));
        filters.add(new FilterDto("PERIOD_TO", ">=" + periodFrom));
    }

    @Override
    @Transactional(readOnly = true)
    public PortStorageTariffDto getTariff(Long id) {
        log.info("Getting tariff with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ShipPortTariffHdr tariff = tariffHdrRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(TARIFF, TRANSACTIONPOID, id.toString()));

        PortStorageTariffDto dto = mapper.mapToDto(tariff);

        // Fetch detail records
        List<ShipPortTariffDtl> details = tariffDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        dto.setTariffDetails(mapper.mapDetailsToDto(details));

        // Enrich with LOV data
        enrichLovData(dto);

        // Log view

        log.info("Successfully retrieved tariff with id: {}", id);
        return dto;
    }

    @Override
    @Transactional
    public PortStorageTariffDto createTariff(PortStorageTariffCreateDTO dto, Long groupPoid, Long userPoid, Long companyPoid) {
        log.info("Creating tariff with description: {}, groupId: {}, userPoid: {}", dto.getDescription(), groupPoid, userPoid);

        // Validate
        validateTariffCreateDTO(dto, groupPoid);

        // Create header entity
        ShipPortTariffHdr tariff = new ShipPortTariffHdr();
        mapper.mapCreateDTOToEntity(dto, tariff, groupPoid, userPoid, companyPoid);

        // Save header
        ShipPortTariffHdr saved = tariffHdrRepository.save(tariff);

        // Create detail records
        if (dto.getTariffDetails() != null && !dto.getTariffDetails().isEmpty()) {
            createTariffDetails(saved.getTransactionPoid(), dto.getTariffDetails());
        }

        // Fetch and return with LOV data
        PortStorageTariffDto result = mapper.mapToDto(saved);
        List<ShipPortTariffDtl> details = tariffDtlRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());
        result.setTariffDetails(mapper.mapDetailsToDto(details));
        enrichLovData(result);

        // Log creation
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), saved.getTransactionPoid().toString());

        log.info("Successfully created tariff with id: {}", saved.getTransactionPoid());
        return result;
    }

    @Override
    @Transactional
    public PortStorageTariffDto updateTariff(Long id, PortStorageTariffUpdateDTO dto, Long groupPoid, Long userPoid, Long companyPoid) {
        log.info("Updating tariff with id: {}, groupId: {}, userPoid: {}", id, groupPoid, userPoid);

        // Find existing tariff
        ShipPortTariffHdr tariff = tariffHdrRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(TARIFF, TRANSACTIONPOID, id.toString()));

        // Validate
        validateTariffUpdateDTO(dto, id, groupPoid);

        // Store old values for logging
        ShipPortTariffHdr oldEntity = new ShipPortTariffHdr();
        BeanUtils.copyProperties(tariff, oldEntity);

        // Update header entity
        mapper.mapUpdateDTOToEntity(dto, tariff, groupPoid, userPoid, companyPoid);
        ShipPortTariffHdr saved = tariffHdrRepository.save(tariff);

        // Update detail records
        if (dto.getTariffDetails() != null) {
            updateTariffDetails(id, dto.getTariffDetails());
        }

        // Fetch and return with LOV data
        PortStorageTariffDto result = mapper.mapToDto(saved);
        List<ShipPortTariffDtl> details = tariffDtlRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());
        result.setTariffDetails(mapper.mapDetailsToDto(details));
        enrichLovData(result);

        // Log changes
        loggingService.logChanges(oldEntity, saved, ShipPortTariffHdr.class, UserContext.getDocumentId(), id.toString(), LogDetailsEnum.MODIFIED, TRANSACTION_POID);

        log.info("Successfully updated tariff with id: {}", id);
        return result;
    }

    @Override
    @Transactional
    public void deleteTariff(Long groupPoid, Long tariffId, Long companyPoid, DeleteReasonDto deleteReasonDto) {
        log.info("deleteTariff started for tariffId={} companyPoid={} groupPoid={}", 
            tariffId, companyPoid, groupPoid);

        // 1. Validate entity exists
        ShipPortTariffHdr tariff = tariffHdrRepository
                .findByTransactionPoidAndGroupPoid(tariffId, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(TARIFF, TRANSACTIONPOID, tariffId));

        // 2. Check if already deleted
        if ("Y".equals(tariff.getDeleted())) {
            log.warn("deleteTariff found companyPoid={} tariffId={} already deleted", companyPoid, tariffId);
            throw new com.asg.shipping.exceptions.CustomException("Cannot delete tariff. It is already deleted.");
        }

        documentDeleteService.deleteDocument(
                tariffId,
                "SHIP_PORT_TARIFF_HDR",
                TRANSACTION_POID,
                deleteReasonDto,
                null);

        log.info("deleteTariff completed for tariffId={} companyPoid={}", tariffId, companyPoid);
    }

    /**
     * Enrich DTO with LOV data
     */
    private void enrichLovData(PortStorageTariffDto dto) {
        enrichPort(dto);
        enrichTariffDetails(dto);
    }

    private void enrichPort(PortStorageTariffDto dto) {
        if (dto.getPortPoid() == null) return;

        try {
            LovGetListDto portDet = lovService.getDetailsByPoidAndLovName(
                    dto.getPortPoid(),
                    "PORT_MASTER"
            );
            dto.setPortDet(portDet);
        } catch (Exception e) {
            log.warn("Failed to fetch PORT_MASTER LOV for portPoid: {}", dto.getPortPoid(), e);
        }
    }

    private void enrichTariffDetails(PortStorageTariffDto dto) {
        if (dto.getTariffDetails() == null) return;

        for (TariffDetailDto detailDto : dto.getTariffDetails()) {
            enrichContainerType(detailDto);
            enrichContainerSize(detailDto);
        }
    }

    private void enrichContainerType(TariffDetailDto detailDto) {
        if (detailDto.getContainerTypePoid() == null) return;

        try {
            LovGetListDto containerTypeDet = lovService.getDetailsByPoidAndLovName(
                    detailDto.getContainerTypePoid(),
                    "CONTAINER_TYPE_MASTER"
            );
            detailDto.setContainerTypeDet(containerTypeDet);
        } catch (Exception e) {
            log.warn("Failed to fetch CONTAINER_TYPE_MASTER LOV for containerTypePoid: {}",
                    detailDto.getContainerTypePoid(), e);
        }
    }

    private void enrichContainerSize(TariffDetailDto detailDto) {
        if (detailDto.getContainerSize() == null) return;

        try {
            LovGetListDto containerSizeDet = lovService.getDetailsByCodeAndLovName(
                    detailDto.getContainerSize().toString(),
                    "SHIP_CONTAINER_SIZE_PORT"
            );
            detailDto.setContainerSizeDet(containerSizeDet);
        } catch (Exception e) {
            log.warn("Failed to fetch SHIP_CONTAINER_SIZE_PORT LOV for containerSize: {}",
                    detailDto.getContainerSize(), e);
        }
    }

    /**
     * Create tariff detail records
     */
    private void createTariffDetails(Long transactionPoid, List<TariffDetailCreateDTO> detailDtos) {
        String currentUser = getCurrentUser();
        long detRowId = 1;

        for (TariffDetailCreateDTO detailDto : detailDtos) {
            validateSlabDetails(detailDto);
            ShipPortTariffDtl detail = mapper.mapDetailCreateDTOToEntity(detailDto, transactionPoid, currentUser);
            detail.setDetRowId(detRowId++);
            tariffDtlRepository.save(detail);
        }
    }


    private void updateTariffDetails(Long transactionPoid, List<TariffDetailUpdateDTO> detailDtos) {
        if (detailDtos == null) return;

        String docId      = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        List<String>                            logEntries  = new ArrayList<>();
        List<ShipPortTariffDtl>                 toUpdate    = new ArrayList<>();
        List<ShipPortTariffDtlId>               toDelete    = new ArrayList<>();
        List<LogRequestDto<ShipPortTariffDtl>>  logRequests = new ArrayList<>();

        for (TariffDetailUpdateDTO detailDto : detailDtos) {
            validateSlabDetails(detailDto);
            String action = resolveAction(detailDto.getActionType());

            switch (action) {
                case ACTION_ISCREATED -> saveTariffDetail(detailDto, transactionPoid, logEntries);

                case ACTION_ISUPDATED -> {
                    ShipPortTariffDtl existing = tariffDtlRepository
                            .findById(new ShipPortTariffDtlId(transactionPoid, detailDto.getDetRowId()))
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Tariff Detail", "detRowId", detailDto.getDetRowId()));

                    ShipPortTariffDtl oldEntity = new ShipPortTariffDtl();
                    BeanUtils.copyProperties(existing, oldEntity);

                    mapper.updateDetailFromDTO(detailDto, existing, getCurrentUser());
                    toUpdate.add(existing);

                    String logDetail = String.format(LOG_KEY_ID_FORMAT, transactionPoid, detailDto.getDetRowId());
                    logRequests.add(new LogRequestDto<>(
                            oldEntity, existing, ShipPortTariffDtl.class, docId, docKeyPoid, logDetail));
                }

                case ACTION_ISDELETED ->
                        Optional.ofNullable(detailDto.getDetRowId())
                                .map(id -> new ShipPortTariffDtlId(transactionPoid, id))
                                .ifPresent(toDelete::add);

                default -> { /* ACTION_NOCHANGES – nothing to do */ }
            }
        }

        processUpdates(tariffDtlRepository, toUpdate, logRequests);

        logSummaryEntries(logEntries, docId, docKeyPoid);

        if (!toDelete.isEmpty()) {
            List<ShipPortTariffDtl> entitiesToDelete = tariffDtlRepository.findAllById(toDelete);
            tariffDtlRepository.deleteAllInBatch(entitiesToDelete);
            entitiesToDelete.forEach(e -> loggingService.logDelete(e, docId, docKeyPoid));
        }
    }

    private void saveTariffDetail(TariffDetailUpdateDTO detailDto, Long transactionPoid, List<String> logEntries) {
        long nextDetRowId = getNextDetRowId(tariffDtlRepository.getMaxDetRowId(transactionPoid));
        ShipPortTariffDtl entity = mapper.mapDetailUpdateDTOToEntity(detailDto, transactionPoid, getCurrentUser());
        entity.setDetRowId(nextDetRowId);
        tariffDtlRepository.save(entity);
        logEntries.add(String.format("Row Created on Tariff Detail with DetRowId: %s", nextDetRowId));
    }

    /**
     * Bulk-save updated entities and, if any field changes are detected, emit a
     * batched audit log via {@link LoggingService#createLogBatch}.
     */
    private <T, ID> void processUpdates(JpaRepository<T, ID> repository,
                                        List<T> entities,
                                        List<LogRequestDto<T>> logRequests) {
        if (!entities.isEmpty()) {
            repository.saveAll(entities);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
    }

    private String resolveAction(String rawAction) {
        String action = (rawAction == null || rawAction.trim().isEmpty())
                ? ACTION_NOCHANGES
                : rawAction.trim().toUpperCase();
        return switch (action) {
            case ACTION_ISCREATED, "ISCREATED", "CREATED", "NEW" -> ACTION_ISCREATED;
            case ACTION_ISUPDATED, "ISUPDATED", "UPDATED"        -> ACTION_ISUPDATED;
            case ACTION_ISDELETED, "ISDELETED", "DELETED"        -> ACTION_ISDELETED;
            default                                               -> ACTION_NOCHANGES;
        };
    }

    private void logSummaryEntries(List<String> logEntries, String docId, String docKeyPoid) {
        if (logEntries != null) {
            logEntries.forEach(entry -> loggingService.createLogSummaryEntry(docId, docKeyPoid, entry));
        }
    }

    private long getNextDetRowId(Long maxDetRowId) {
        return (maxDetRowId != null ? maxDetRowId : 0L) + 1L;
    }

    private void validateTariffCreateDTO(PortStorageTariffCreateDTO dto, Long groupPoid) {
        // Validate LOVs
        validatePort(dto.getPortPoid());
        validateTariffType(dto.getTariffType());

        // Date validation: periodFrom must be <= periodTo
        if (dto.getPeriodFrom() != null && dto.getPeriodTo() != null && dto.getPeriodFrom().isAfter(dto.getPeriodTo())) {
                throw new ValidationException("Period from date must be less than or equal to period to date");
        }

        // Check for date overlap
        if (dto.getPortPoid() != null && dto.getTariffType() != null &&
                dto.getPeriodFrom() != null && dto.getPeriodTo() != null && tariffHdrRepository.existsOverlappingPeriod(
                dto.getPortPoid(),
                dto.getTariffType(),
                groupPoid,
                dto.getPeriodFrom(),
                dto.getPeriodTo(),
                null)) {
                throw new ValidationException("Period overlaps with an existing tariff for the same port and tariff type");
        }

        // Check if document reference already exists
        if (dto.getDocRef() != null && !dto.getDocRef().isEmpty() && tariffHdrRepository.existsByDocRef(dto.getDocRef())) {
            throw new ValidationException("Document reference already exists");
        }

        // Validate detail LOVs
        if (dto.getTariffDetails() != null) {
            for (TariffDetailCreateDTO detailDto : dto.getTariffDetails()) {
                validateDetailLovs(detailDto.getContainerTypePoid(), detailDto.getContainerSize());
            }
        }
    }

    /**
     * Validate TariffUpdateDTO
     */
    private void validateTariffUpdateDTO(PortStorageTariffUpdateDTO dto, Long excludeTransactionPoid, Long groupPoid) {
        // Validate LOVs
        validatePort(dto.getPortPoid());
        validateTariffType(dto.getTariffType());

        // Date validation: periodFrom must be <= periodTo
        if (dto.getPeriodFrom() != null && dto.getPeriodTo() != null && dto.getPeriodFrom().isAfter(dto.getPeriodTo())) {
            throw new ValidationException("Period from date must be less than or equal to period to date");
        }

        // Check for date overlap (excluding current transaction)
        if (dto.getPortPoid() != null && dto.getTariffType() != null &&
                dto.getPeriodFrom() != null && dto.getPeriodTo() != null &&
                tariffHdrRepository.existsOverlappingPeriod(
                        dto.getPortPoid(),
                        dto.getTariffType(),
                        groupPoid,
                        dto.getPeriodFrom(),
                        dto.getPeriodTo(),
                        excludeTransactionPoid)) {
            throw new ValidationException("Period overlaps with an existing tariff for the same port and tariff type");
        }

        // Check if document reference already exists (excluding current transaction)
        if (dto.getDocRef() != null && !dto.getDocRef().isEmpty() && tariffHdrRepository.existsByDocRefExcludingPoid(dto.getDocRef(), excludeTransactionPoid)) {
            throw new ValidationException("Document reference already exists");
        }

        // Validate detail LOVs
        if (dto.getTariffDetails() != null) {
            for (TariffDetailUpdateDTO detailDto : dto.getTariffDetails()) {
                validateDetailLovs(detailDto.getContainerTypePoid(), detailDto.getContainerSize());
            }
        }
    }

    /**
     * Validate slab details for TariffDetailCreateDTO
     */
    private void validateSlabDetails(TariffDetailCreateDTO dto) {
        validateSlabSequence(dto.getSlab1Tilldays(), dto.getSlab2Tilldays(), SLAB1, SLAB2);
        validateSlabSequence(dto.getSlab2Tilldays(), dto.getSlab3Tilldays(), SLAB2, SLAB3);
        validateSlabSequence(dto.getSlab3Tilldays(), dto.getSlab4Tilldays(), SLAB3, SLAB4);
        validateSlabSequence(dto.getSlab4Tilldays(), dto.getSlab5Tilldays(), SLAB4, SLAB5);
        validateSlabSequence(dto.getSlab5Tilldays(), dto.getSlab6Tilldays(), SLAB5, SLAB6);
        validateSlabSequence(dto.getSlab6Tilldays(), dto.getSlab7Tilldays(), SLAB6, SLAB7);

        validateSlabRate(dto.getSlab1Tilldays(), dto.getSlab1Rate(), SLAB1);
        validateSlabRate(dto.getSlab2Tilldays(), dto.getSlab2Rate(), SLAB2);
        validateSlabRate(dto.getSlab3Tilldays(), dto.getSlab3Rate(), SLAB3);
        validateSlabRate(dto.getSlab4Tilldays(), dto.getSlab4Rate(), SLAB4);
        validateSlabRate(dto.getSlab5Tilldays(), dto.getSlab5Rate(), SLAB5);
        validateSlabRate(dto.getSlab6Tilldays(), dto.getSlab6Rate(), SLAB6);
        validateSlabRate(dto.getSlab7Tilldays(), dto.getSlab7Rate(), SLAB7);
    }

    /**
     * Validate slab details for TariffDetailUpdateDTO
     */
    private void validateSlabDetails(TariffDetailUpdateDTO dto) {
        validateSlabSequence(dto.getSlab1Tilldays(), dto.getSlab2Tilldays(), SLAB1, SLAB2);
        validateSlabSequence(dto.getSlab2Tilldays(), dto.getSlab3Tilldays(), SLAB2, SLAB3);
        validateSlabSequence(dto.getSlab3Tilldays(), dto.getSlab4Tilldays(), SLAB3, SLAB4);
        validateSlabSequence(dto.getSlab4Tilldays(), dto.getSlab5Tilldays(), SLAB4, SLAB5);
        validateSlabSequence(dto.getSlab5Tilldays(), dto.getSlab6Tilldays(), SLAB5, SLAB6);
        validateSlabSequence(dto.getSlab6Tilldays(), dto.getSlab7Tilldays(), SLAB6, SLAB7);

        validateSlabRate(dto.getSlab1Tilldays(), dto.getSlab1Rate(), SLAB1);
        validateSlabRate(dto.getSlab2Tilldays(), dto.getSlab2Rate(), SLAB2);
        validateSlabRate(dto.getSlab3Tilldays(), dto.getSlab3Rate(), SLAB3);
        validateSlabRate(dto.getSlab4Tilldays(), dto.getSlab4Rate(), SLAB4);
        validateSlabRate(dto.getSlab5Tilldays(), dto.getSlab5Rate(), SLAB5);
        validateSlabRate(dto.getSlab6Tilldays(), dto.getSlab6Rate(), SLAB6);
        validateSlabRate(dto.getSlab7Tilldays(), dto.getSlab7Rate(), SLAB7);
    }

    /**
     * Validate that current slab is less than next slab
     */
    private void validateSlabSequence(Integer currentSlab, Integer nextSlab, String currentName, String nextName) {
        if (currentSlab != null && nextSlab != null && currentSlab >= nextSlab) {
            throw new ValidationException(currentName + " must be less than " + nextName);
        }
    }

    /**
     * Validate that if slab is entered, rate must be provided
     */
    private void validateSlabRate(Integer slabDays, java.math.BigDecimal rate, String slabName) {
        if (slabDays != null && rate == null) {
            throw new ValidationException(slabName + " rate is mandatory when slab days are entered");
        }
    }

    private void validatePort(Long portPoid) {
        if (portPoid != null) {
            LovGetListDto lovGetListDto = lovService.getDetailsByPoidAndLovName(portPoid, "PORT_MASTER");
            if (lovGetListDto == null || lovGetListDto.getPoid() == null)
                throw new ValidationException("Port is not active");
        }
    }

    private void validateTariffType(String tariffType) {
        if (tariffType != null) {
            LovGetListDto lovGetListDto = lovService.getDetailsByCodeAndLovName(tariffType, "PORT_TARIFF_TYPES");
            if (lovGetListDto == null || lovGetListDto.getCode() == null)
                throw new ValidationException("Tariff type is not active");
        }
    }

    private void validateDetailLovs(Long containerTypePoid, java.math.BigDecimal containerSize) {
        if (containerTypePoid != null) {
            LovGetListDto lovGetListDto = lovService.getDetailsByPoidAndLovName(containerTypePoid, "CONTAINER_TYPE_MASTER");
            if (lovGetListDto == null || lovGetListDto.getPoid() == null)
                throw new ValidationException("Container type is not active");
        }
        if (containerSize != null) {
            LovGetListDto lovGetListDto = lovService.getDetailsByCodeAndLovName(containerSize.toString(), "SHIP_CONTAINER_SIZE_PORT");
            if (lovGetListDto == null || lovGetListDto.getCode() == null)
                throw new ValidationException("Container size is not active");
        }
    }
}
