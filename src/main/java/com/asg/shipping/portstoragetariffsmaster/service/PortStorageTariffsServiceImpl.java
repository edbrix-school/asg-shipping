package com.asg.shipping.portstoragetariffsmaster.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final LovDataService lovService;
    private final PortStorageTariffMapper mapper;
    private final LoggingService loggingService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchTariffs(String docId, com.asg.common.lib.dto.FilterRequestDto request, Pageable pageable) {
        log.info("Searching tariffs with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(), pageable.getPageSize());

        // Resolve filter components from FilterRequestDto
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

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
                "TRANSACTION_POID"    // value field (primary key)
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
    @Transactional(readOnly = true)
    public PortStorageTariffDto getTariff(Long id) {
        log.info("Getting tariff with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ShipPortTariffHdr tariff = tariffHdrRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff", "transactionPoid", id.toString()));

        PortStorageTariffDto dto = mapper.mapToDto(tariff);

        // Fetch detail records
        List<ShipPortTariffDtl> details = tariffDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        dto.setTariffDetails(mapper.mapDetailsToDto(details));

        // Enrich with LOV data
        enrichLovData(dto, groupPoid);

        // Log view
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());

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
        enrichLovData(result, groupPoid);

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
                .orElseThrow(() -> new ResourceNotFoundException("Tariff", "transactionPoid", id.toString()));

        // Validate
        validateTariffUpdateDTO(dto, id, groupPoid);

        // Store old values for logging
        ShipPortTariffHdr oldTariff = ShipPortTariffHdr.builder()
                .portPoid(tariff.getPortPoid())
                .description(tariff.getDescription())
                .tariffType(tariff.getTariffType())
                .periodFrom(tariff.getPeriodFrom())
                .periodTo(tariff.getPeriodTo())
                .transactionDate(tariff.getTransactionDate())
                .docRef(tariff.getDocRef())
                .build();

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
        enrichLovData(result, groupPoid);

        // Log changes
        loggingService.logChanges(oldTariff, saved, ShipPortTariffHdr.class, UserContext.getDocumentId(), id.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        log.info("Successfully updated tariff with id: {}", id);
        return result;
    }

    @Override
    @Transactional
    public void deleteTariff(Long id) {
        log.info("Deleting tariff with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ShipPortTariffHdr tariff = tariffHdrRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff", "transactionPoid", id.toString()));

        // Check if already deleted (idempotent)
        if ("Y".equals(tariff.getDeleted())) {
            log.info("Tariff with id: {} is already deleted", id);
            return;
        }

        // Soft delete
        tariff.setDeleted("Y");
        tariff.setLastModifiedBy(getCurrentUser());
        tariff.setLastModifiedDate(LocalDateTime.now());

        tariffHdrRepository.save(tariff);

        // Log deletion
        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, UserContext.getDocumentId(), id.toString());
        String logDetail = String.format("KeyId = TRANSACTION_POID:%s", id);
        String tableName = ShipPortTariffHdr.class.getAnnotation(jakarta.persistence.Table.class).name();
        loggingService.createLogDetailsEntry(UserContext.getDocumentId(), id.toString(), "Deleted", "N", "Y", logDetail, tableName);

        log.info("Successfully deleted tariff with id: {}", id);
    }

    /**
     * Enrich DTO with LOV data
     */
    private void enrichLovData(PortStorageTariffDto dto, Long groupPoid) {
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();
        Long userPoid = com.asg.common.lib.security.util.UserContext.getUserPoid();

        // Port LOV
        if (dto.getPortPoid() != null) {
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

        // Enrich detail LOVs
        if (dto.getTariffDetails() != null) {
            for (TariffDetailDto detailDto : dto.getTariffDetails()) {
                // Container Type LOV
                if (detailDto.getContainerTypePoid() != null) {
                    try {
                        LovGetListDto containerTypeDet = lovService.getDetailsByPoidAndLovName(
                                detailDto.getContainerTypePoid(),
                                "CONTAINER_TYPE_MASTER"
                        );
                        detailDto.setContainerTypeDet(containerTypeDet);
                    } catch (Exception e) {
                        log.warn("Failed to fetch CONTAINER_TYPE_MASTER LOV for containerTypePoid: {}", detailDto.getContainerTypePoid(), e);
                    }
                }

                // Container Size LOV (code-based dropdown)
                if (detailDto.getContainerSize() != null) {
                    try {
                        LovGetListDto containerSizeDet = lovService.getDetailsByCodeAndLovName(
                                detailDto.getContainerSize().toString(),
                                "SHIP_CONTAINER_SIZE_PORT"
                        );
                        detailDto.setContainerSizeDet(containerSizeDet);
                    } catch (Exception e) {
                        log.warn("Failed to fetch SHIP_CONTAINER_SIZE_PORT LOV for containerSize: {}", detailDto.getContainerSize(), e);
                    }
                }
            }
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

    /**
     * Update tariff detail records
     */
    private void updateTariffDetails(Long transactionPoid, List<TariffDetailUpdateDTO> detailDtos) {
        String currentUser = getCurrentUser();
        List<ShipPortTariffDtl> existingDetails = tariffDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        Set<Long> existingDetRowIds = existingDetails.stream()
                .map(ShipPortTariffDtl::getDetRowId)
                .collect(Collectors.toSet());

        Set<Long> requestDetRowIds = detailDtos.stream()
                .map(TariffDetailUpdateDTO::getDetRowId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        // Delete details not in request
        List<Long> toDelete = existingDetRowIds.stream()
                .filter(id -> !requestDetRowIds.contains(id))
                .collect(Collectors.toList());
        for (Long detRowId : toDelete) {
            tariffDtlRepository.deleteById(new ShipPortTariffDtlId(transactionPoid, detRowId));
        }

        // Calculate next detRowId for new records
        long nextDetRowId = existingDetRowIds.stream()
                .max(Long::compareTo)
                .orElse(0L) + 1;

        // Update or create details
        for (TariffDetailUpdateDTO detailDto : detailDtos) {
            validateSlabDetails(detailDto);
            if (detailDto.getDetRowId() != null) {
                // Update existing
                ShipPortTariffDtl existing = tariffDtlRepository.findByTransactionPoidAndDetRowId(transactionPoid, detailDto.getDetRowId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tariff Detail", "detRowId", detailDto.getDetRowId().toString()));

                mapper.updateDetailFromDTO(detailDto, existing, currentUser);
                tariffDtlRepository.save(existing);
            } else {
                // Create new
                ShipPortTariffDtl newDetail = mapper.mapDetailUpdateDTOToEntity(detailDto, transactionPoid, currentUser);
                newDetail.setDetRowId(nextDetRowId++);
                tariffDtlRepository.save(newDetail);
            }
        }
    }

    /**
     * Validate TariffCreateDTO
     */
    private void validateTariffCreateDTO(PortStorageTariffCreateDTO dto, Long groupPoid) {
        // Validate LOVs
        validatePort(dto.getPortPoid());
        validateTariffType(dto.getTariffType());

        // Date validation: periodFrom must be <= periodTo
        if (dto.getPeriodFrom() != null && dto.getPeriodTo() != null) {
            if (dto.getPeriodFrom().isAfter(dto.getPeriodTo())) {
                throw new ValidationException("Period from date must be less than or equal to period to date");
            }
        }

        // Check for date overlap
        if (dto.getPortPoid() != null && dto.getTariffType() != null &&
                dto.getPeriodFrom() != null && dto.getPeriodTo() != null) {
            if (tariffHdrRepository.existsOverlappingPeriod(
                    dto.getPortPoid(),
                    dto.getTariffType(),
                    groupPoid,
                    dto.getPeriodFrom(),
                    dto.getPeriodTo(),
                    null)) {
                throw new ValidationException("Period overlaps with an existing tariff for the same port and tariff type");
            }
        }

        // Check if document reference already exists
        if (dto.getDocRef() != null && !dto.getDocRef().isEmpty()) {
            if (tariffHdrRepository.existsByDocRef(dto.getDocRef())) {
                throw new ValidationException("Document reference already exists");
            }
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
        if (dto.getPeriodFrom() != null && dto.getPeriodTo() != null) {
            if (dto.getPeriodFrom().isAfter(dto.getPeriodTo())) {
                throw new ValidationException("Period from date must be less than or equal to period to date");
            }
        }

        // Check for date overlap (excluding current transaction)
        if (dto.getPortPoid() != null && dto.getTariffType() != null &&
                dto.getPeriodFrom() != null && dto.getPeriodTo() != null) {
            if (tariffHdrRepository.existsOverlappingPeriod(
                    dto.getPortPoid(),
                    dto.getTariffType(),
                    groupPoid,
                    dto.getPeriodFrom(),
                    dto.getPeriodTo(),
                    excludeTransactionPoid)) {
                throw new ValidationException("Period overlaps with an existing tariff for the same port and tariff type");
            }
        }

        // Check if document reference already exists (excluding current transaction)
        if (dto.getDocRef() != null && !dto.getDocRef().isEmpty()) {
            if (tariffHdrRepository.existsByDocRefExcludingPoid(dto.getDocRef(), excludeTransactionPoid)) {
                throw new ValidationException("Document reference already exists");
            }
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
        validateSlabSequence(dto.getSlab1Tilldays(), dto.getSlab2Tilldays(), "Slab 1", "Slab 2");
        validateSlabSequence(dto.getSlab2Tilldays(), dto.getSlab3Tilldays(), "Slab 2", "Slab 3");
        validateSlabSequence(dto.getSlab3Tilldays(), dto.getSlab4Tilldays(), "Slab 3", "Slab 4");
        validateSlabSequence(dto.getSlab4Tilldays(), dto.getSlab5Tilldays(), "Slab 4", "Slab 5");
        validateSlabSequence(dto.getSlab5Tilldays(), dto.getSlab6Tilldays(), "Slab 5", "Slab 6");
        validateSlabSequence(dto.getSlab6Tilldays(), dto.getSlab7Tilldays(), "Slab 6", "Slab 7");

        validateSlabRate(dto.getSlab1Tilldays(), dto.getSlab1Rate(), "Slab 1");
        validateSlabRate(dto.getSlab2Tilldays(), dto.getSlab2Rate(), "Slab 2");
        validateSlabRate(dto.getSlab3Tilldays(), dto.getSlab3Rate(), "Slab 3");
        validateSlabRate(dto.getSlab4Tilldays(), dto.getSlab4Rate(), "Slab 4");
        validateSlabRate(dto.getSlab5Tilldays(), dto.getSlab5Rate(), "Slab 5");
        validateSlabRate(dto.getSlab6Tilldays(), dto.getSlab6Rate(), "Slab 6");
        validateSlabRate(dto.getSlab7Tilldays(), dto.getSlab7Rate(), "Slab 7");
    }

    /**
     * Validate slab details for TariffDetailUpdateDTO
     */
    private void validateSlabDetails(TariffDetailUpdateDTO dto) {
        validateSlabSequence(dto.getSlab1Tilldays(), dto.getSlab2Tilldays(), "Slab 1", "Slab 2");
        validateSlabSequence(dto.getSlab2Tilldays(), dto.getSlab3Tilldays(), "Slab 2", "Slab 3");
        validateSlabSequence(dto.getSlab3Tilldays(), dto.getSlab4Tilldays(), "Slab 3", "Slab 4");
        validateSlabSequence(dto.getSlab4Tilldays(), dto.getSlab5Tilldays(), "Slab 4", "Slab 5");
        validateSlabSequence(dto.getSlab5Tilldays(), dto.getSlab6Tilldays(), "Slab 5", "Slab 6");
        validateSlabSequence(dto.getSlab6Tilldays(), dto.getSlab7Tilldays(), "Slab 6", "Slab 7");

        validateSlabRate(dto.getSlab1Tilldays(), dto.getSlab1Rate(), "Slab 1");
        validateSlabRate(dto.getSlab2Tilldays(), dto.getSlab2Rate(), "Slab 2");
        validateSlabRate(dto.getSlab3Tilldays(), dto.getSlab3Rate(), "Slab 3");
        validateSlabRate(dto.getSlab4Tilldays(), dto.getSlab4Rate(), "Slab 4");
        validateSlabRate(dto.getSlab5Tilldays(), dto.getSlab5Rate(), "Slab 5");
        validateSlabRate(dto.getSlab6Tilldays(), dto.getSlab6Rate(), "Slab 6");
        validateSlabRate(dto.getSlab7Tilldays(), dto.getSlab7Rate(), "Slab 7");
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
