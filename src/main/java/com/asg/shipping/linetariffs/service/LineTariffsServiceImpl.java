package com.asg.shipping.linetariffs.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.service.LovDataService;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.linetariffs.dto.*;
import com.asg.shipping.linetariffs.entity.*;
import com.asg.shipping.linetariffs.repository.*;
import com.asg.shipping.linetariffs.util.LineTariffMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Service implementation for Line Tariffs operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LineTariffsServiceImpl implements LineTariffsService {

    private final ShipLineTariffHdrRepository tariffHdrRepository;
    private final ShipLineTariffImpDtlRepository impDtlRepository;
    private final ShipLineTariffImpPayDtlRepository impPayDtlRepository;
    private final ShipLineTariffExpDtlRepository expDtlRepository;
    private final ShipLineTariffExpPayDtlRepository expPayDtlRepository;
    private final DocumentSearchService documentService;
    private final LovDataService lovService;
    private final LineTariffMapper mapper;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchLineTariffs(String docId, com.asg.common.lib.dto.FilterRequestDto request, Pageable pageable, LocalDate startDate, LocalDate endDate) {
        log.info("Searching line tariffs with docId: {}, page: {}, size: {}, startDate: {}, endDate: {}", docId, pageable.getPageNumber(), pageable.getPageSize(), startDate, endDate);

        // Resolve filter components from FilterRequestDto
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);
        
        // Add date filters if provided
        if (startDate != null && endDate != null) {
            // Add date range filter for TRANSACTION_DATE field
            filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", startDate, endDate);
        }

        // Call documentService.search with docId, filters, operator, pageable, isDeleted
        // Label field: "DESCRIPTION" (display field)
        // Value field: "TRANSACTION_POID" (primary key)
        RawSearchResult raw = documentService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "DESCRIPTION",        // label field for display
                "TRANSACTION_POID"   // value field (primary key)
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
    public LineTariffDto getLineTariff(Long id) {
        log.info("Getting line tariff with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ShipLineTariffHdr tariff = tariffHdrRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line Tariff", "transactionPoid", id.toString()));

        // Fetch all detail records
        List<ShipLineTariffImpDtl> impDtlList = impDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffImpPayDtl> impPayDtlList = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffExpDtl> expDtlList = expDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffExpPayDtl> expPayDtlList = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(id);

        LineTariffDto dto = mapper.mapToDto(tariff, impDtlList, impPayDtlList, expDtlList, expPayDtlList);

        // Enrich with LOV data - TEMPORARILY DISABLED FOR PERFORMANCE
        // enrichLovData(dto);

        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());

        log.info("Successfully retrieved line tariff with id: {}", id);
        return dto;
    }

    @Override
    @Transactional
    public LineTariffDto createLineTariff(LineTariffCreateDTO dto, Long groupPoid, Long userPoid) {
        log.info("Creating line tariff for line: {}, period: {} to {}", dto.getLinePoid(), dto.getPeriodFrom(), dto.getPeriodTo());

        // Validate
        validateTariffCreateDTO(dto, groupPoid);

        // Create header entity
        ShipLineTariffHdr tariff = new ShipLineTariffHdr();
        mapper.mapCreateDTOToEntity(dto, tariff, groupPoid, userPoid);

        // Additional validation just before save to prevent race conditions
        if (dto.getDocRef() != null && !dto.getDocRef().trim().isEmpty()) {
            if (tariffHdrRepository.existsByDocRef(dto.getDocRef().trim())) {
                throw new ValidationException("Document Reference " + dto.getDocRef().trim() + " already exists. Please use a different reference.");
            }
        }

        // Save header (generates TRANSACTION_POID)
        ShipLineTariffHdr saved;
        try {
            saved = tariffHdrRepository.save(tariff);
            tariffHdrRepository.flush(); // Force flush to catch constraint violations immediately
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            if (e.getMessage().contains("UK_DOCREFFSHIP_LINE_TARIFF_HDR")) {
                throw new ValidationException("Document Reference " + dto.getDocRef() + " already exists. Please use a different reference.");
            }
            if (e.getMessage().contains("SHIP_TF_HDR_LINPERION_UK")) {
                throw new ValidationException("A tariff with the same line and period already exists. Please use a different period.");
            }
            throw e;
        }

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), saved.getTransactionPoid().toString());

        // Create detail records
        createDetailRecords(saved.getTransactionPoid(), dto);

        // Fetch all detail records for response
        List<ShipLineTariffImpDtl> impDtlList = impDtlRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());
        List<ShipLineTariffImpPayDtl> impPayDtlList = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());
        List<ShipLineTariffExpDtl> expDtlList = expDtlRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());
        List<ShipLineTariffExpPayDtl> expPayDtlList = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());

        LineTariffDto result = mapper.mapToDto(saved, impDtlList, impPayDtlList, expDtlList, expPayDtlList);
        // enrichLovData(result); // TEMPORARILY DISABLED FOR PERFORMANCE

        log.info("Successfully created line tariff with id: {}", saved.getTransactionPoid());
        return result;
    }

    @Override
    @Transactional
    public LineTariffDto updateLineTariff(Long id, LineTariffUpdateDTO dto, Long groupPoid, Long userPoid) {
        log.info("Updating line tariff with id: {}", id);

        // Find existing tariff
        ShipLineTariffHdr tariff = tariffHdrRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line Tariff", "transactionPoid", id.toString()));

        // Validate
        validateTariffUpdateDTO(dto, id, groupPoid);

        // Create old entity for logging changes
        ShipLineTariffHdr oldTariff = new ShipLineTariffHdr();
        oldTariff.setDescription(tariff.getDescription());
        oldTariff.setPeriodFrom(tariff.getPeriodFrom());
        oldTariff.setPeriodTo(tariff.getPeriodTo());
        oldTariff.setDmgFromSameday(tariff.getDmgFromSameday());
        oldTariff.setDmgFromNextday(tariff.getDmgFromNextday());
        oldTariff.setPayableCurrency(tariff.getPayableCurrency());
        oldTariff.setReceivableCurrency(tariff.getReceivableCurrency());
        oldTariff.setDocRef(tariff.getDocRef());

        // Update header entity
        mapper.mapUpdateDTOToEntity(dto, tariff, groupPoid, userPoid);
        ShipLineTariffHdr saved;
        try {
            saved = tariffHdrRepository.save(tariff);
            tariffHdrRepository.flush(); // Force flush to catch constraint violations immediately
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            if (e.getMessage().contains("UK_DOCREFFSHIP_LINE_TARIFF_HDR")) {
                throw new ValidationException("Document Reference " + dto.getDocRef() + " already exists. Please use a different reference.");
            }
            if (e.getMessage().contains("SHIP_TF_HDR_LINPERION_UK")) {
                throw new ValidationException("A tariff with the same line and period already exists. Please use a different period.");
            }
            throw e;
        }

        loggingService.logChanges(oldTariff, saved, ShipLineTariffHdr.class, UserContext.getDocumentId(), id.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        // Update detail records
        updateDetailRecords(id, dto);

        // Fetch all detail records for response
        List<ShipLineTariffImpDtl> impDtlList = impDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffImpPayDtl> impPayDtlList = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffExpDtl> expDtlList = expDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffExpPayDtl> expPayDtlList = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(id);

        LineTariffDto result = mapper.mapToDto(saved, impDtlList, impPayDtlList, expDtlList, expPayDtlList);
        // enrichLovData(result); // TEMPORARILY DISABLED FOR PERFORMANCE

        log.info("Successfully updated line tariff with id: {}", id);
        return result;
    }

    @Override
    @Transactional
    public void deleteLineTariff(Long id, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting line tariff with id: {}", id);

        Long groupPoid = UserContext.getGroupPoid();

        ShipLineTariffHdr tariff = tariffHdrRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line Tariff", "transactionPoid", id.toString()));

        documentDeleteService.deleteDocument(
                id,
                "SHIP_LINE_TARIFF_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                null
        );

        log.info("Successfully deleted line tariff with id: {}", id);
    }

    @Override
    @Transactional
    public LineTariffDto copyLineTariff(Long id, CopyTariffRequestDTO request, Long groupPoid, Long userPoid) {
        log.info("Copying line tariff with id: {} to new period: {} to {}", id, request.getPeriodFrom(), request.getPeriodTo());

        // Find source tariff
        ShipLineTariffHdr sourceTariff = tariffHdrRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line Tariff", "transactionPoid", id.toString()));

        // Validate new period does not overlap
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();
        if (tariffHdrRepository.existsOverlappingPeriod(
                sourceTariff.getLinePoid(),
                groupPoid,
                companyPoid,
                request.getPeriodFrom(),
                request.getPeriodTo(),
                null)) {
            throw new ValidationException("New period overlaps with an existing tariff for the same line");
        }

        // Validate period dates
        if (request.getPeriodFrom().isAfter(request.getPeriodTo())) {
            throw new ValidationException("Period from date must be less than or equal to period to date");
        }

        // Update source tariff PERIOD_TO to new PERIOD_FROM - 1 day
        LocalDate newPeriodTo = request.getPeriodFrom().minusDays(1);
        sourceTariff.setPeriodTo(newPeriodTo);
        sourceTariff.setLastModifiedBy(getCurrentUser());
        sourceTariff.setLastModifiedDate(LocalDateTime.now());
        tariffHdrRepository.save(sourceTariff);

        // Create new tariff header
        ShipLineTariffHdr newTariff = new ShipLineTariffHdr();
        newTariff.setGroupPoid(sourceTariff.getGroupPoid());
        newTariff.setLinePoid(sourceTariff.getLinePoid());
        newTariff.setDescription(request.getDescription() != null ? request.getDescription() : sourceTariff.getDescription());
        newTariff.setPeriodFrom(request.getPeriodFrom());
        newTariff.setPeriodTo(request.getPeriodTo());
        newTariff.setDmgFromSameday(sourceTariff.getDmgFromSameday());
        newTariff.setDmgFromNextday(sourceTariff.getDmgFromNextday());
        newTariff.setDmgSkipHolidays(sourceTariff.getDmgSkipHolidays());
        newTariff.setDmgSkipWeekends(sourceTariff.getDmgSkipWeekends());
        newTariff.setDmgBaseslabAfterFree(sourceTariff.getDmgBaseslabAfterFree());
        newTariff.setDtnFromSameday(sourceTariff.getDtnFromSameday());
        newTariff.setDtnFromNextday(sourceTariff.getDtnFromNextday());
        newTariff.setDtnSkipHolidays(sourceTariff.getDtnSkipHolidays());
        newTariff.setDtnSkipWeekends(sourceTariff.getDtnSkipWeekends());
        newTariff.setDtnBaseslabAfterFree(sourceTariff.getDtnBaseslabAfterFree());
        newTariff.setPayableCurrency(sourceTariff.getPayableCurrency());
        newTariff.setReceivableCurrency(sourceTariff.getReceivableCurrency());
        newTariff.setDocRef(null); // Clear DOC_REF for new tariff
        newTariff.setCompanyPoid(sourceTariff.getCompanyPoid());
        newTariff.setSeqno(sourceTariff.getSeqno());
        newTariff.setCreatedBy(getCurrentUser());
        newTariff.setCreatedDate(LocalDateTime.now());
        newTariff.setLastModifiedBy(getCurrentUser());
        newTariff.setLastModifiedDate(LocalDateTime.now());
        newTariff.setDeleted("N");

        ShipLineTariffHdr savedNewTariff = tariffHdrRepository.save(newTariff);

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), savedNewTariff.getTransactionPoid().toString());

        // Copy all detail records
        copyDetailRecords(id, savedNewTariff.getTransactionPoid());

        // Fetch all detail records for response
        List<ShipLineTariffImpDtl> impDtlList = impDtlRepository.findByTransactionPoidOrderByDetRowId(savedNewTariff.getTransactionPoid());
        List<ShipLineTariffImpPayDtl> impPayDtlList = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(savedNewTariff.getTransactionPoid());
        List<ShipLineTariffExpDtl> expDtlList = expDtlRepository.findByTransactionPoidOrderByDetRowId(savedNewTariff.getTransactionPoid());
        List<ShipLineTariffExpPayDtl> expPayDtlList = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(savedNewTariff.getTransactionPoid());

        LineTariffDto result = mapper.mapToDto(savedNewTariff, impDtlList, impPayDtlList, expDtlList, expPayDtlList);
        // enrichLovData(result); // TEMPORARILY DISABLED FOR PERFORMANCE

        log.info("Successfully copied line tariff with id: {} to new tariff with id: {}", id, savedNewTariff.getTransactionPoid());
        return result;
    }

    /**
     * Create detail records for all four detail tables
     */
    private void createDetailRecords(Long transactionPoid, LineTariffCreateDTO dto) {
        String currentUser = getCurrentUser();

        // Import Demurrage Collectable
        if (dto.getImportDemurrageCollectable() != null) {
            Long maxDetRowId = impDtlRepository.getMaxDetRowId(transactionPoid);
            for (TariffDetailCreateDTO detailDto : dto.getImportDemurrageCollectable()) {
                maxDetRowId++;
                ShipLineTariffImpDtl detail = mapper.mapImpDtlCreateDTOToEntity(detailDto, transactionPoid, maxDetRowId, currentUser);
                impDtlRepository.save(detail);
            }
        }

        // Import Demurrage Payable
        if (dto.getImportDemurragePayable() != null) {
            Long maxDetRowId = impPayDtlRepository.getMaxDetRowId(transactionPoid);
            for (TariffDetailCreateDTO detailDto : dto.getImportDemurragePayable()) {
                maxDetRowId++;
                ShipLineTariffImpPayDtl detail = mapper.mapImpPayDtlCreateDTOToEntity(detailDto, transactionPoid, maxDetRowId, currentUser);
                impPayDtlRepository.save(detail);
            }
        }

        // Export Detention Collectable
        if (dto.getExportDetentionCollectable() != null) {
            Long maxDetRowId = expDtlRepository.getMaxDetRowId(transactionPoid);
            for (TariffDetailCreateDTO detailDto : dto.getExportDetentionCollectable()) {
                maxDetRowId++;
                ShipLineTariffExpDtl detail = mapper.mapExpDtlCreateDTOToEntity(detailDto, transactionPoid, maxDetRowId, currentUser);
                expDtlRepository.save(detail);
            }
        }

        // Export Detention Payable
        if (dto.getExportDetentionPayable() != null) {
            Long maxDetRowId = expPayDtlRepository.getMaxDetRowId(transactionPoid);
            for (TariffDetailCreateDTO detailDto : dto.getExportDetentionPayable()) {
                maxDetRowId++;
                ShipLineTariffExpPayDtl detail = mapper.mapExpPayDtlCreateDTOToEntity(detailDto, transactionPoid, maxDetRowId, currentUser);
                expPayDtlRepository.save(detail);
            }
        }
    }

    /**
     * Update detail records for all four detail tables
     */
    private void updateDetailRecords(Long transactionPoid, LineTariffUpdateDTO dto) {
        String currentUser = getCurrentUser();

        // Update Import Demurrage Collectable
        updateDetailRecordsImpDtl(transactionPoid, dto.getImportDemurrageCollectable(), currentUser);

        // Update Import Demurrage Payable
        updateDetailRecordsImpPayDtl(transactionPoid, dto.getImportDemurragePayable(), currentUser);

        // Update Export Detention Collectable
        updateDetailRecordsExpDtl(transactionPoid, dto.getExportDetentionCollectable(), currentUser);

        // Update Export Detention Payable
        updateDetailRecordsExpPayDtl(transactionPoid, dto.getExportDetentionPayable(), currentUser);
    }

    /**
     * Update Import Demurrage Collectable detail records
     */
    private void updateDetailRecordsImpDtl(Long transactionPoid, List<TariffDetailUpdateDTO> detailDtos, String currentUser) {
        if (detailDtos == null) {
            detailDtos = java.util.Collections.emptyList();
        }

        List<ShipLineTariffImpDtl> existingDetails = impDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        Set<Long> existingDetRowIds = existingDetails.stream()
                .map(ShipLineTariffImpDtl::getDetRowId)
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
            impDtlRepository.deleteById(new ShipLineTariffImpDtlId(transactionPoid, detRowId));
        }

        // Update or create details
        Long maxDetRowId = impDtlRepository.getMaxDetRowId(transactionPoid);
        for (TariffDetailUpdateDTO detailDto : detailDtos) {
            if (detailDto.getDetRowId() != null) {
                // Update existing
                ShipLineTariffImpDtl existing = impDtlRepository.findByTransactionPoidAndDetRowId(transactionPoid, detailDto.getDetRowId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tariff Detail", "detRowId", detailDto.getDetRowId().toString()));

                mapper.updateImpDtlFromDTO(detailDto, existing, currentUser);
                impDtlRepository.save(existing);
            } else {
                // Create new
                maxDetRowId++;
                ShipLineTariffImpDtl newDetail = mapper.mapImpDtlUpdateDTOToEntity(detailDto, transactionPoid, maxDetRowId, currentUser);
                impDtlRepository.save(newDetail);
            }
        }
    }

    /**
     * Update Import Demurrage Payable detail records
     */
    private void updateDetailRecordsImpPayDtl(Long transactionPoid, List<TariffDetailUpdateDTO> detailDtos, String currentUser) {
        if (detailDtos == null) {
            detailDtos = java.util.Collections.emptyList();
        }

        List<ShipLineTariffImpPayDtl> existingDetails = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        Set<Long> existingDetRowIds = existingDetails.stream()
                .map(ShipLineTariffImpPayDtl::getDetRowId)
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
            impPayDtlRepository.deleteById(new ShipLineTariffImpPayDtlId(transactionPoid, detRowId));
        }

        // Update or create details
        Long maxDetRowId = impPayDtlRepository.getMaxDetRowId(transactionPoid);
        for (TariffDetailUpdateDTO detailDto : detailDtos) {
            if (detailDto.getDetRowId() != null) {
                // Update existing
                ShipLineTariffImpPayDtl existing = impPayDtlRepository.findByTransactionPoidAndDetRowId(transactionPoid, detailDto.getDetRowId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tariff Detail", "detRowId", detailDto.getDetRowId().toString()));

                mapper.updateImpPayDtlFromDTO(detailDto, existing, currentUser);
                impPayDtlRepository.save(existing);
            } else {
                // Create new
                maxDetRowId++;
                ShipLineTariffImpPayDtl newDetail = mapper.mapImpPayDtlUpdateDTOToEntity(detailDto, transactionPoid, maxDetRowId, currentUser);
                impPayDtlRepository.save(newDetail);
            }
        }
    }

    /**
     * Update Export Detention Collectable detail records
     */
    private void updateDetailRecordsExpDtl(Long transactionPoid, List<TariffDetailUpdateDTO> detailDtos, String currentUser) {
        if (detailDtos == null) {
            detailDtos = java.util.Collections.emptyList();
        }

        List<ShipLineTariffExpDtl> existingDetails = expDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        Set<Long> existingDetRowIds = existingDetails.stream()
                .map(ShipLineTariffExpDtl::getDetRowId)
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
            expDtlRepository.deleteById(new ShipLineTariffExpDtlId(transactionPoid, detRowId));
        }

        // Update or create details
        Long maxDetRowId = expDtlRepository.getMaxDetRowId(transactionPoid);
        for (TariffDetailUpdateDTO detailDto : detailDtos) {
            if (detailDto.getDetRowId() != null) {
                // Update existing
                ShipLineTariffExpDtl existing = expDtlRepository.findByTransactionPoidAndDetRowId(transactionPoid, detailDto.getDetRowId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tariff Detail", "detRowId", detailDto.getDetRowId().toString()));

                mapper.updateExpDtlFromDTO(detailDto, existing, currentUser);
                expDtlRepository.save(existing);
            } else {
                // Create new
                maxDetRowId++;
                ShipLineTariffExpDtl newDetail = mapper.mapExpDtlUpdateDTOToEntity(detailDto, transactionPoid, maxDetRowId, currentUser);
                expDtlRepository.save(newDetail);
            }
        }
    }

    /**
     * Update Export Detention Payable detail records
     */
    private void updateDetailRecordsExpPayDtl(Long transactionPoid, List<TariffDetailUpdateDTO> detailDtos, String currentUser) {
        if (detailDtos == null) {
            detailDtos = java.util.Collections.emptyList();
        }

        List<ShipLineTariffExpPayDtl> existingDetails = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        Set<Long> existingDetRowIds = existingDetails.stream()
                .map(ShipLineTariffExpPayDtl::getDetRowId)
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
            expPayDtlRepository.deleteById(new ShipLineTariffExpPayDtlId(transactionPoid, detRowId));
        }

        // Update or create details
        Long maxDetRowId = expPayDtlRepository.getMaxDetRowId(transactionPoid);
        for (TariffDetailUpdateDTO detailDto : detailDtos) {
            if (detailDto.getDetRowId() != null) {
                // Update existing
                ShipLineTariffExpPayDtl existing = expPayDtlRepository.findByTransactionPoidAndDetRowId(transactionPoid, detailDto.getDetRowId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tariff Detail", "detRowId", detailDto.getDetRowId().toString()));

                mapper.updateExpPayDtlFromDTO(detailDto, existing, currentUser);
                expPayDtlRepository.save(existing);
            } else {
                // Create new
                maxDetRowId++;
                ShipLineTariffExpPayDtl newDetail = mapper.mapExpPayDtlUpdateDTOToEntity(detailDto, transactionPoid, maxDetRowId, currentUser);
                expPayDtlRepository.save(newDetail);
            }
        }
    }

    /**
     * Copy detail records from source transaction to target transaction
     */
    private void copyDetailRecords(Long sourceTransactionPoid, Long targetTransactionPoid) {
        String currentUser = getCurrentUser();

        // Copy Import Demurrage Collectable
        List<ShipLineTariffImpDtl> sourceImpDtlList = impDtlRepository.findByTransactionPoidOrderByDetRowId(sourceTransactionPoid);
        Long maxDetRowId = 0L;
        for (ShipLineTariffImpDtl source : sourceImpDtlList) {
            maxDetRowId++;
            ShipLineTariffImpDtl target = new ShipLineTariffImpDtl();
            target.setTransactionPoid(targetTransactionPoid);
            target.setDetRowId(maxDetRowId);
            target.setContainerTypePoid(source.getContainerTypePoid());
            target.setFreeDays(source.getFreeDays());
            target.setSlab1Tilldays(source.getSlab1Tilldays());
            target.setSlab1Rate(source.getSlab1Rate());
            target.setSlab2Tilldays(source.getSlab2Tilldays());
            target.setSlab2Rate(source.getSlab2Rate());
            target.setSlab3Tilldays(source.getSlab3Tilldays());
            target.setSlab3Rate(source.getSlab3Rate());
            target.setSlab4Tilldays(source.getSlab4Tilldays());
            target.setSlab4Rate(source.getSlab4Rate());
            target.setSlab5Tilldays(source.getSlab5Tilldays());
            target.setSlab5Rate(source.getSlab5Rate());
            target.setSlab6Tilldays(source.getSlab6Tilldays());
            target.setSlab6Rate(source.getSlab6Rate());
            target.setSlab7Tilldays(source.getSlab7Tilldays());
            target.setSlab7Rate(source.getSlab7Rate());
            impDtlRepository.save(target);
        }

        // Copy Import Demurrage Payable
        List<ShipLineTariffImpPayDtl> sourceImpPayDtlList = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(sourceTransactionPoid);
        maxDetRowId = 0L;
        for (ShipLineTariffImpPayDtl source : sourceImpPayDtlList) {
            maxDetRowId++;
            ShipLineTariffImpPayDtl target = ShipLineTariffImpPayDtl.builder()
                    .transactionPoid(targetTransactionPoid)
                    .detRowId(maxDetRowId)
                    .containerTypePoid(source.getContainerTypePoid())
                    .freeDays(source.getFreeDays())
                    .slab1Tilldays(source.getSlab1Tilldays())
                    .slab1Rate(source.getSlab1Rate())
                    .slab2Tilldays(source.getSlab2Tilldays())
                    .slab2Rate(source.getSlab2Rate())
                    .slab3Tilldays(source.getSlab3Tilldays())
                    .slab3Rate(source.getSlab3Rate())
                    .slab4Tilldays(source.getSlab4Tilldays())
                    .slab4Rate(source.getSlab4Rate())
                    .slab5Tilldays(source.getSlab5Tilldays())
                    .slab5Rate(source.getSlab5Rate())
                    .slab6Tilldays(source.getSlab6Tilldays())
                    .slab6Rate(source.getSlab6Rate())
                    .slab7Tilldays(source.getSlab7Tilldays())
                    .slab7Rate(source.getSlab7Rate())
                    .build();
            impPayDtlRepository.save(target);
        }

        // Copy Export Detention Collectable
        List<ShipLineTariffExpDtl> sourceExpDtlList = expDtlRepository.findByTransactionPoidOrderByDetRowId(sourceTransactionPoid);
        maxDetRowId = 0L;
        for (ShipLineTariffExpDtl source : sourceExpDtlList) {
            maxDetRowId++;
            ShipLineTariffExpDtl target = new ShipLineTariffExpDtl();
            target.setTransactionPoid(targetTransactionPoid);
            target.setDetRowId(maxDetRowId);
            target.setContainerTypePoid(source.getContainerTypePoid());
            target.setFreeDays(source.getFreeDays());
            target.setSlab1Tilldays(source.getSlab1Tilldays());
            target.setSlab1Rate(source.getSlab1Rate());
            target.setSlab2Tilldays(source.getSlab2Tilldays());
            target.setSlab2Rate(source.getSlab2Rate());
            target.setSlab3Tilldays(source.getSlab3Tilldays());
            target.setSlab3Rate(source.getSlab3Rate());
            target.setSlab4Tilldays(source.getSlab4Tilldays());
            target.setSlab4Rate(source.getSlab4Rate());
            target.setSlab5Tilldays(source.getSlab5Tilldays());
            target.setSlab5Rate(source.getSlab5Rate());
            target.setSlab6Tilldays(source.getSlab6Tilldays());
            target.setSlab6Rate(source.getSlab6Rate());
            target.setSlab7Tilldays(source.getSlab7Tilldays());
            target.setSlab7Rate(source.getSlab7Rate());
            expDtlRepository.save(target);
        }

        // Copy Export Detention Payable
        List<ShipLineTariffExpPayDtl> sourceExpPayDtlList = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(sourceTransactionPoid);
        maxDetRowId = 0L;
        for (ShipLineTariffExpPayDtl source : sourceExpPayDtlList) {
            maxDetRowId++;
            ShipLineTariffExpPayDtl target = new ShipLineTariffExpPayDtl();
            target.setTransactionPoid(targetTransactionPoid);
            target.setDetRowId(maxDetRowId);
            target.setContainerTypePoid(source.getContainerTypePoid());
            target.setFreeDays(source.getFreeDays());
            target.setSlab1Tilldays(source.getSlab1Tilldays());
            target.setSlab1Rate(source.getSlab1Rate());
            target.setSlab2Tilldays(source.getSlab2Tilldays());
            target.setSlab2Rate(source.getSlab2Rate());
            target.setSlab3Tilldays(source.getSlab3Tilldays());
            target.setSlab3Rate(source.getSlab3Rate());
            target.setSlab4Tilldays(source.getSlab4Tilldays());
            target.setSlab4Rate(source.getSlab4Rate());
            target.setSlab5Tilldays(source.getSlab5Tilldays());
            target.setSlab5Rate(source.getSlab5Rate());
            target.setSlab6Tilldays(source.getSlab6Tilldays());
            target.setSlab6Rate(source.getSlab6Rate());
            target.setSlab7Tilldays(source.getSlab7Tilldays());
            target.setSlab7Rate(source.getSlab7Rate());
            expPayDtlRepository.save(target);
        }
    }

    /**
     * Enrich DTO with LOV data
     */
    private void enrichLovData(LineTariffDto dto) {
        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();
        Long userPoid = com.asg.common.lib.security.util.UserContext.getUserPoid();

        // Line LOV
        if (dto.getLinePoid() != null) {
            try {
                LovGetListDto lineDet =
                        lovService.getDetailsByPoidAndLovName(
                                dto.getLinePoid(),
                                "LINE_MASTER"
                        );
                dto.setLineDet(lineDet);
            } catch (Exception e) {
                log.warn("Failed to fetch LINE_MASTER LOV for linePoid: {}", dto.getLinePoid(), e);
            }
        }


        // Payable Currency LOV (code-based)
        if (dto.getPayableCurrency() != null && !dto.getPayableCurrency().isEmpty()) {
            try {
                LovGetListDto payableCurrencyDet = lovService.getDetailsByCodeAndLovName(dto.getPayableCurrency(), "CURRENCY");
                dto.setPayableCurrencyDet(payableCurrencyDet);

            } catch (Exception e) {
                log.warn("Failed to fetch CURRENCY LOV for payableCurrency: {}", dto.getPayableCurrency(), e);
            }
        }

        // Receivable Currency LOV (code-based)
        if (dto.getReceivableCurrency() != null && !dto.getReceivableCurrency().isEmpty()) {
            try {
                LovGetListDto receivableCurrencyDet =
                        lovService.getDetailsByCodeAndLovName(
                                dto.getReceivableCurrency(),
                                "CURRENCY"
                        );
                dto.setReceivableCurrencyDet(receivableCurrencyDet);

            } catch (Exception e) {
                log.warn("Failed to fetch CURRENCY LOV for receivableCurrency: {}", dto.getReceivableCurrency(), e);
            }
        }


        // Enrich detail LOVs
      /*  enrichDetailLovs(dto.getImportDemurrageCollectable(), groupPoid, companyPoid, userPoid);
        enrichDetailLovs(dto.getImportDemurragePayable(), groupPoid, companyPoid, userPoid);
        enrichDetailLovs(dto.getExportDetentionCollectable(), groupPoid, companyPoid, userPoid);
        enrichDetailLovs(dto.getExportDetentionPayable(), groupPoid, companyPoid, userPoid);*/
        enrichDetailLovs(dto.getImportDemurrageCollectable());
        enrichDetailLovs(dto.getImportDemurragePayable());
        enrichDetailLovs(dto.getExportDetentionCollectable());
        enrichDetailLovs(dto.getExportDetentionPayable());

    }

    /**
     * Enrich detail DTOs with container type LOV data
     */
  /*  private void enrichDetailLovs(List<TariffDetailDto> detailDtos, Long groupPoid, Long companyPoid, Long userPoid) {
        if (detailDtos == null) {
            return;
        }

        for (TariffDetailDto detailDto : detailDtos) {
            if (detailDto.getContainerTypePoid() != null) {
                try {
                    LovGetListDto containerTypeDet =
                            lovService.getDetailsByPoidAndLovName(
                                    detailDto.getContainerTypePoid(),
                                    "LINEWISE_CONTAINER_TYPE"
                            );
                    detailDto.setContainerTypeDet(containerTypeDet);
                } catch (Exception e) {
                    log.warn("Failed to fetch LINEWISE_CONTAINER_TYPE LOV for containerTypePoid: {}", detailDto.getContainerTypePoid(), e);
                }
            }
        }
    }*/
    private void enrichDetailLovs(List<TariffDetailDto> detailDtos) {
        if (detailDtos == null) {
            return;
        }

        for (TariffDetailDto detailDto : detailDtos) {
            if (detailDto.getContainerTypePoid() != null) {
                try {
                    LovGetListDto containerTypeDet =
                            lovService.getDetailsByPoidAndLovName(
                                    detailDto.getContainerTypePoid(),
                                    "LINEWISE_CONTAINER_TYPE"
                            );
                    detailDto.setContainerTypeDet(containerTypeDet);
                } catch (Exception e) {
                    log.warn("Failed to fetch LINEWISE_CONTAINER_TYPE LOV for containerTypePoid: {}",
                            detailDto.getContainerTypePoid(), e);
                }
            }
        }
    }

    /**
     * Validate TariffCreateDTO
     */
    private void validateTariffCreateDTO(LineTariffCreateDTO dto, Long groupPoid) {
        // Date validation: periodFrom must be <= periodTo
        if (dto.getPeriodFrom() != null && dto.getPeriodTo() != null) {
            if (dto.getPeriodFrom().isAfter(dto.getPeriodTo())) {
                throw new ValidationException("Period from date must be less than or equal to period to date");
            }
        }

        // Check for date overlap
        if (dto.getLinePoid() != null && dto.getPeriodFrom() != null && dto.getPeriodTo() != null) {
            Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();
            if (tariffHdrRepository.existsOverlappingPeriod(
                    dto.getLinePoid(),
                    groupPoid,
                    companyPoid,
                    dto.getPeriodFrom(),
                    dto.getPeriodTo(),
                    null)) {
                throw new ValidationException("Period overlaps with an existing tariff for the same line");
            }
        }

        // Check if document reference already exists (with trimming)
        if (dto.getDocRef() != null && !dto.getDocRef().trim().isEmpty()) {
            String trimmedDocRef = dto.getDocRef().trim();
            if (tariffHdrRepository.existsByDocRef(trimmedDocRef)) {
                throw new ValidationException("Document Reference " + trimmedDocRef + " already exists. Please use a different reference.");
            }
        }

        // Validate mutually exclusive flags
        if ("Y".equals(dto.getDmgFromSameday()) && "Y".equals(dto.getDmgFromNextday())) {
            throw new ValidationException("Same day and Next day both cannot be selected for Demurrage");
        }

        if ("Y".equals(dto.getDtnFromSameday()) && "Y".equals(dto.getDtnFromNextday())) {
            throw new ValidationException("Same day and Next day both cannot be selected for Detention");
        }
    }

    /**
     * Validate TariffUpdateDTO
     */
    private void validateTariffUpdateDTO(LineTariffUpdateDTO dto, Long excludeTransactionPoid, Long groupPoid) {
        // Date validation: periodFrom must be <= periodTo
        if (dto.getPeriodFrom() != null && dto.getPeriodTo() != null) {
            if (dto.getPeriodFrom().isAfter(dto.getPeriodTo())) {
                throw new ValidationException("Period from date must be less than or equal to period to date");
            }
        }

        // Get existing tariff to check linePoid if not provided in update
        ShipLineTariffHdr existing = tariffHdrRepository.findById(excludeTransactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line Tariff", "transactionPoid", excludeTransactionPoid.toString()));

        Long linePoid = dto.getLinePoid() != null ? dto.getLinePoid() : existing.getLinePoid();
        LocalDate periodFrom = dto.getPeriodFrom() != null ? dto.getPeriodFrom() : existing.getPeriodFrom();
        LocalDate periodTo = dto.getPeriodTo() != null ? dto.getPeriodTo() : existing.getPeriodTo();

        // Check for date overlap (excluding current transaction)
        if (linePoid != null && periodFrom != null && periodTo != null) {
            Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();
            if (tariffHdrRepository.existsOverlappingPeriod(
                    linePoid,
                    groupPoid,
                    companyPoid,
                    periodFrom,
                    periodTo,
                    excludeTransactionPoid)) {
                throw new ValidationException("Period overlaps with an existing tariff for the same line");
            }
        }

        // Check if document reference already exists (excluding current transaction)
        if (dto.getDocRef() != null && !dto.getDocRef().isEmpty()) {
            if (tariffHdrRepository.existsByDocRefExcludingPoid(dto.getDocRef(), excludeTransactionPoid)) {
                throw new ValidationException("Document reference already exists");
            }
        }

        // Validate mutually exclusive flags
        String dmgFromSameday = dto.getDmgFromSameday() != null ? dto.getDmgFromSameday() : existing.getDmgFromSameday();
        String dmgFromNextday = dto.getDmgFromNextday() != null ? dto.getDmgFromNextday() : existing.getDmgFromNextday();
        if ("Y".equals(dmgFromSameday) && "Y".equals(dmgFromNextday)) {
            throw new ValidationException("Same day and Next day both cannot be selected for Demurrage");
        }

        String dtnFromSameday = dto.getDtnFromSameday() != null ? dto.getDtnFromSameday() : existing.getDtnFromSameday();
        String dtnFromNextday = dto.getDtnFromNextday() != null ? dto.getDtnFromNextday() : existing.getDtnFromNextday();
        if ("Y".equals(dtnFromSameday) && "Y".equals(dtnFromNextday)) {
            throw new ValidationException("Same day and Next day both cannot be selected for Detention");
        }
    }
}

