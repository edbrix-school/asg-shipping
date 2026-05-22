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
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.linetariffs.dto.*;
import com.asg.shipping.linetariffs.entity.*;
import com.asg.shipping.common.repository.ShipLineMasterTypeRepository;
import com.asg.shipping.containertypes.entity.ShipContainerTypeMaster;
import com.asg.shipping.containertypes.repository.ShipContainerTypeMasterRepository;
import com.asg.shipping.linetariffs.repository.*;
import com.asg.shipping.linetariffs.util.LineTariffMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Service implementation for Line Tariffs operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LineTariffsServiceImpl implements LineTariffsService {

    private static final String LINE_TARIFF = "Line Tariff";
    private static final String TRANSACTION_POID = "transactionPoid";
    private static final String TRANSACTION_POID_COL = "TRANSACTION_POID";
    private static final String TARIFF_DETAIL = "Tariff Detail";
    private static final String DET_ROW_ID = "detRowId";

    private final ShipLineTariffHdrRepository tariffHdrRepository;
    private final ShipLineTariffImpDtlRepository impDtlRepository;
    private final ShipLineTariffImpPayDtlRepository impPayDtlRepository;
    private final ShipLineTariffExpDtlRepository expDtlRepository;
    private final ShipLineTariffExpPayDtlRepository expPayDtlRepository;
    private final DocumentSearchService documentService;
    private final LineTariffMapper mapper;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;
    private final ShipContainerTypeMasterRepository containerTypeRepository;
    private final ShipLineMasterTypeRepository lineMasterTypeRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchLineTariffs(String docId, com.asg.common.lib.dto.FilterRequestDto request, Pageable pageable, LocalDate startDate, LocalDate endDate) {
        log.info("Searching line tariffs with docId: {}, page: {}, size: {}, startDate: {}, endDate: {}", docId, pageable.getPageNumber(), pageable.getPageSize(), startDate, endDate);

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "DESCRIPTION",
                TRANSACTION_POID_COL
        );

        Page<Map<String, Object>> page = new PageImpl<>(
                raw.records(),
                pageable,
                raw.totalRecords()
        );

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public LineTariffDto getLineTariff(Long id) {
        log.info("Getting line tariff with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ShipLineTariffHdr tariff = tariffHdrRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LINE_TARIFF, TRANSACTION_POID, id.toString()));

        // Fetch all detail records
        List<ShipLineTariffImpDtl> impDtlList = impDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffImpPayDtl> impPayDtlList = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffExpDtl> expDtlList = expDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffExpPayDtl> expPayDtlList = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(id);

        LineTariffDto dto = mapper.mapToDto(tariff, impDtlList, impPayDtlList, expDtlList, expPayDtlList, buildContainerTypeMap(impDtlList, impPayDtlList, expDtlList, expPayDtlList));

        // Legacy DocumentAfterView: disable period-from when only 1 tariff exists for this line
        long tariffCountForLine = tariffHdrRepository.findLatestByLinePoidAndGroupPoid(tariff.getLinePoid(), groupPoid).size();
        dto.setPeriodFromEditable(tariffCountForLine != 1);

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
        mapper.mapCreateDTOToEntity(dto, tariff, groupPoid);

        // Additional validation just before save to prevent race conditions
        if (dto.getDocRef() != null && !dto.getDocRef().trim().isEmpty() && tariffHdrRepository.existsByDocRef(dto.getDocRef().trim())) {
            throw new ValidationException("Document Reference " + dto.getDocRef().trim() + " already exists. Please use a different reference.");
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

        LineTariffDto result = mapper.mapToDto(saved, impDtlList, impPayDtlList, expDtlList, expPayDtlList, buildContainerTypeMap(impDtlList, impPayDtlList, expDtlList, expPayDtlList));
        log.info("Successfully created line tariff with id: {}", saved.getTransactionPoid());
        return result;
    }

    @Override
    @Transactional(timeout = 120)
    public LineTariffDto updateLineTariff(Long id, LineTariffUpdateDTO dto, Long groupPoid, Long userPoid) {
        log.info("Updating line tariff with id: {}", id);

        // Find existing tariff
        ShipLineTariffHdr tariff = tariffHdrRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LINE_TARIFF, TRANSACTION_POID, id.toString()));

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
        mapper.mapUpdateDTOToEntity(dto, tariff);
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

        loggingService.logChanges(oldTariff, saved, ShipLineTariffHdr.class, UserContext.getDocumentId(), id.toString(), LogDetailsEnum.MODIFIED, TRANSACTION_POID_COL);

        // Update detail records
        updateDetailRecords(id, dto);

        // Fetch all detail records for response
        List<ShipLineTariffImpDtl> impDtlList = impDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffImpPayDtl> impPayDtlList = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffExpDtl> expDtlList = expDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffExpPayDtl> expPayDtlList = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(id);

        LineTariffDto result = mapper.mapToDto(saved, impDtlList, impPayDtlList, expDtlList, expPayDtlList, buildContainerTypeMap(impDtlList, impPayDtlList, expDtlList, expPayDtlList));
        log.info("Successfully updated line tariff with id: {}", id);
        return result;
    }

    @Override
    @Transactional
    public void deleteLineTariff(Long id, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting line tariff with id: {}", id);

        Long groupPoid = UserContext.getGroupPoid();

        tariffHdrRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LINE_TARIFF, TRANSACTION_POID, id.toString()));

        documentDeleteService.deleteDocument(
                id,
                "SHIP_LINE_TARIFF_HDR",
                TRANSACTION_POID_COL,
                deleteReasonDto,
                null
        );

        log.info("Successfully deleted line tariff with id: {}", id);
    }

    @Override
    @Transactional
    public LineTariffDto copyLineTariff(Long id, CopyTariffRequestDTO request, Long groupPoid, Long userPoid) {
        log.info("Copying line tariff with id: {} to new period: {} to {}", id, request.getPeriodFrom(), request.getPeriodTo());

        // Validate source exists
        tariffHdrRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LINE_TARIFF, TRANSACTION_POID, id.toString()));

        // Call stored procedure — handles sequence, header + all 4 detail table copies atomically
        // Proc uses PRAGMA AUTONOMOUS_TRANSACTION + COMMIT, so clear JPA cache to see the new row
        tariffHdrRepository.callCopyLineTariff(id);
        entityManager.flush();
        entityManager.clear();

        // Fetch the newly created tariff (max transaction_poid for same line)
        ShipLineTariffHdr sourceTariff = tariffHdrRepository.findById(id).get();
        List<ShipLineTariffHdr> latest = tariffHdrRepository.findLatestByLinePoidAndGroupPoid(
                sourceTariff.getLinePoid(), groupPoid);
        ShipLineTariffHdr newTariff = latest.stream()
                .filter(t -> !t.getTransactionPoid().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(LINE_TARIFF, TRANSACTION_POID, "new copy"));

        // Apply caller-supplied period/description overrides (legacy UI allowed overriding after copy)
        if (request.getDescription() != null) newTariff.setDescription(request.getDescription());
        if (request.getPeriodFrom() != null || request.getPeriodTo() != null) {
            LocalDate newFrom = request.getPeriodFrom() != null ? request.getPeriodFrom() : newTariff.getPeriodFrom();
            LocalDate newTo = request.getPeriodTo() != null ? request.getPeriodTo() : newTariff.getPeriodTo();
            // Validate the override period doesn't overlap with any existing tariff (excluding the new copy itself)
            if (newFrom != null && newTo != null) {
                if (newFrom.isAfter(newTo)) {
                    throw new ValidationException("Period from date must be less than or equal to period to date");
                }
                Long companyPoid = UserContext.getCompanyPoid();
                if (tariffHdrRepository.existsOverlappingPeriod(
                        newTariff.getLinePoid(), groupPoid, companyPoid,
                        newFrom, newTo, newTariff.getTransactionPoid())) {
                    throw new ValidationException("Period overlaps with an existing tariff for the same line");
                }
            }
            newTariff.setPeriodFrom(newFrom);
            newTariff.setPeriodTo(newTo);
        }
        tariffHdrRepository.save(newTariff);

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), newTariff.getTransactionPoid().toString());

        List<ShipLineTariffImpDtl> impDtlList = impDtlRepository.findByTransactionPoidOrderByDetRowId(newTariff.getTransactionPoid());
        List<ShipLineTariffImpPayDtl> impPayDtlList = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(newTariff.getTransactionPoid());
        List<ShipLineTariffExpDtl> expDtlList = expDtlRepository.findByTransactionPoidOrderByDetRowId(newTariff.getTransactionPoid());
        List<ShipLineTariffExpPayDtl> expPayDtlList = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(newTariff.getTransactionPoid());

        LineTariffDto result = mapper.mapToDto(newTariff, impDtlList, impPayDtlList, expDtlList, expPayDtlList,
                buildContainerTypeMap(impDtlList, impPayDtlList, expDtlList, expPayDtlList));
        log.info("Successfully copied line tariff with id: {} to new tariff with id: {}", id, newTariff.getTransactionPoid());
        return result;
    }

    /**
     * Create detail records for all four detail tables
     */
    private void createDetailRecords(Long transactionPoid, LineTariffCreateDTO dto) {
        // Import Demurrage Collectable
        if (dto.getImportDemurrageCollectable() != null) {
            Long maxDetRowId = impDtlRepository.getMaxDetRowId(transactionPoid);
            for (TariffDetailCreateDTO detailDto : dto.getImportDemurrageCollectable()) {
                validateContainerTypeExists(detailDto.getContainerTypePoid());
                maxDetRowId++;
                ShipLineTariffImpDtl detail = mapper.mapImpDtlCreateDTOToEntity(detailDto, transactionPoid, maxDetRowId);
                impDtlRepository.save(detail);
            }
        }

        // Import Demurrage Payable
        if (dto.getImportDemurragePayable() != null) {
            Long maxDetRowId = impPayDtlRepository.getMaxDetRowId(transactionPoid);
            for (TariffDetailCreateDTO detailDto : dto.getImportDemurragePayable()) {
                validateContainerTypeExists(detailDto.getContainerTypePoid());
                maxDetRowId++;
                ShipLineTariffImpPayDtl detail = mapper.mapImpPayDtlCreateDTOToEntity(detailDto, transactionPoid, maxDetRowId);
                impPayDtlRepository.save(detail);
            }
        }

        // Export Detention Collectable
        if (dto.getExportDetentionCollectable() != null) {
            Long maxDetRowId = expDtlRepository.getMaxDetRowId(transactionPoid);
            for (TariffDetailCreateDTO detailDto : dto.getExportDetentionCollectable()) {
                validateContainerTypeExists(detailDto.getContainerTypePoid());
                maxDetRowId++;
                ShipLineTariffExpDtl detail = mapper.mapExpDtlCreateDTOToEntity(detailDto, transactionPoid, maxDetRowId);
                expDtlRepository.save(detail);
            }
        }

        // Export Detention Payable
        if (dto.getExportDetentionPayable() != null) {
            Long maxDetRowId = expPayDtlRepository.getMaxDetRowId(transactionPoid);
            for (TariffDetailCreateDTO detailDto : dto.getExportDetentionPayable()) {
                validateContainerTypeExists(detailDto.getContainerTypePoid());
                maxDetRowId++;
                ShipLineTariffExpPayDtl detail = mapper.mapExpPayDtlCreateDTOToEntity(detailDto, transactionPoid, maxDetRowId);
                expPayDtlRepository.save(detail);
            }
        }
    }

    /**
     * Update detail records for all four detail tables
     */
    private void updateDetailRecords(Long transactionPoid, LineTariffUpdateDTO dto) {
        // Collect deleted detRowIds from collectable tables to cascade to payable
        Set<Long> deletedImpDetRowIds = dto.getImportDemurrageCollectable() == null ? Set.of() :
                dto.getImportDemurrageCollectable().stream()
                        .filter(d -> "isDeleted".equalsIgnoreCase(d.getActionType()) && d.getDetRowId() != null)
                        .map(TariffDetailUpdateDTO::getDetRowId)
                        .collect(Collectors.toSet());

        Set<Long> deletedExpDetRowIds = dto.getExportDetentionCollectable() == null ? Set.of() :
                dto.getExportDetentionCollectable().stream()
                        .filter(d -> "isDeleted".equalsIgnoreCase(d.getActionType()) && d.getDetRowId() != null)
                        .map(TariffDetailUpdateDTO::getDetRowId)
                        .collect(Collectors.toSet());

        // Cascade deletes to payable lists
        if (!deletedImpDetRowIds.isEmpty() && dto.getImportDemurragePayable() != null) {
            dto.getImportDemurragePayable().stream()
                    .filter(d -> deletedImpDetRowIds.contains(d.getDetRowId()))
                    .forEach(d -> d.setActionType("isDeleted"));
        }
        if (!deletedExpDetRowIds.isEmpty() && dto.getExportDetentionPayable() != null) {
            dto.getExportDetentionPayable().stream()
                    .filter(d -> deletedExpDetRowIds.contains(d.getDetRowId()))
                    .forEach(d -> d.setActionType("isDeleted"));
        }

        updateDetailRecordsImpDtl(transactionPoid, dto.getImportDemurrageCollectable());
        updateDetailRecordsImpPayDtl(transactionPoid, dto.getImportDemurragePayable());
        updateDetailRecordsExpDtl(transactionPoid, dto.getExportDetentionCollectable());
        updateDetailRecordsExpPayDtl(transactionPoid, dto.getExportDetentionPayable());
    }

    /**
     * Update Import Demurrage Collectable detail records
     */
    private void updateDetailRecordsImpDtl(Long transactionPoid, List<TariffDetailUpdateDTO> detailDtos) {
        if (detailDtos == null) detailDtos = java.util.Collections.emptyList();
        Map<Long, ShipLineTariffImpDtl> existingMap = impDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid)
                .stream().collect(Collectors.toMap(ShipLineTariffImpDtl::getDetRowId, e -> e));
        List<ShipLineTariffImpDtl> toDelete = new ArrayList<>();
        List<ShipLineTariffImpDtl> toSave = new ArrayList<>();
        long maxDetRowId = existingMap.keySet().stream().mapToLong(Long::longValue).max().orElse(0L);
        for (TariffDetailUpdateDTO dto : detailDtos) {
            String action = dto.getActionType() != null ? dto.getActionType().toLowerCase() : "";
            switch (action) {
                case "isdeleted" -> { if (dto.getDetRowId() != null && existingMap.containsKey(dto.getDetRowId())) toDelete.add(existingMap.get(dto.getDetRowId())); }
                case "iscreated", "isupdated", "" -> {
                    if (dto.getDetRowId() != null && existingMap.containsKey(dto.getDetRowId())) {
                        ShipLineTariffImpDtl e = existingMap.get(dto.getDetRowId()); mapper.updateImpDtlFromDTO(dto, e); toSave.add(e);
                    } else { toSave.add(mapper.mapImpDtlUpdateDTOToEntity(dto, transactionPoid, ++maxDetRowId)); }
                }
            }
        }
        if (!toDelete.isEmpty()) impDtlRepository.deleteAllInBatch(toDelete);
        if (!toSave.isEmpty()) impDtlRepository.saveAll(toSave);
    }

    /**
     * Update Import Demurrage Payable detail records
     */
    private void updateDetailRecordsImpPayDtl(Long transactionPoid, List<TariffDetailUpdateDTO> detailDtos) {
        if (detailDtos == null) detailDtos = java.util.Collections.emptyList();
        Map<Long, ShipLineTariffImpPayDtl> existingMap = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid)
                .stream().collect(Collectors.toMap(ShipLineTariffImpPayDtl::getDetRowId, e -> e));
        List<ShipLineTariffImpPayDtl> toDelete = new ArrayList<>();
        List<ShipLineTariffImpPayDtl> toSave = new ArrayList<>();
        long maxDetRowId = existingMap.keySet().stream().mapToLong(Long::longValue).max().orElse(0L);
        for (TariffDetailUpdateDTO dto : detailDtos) {
            String action = dto.getActionType() != null ? dto.getActionType().toLowerCase() : "";
            switch (action) {
                case "isdeleted" -> { if (dto.getDetRowId() != null && existingMap.containsKey(dto.getDetRowId())) toDelete.add(existingMap.get(dto.getDetRowId())); }
                case "iscreated", "isupdated", "" -> {
                    if (dto.getDetRowId() != null && existingMap.containsKey(dto.getDetRowId())) {
                        ShipLineTariffImpPayDtl e = existingMap.get(dto.getDetRowId()); mapper.updateImpPayDtlFromDTO(dto, e); toSave.add(e);
                    } else { toSave.add(mapper.mapImpPayDtlUpdateDTOToEntity(dto, transactionPoid, ++maxDetRowId)); }
                }
            }
        }
        if (!toDelete.isEmpty()) impPayDtlRepository.deleteAllInBatch(toDelete);
        if (!toSave.isEmpty()) impPayDtlRepository.saveAll(toSave);
    }

    /**
     * Update Export Detention Collectable detail records
     */
    private void updateDetailRecordsExpDtl(Long transactionPoid, List<TariffDetailUpdateDTO> detailDtos) {
        if (detailDtos == null) detailDtos = java.util.Collections.emptyList();
        Map<Long, ShipLineTariffExpDtl> existingMap = expDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid)
                .stream().collect(Collectors.toMap(ShipLineTariffExpDtl::getDetRowId, e -> e));
        List<ShipLineTariffExpDtl> toDelete = new ArrayList<>();
        List<ShipLineTariffExpDtl> toSave = new ArrayList<>();
        long maxDetRowId = existingMap.keySet().stream().mapToLong(Long::longValue).max().orElse(0L);
        for (TariffDetailUpdateDTO dto : detailDtos) {
            String action = dto.getActionType() != null ? dto.getActionType().toLowerCase() : "";
            switch (action) {
                case "isdeleted" -> { if (dto.getDetRowId() != null && existingMap.containsKey(dto.getDetRowId())) toDelete.add(existingMap.get(dto.getDetRowId())); }
                case "iscreated", "isupdated", "" -> {
                    if (dto.getDetRowId() != null && existingMap.containsKey(dto.getDetRowId())) {
                        ShipLineTariffExpDtl e = existingMap.get(dto.getDetRowId()); mapper.updateExpDtlFromDTO(dto, e); toSave.add(e);
                    } else { toSave.add(mapper.mapExpDtlUpdateDTOToEntity(dto, transactionPoid, ++maxDetRowId)); }
                }
            }
        }
        if (!toDelete.isEmpty()) expDtlRepository.deleteAllInBatch(toDelete);
        if (!toSave.isEmpty()) expDtlRepository.saveAll(toSave);
    }

    /**
     * Update Export Detention Payable detail records
     */
    private void updateDetailRecordsExpPayDtl(Long transactionPoid, List<TariffDetailUpdateDTO> detailDtos) {
        if (detailDtos == null) detailDtos = java.util.Collections.emptyList();
        Map<Long, ShipLineTariffExpPayDtl> existingMap = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid)
                .stream().collect(Collectors.toMap(ShipLineTariffExpPayDtl::getDetRowId, e -> e));
        List<ShipLineTariffExpPayDtl> toDelete = new ArrayList<>();
        List<ShipLineTariffExpPayDtl> toSave = new ArrayList<>();
        long maxDetRowId = existingMap.keySet().stream().mapToLong(Long::longValue).max().orElse(0L);
        for (TariffDetailUpdateDTO dto : detailDtos) {
            String action = dto.getActionType() != null ? dto.getActionType().toLowerCase() : "";
            switch (action) {
                case "isdeleted" -> { if (dto.getDetRowId() != null && existingMap.containsKey(dto.getDetRowId())) toDelete.add(existingMap.get(dto.getDetRowId())); }
                case "iscreated", "isupdated", "" -> {
                    if (dto.getDetRowId() != null && existingMap.containsKey(dto.getDetRowId())) {
                        ShipLineTariffExpPayDtl e = existingMap.get(dto.getDetRowId()); mapper.updateExpPayDtlFromDTO(dto, e); toSave.add(e);
                    } else { toSave.add(mapper.mapExpPayDtlUpdateDTOToEntity(dto, transactionPoid, ++maxDetRowId)); }
                }
            }
        }
        if (!toDelete.isEmpty()) expPayDtlRepository.deleteAllInBatch(toDelete);
        if (!toSave.isEmpty()) expPayDtlRepository.saveAll(toSave);
    }

    /**
     * Copy detail records from source transaction to target transaction
     */
    private void copyDetailRecords(Long sourceTransactionPoid, Long targetTransactionPoid) {
        copyImpDtlRecords(sourceTransactionPoid, targetTransactionPoid);
        copyImpPayDtlRecords(sourceTransactionPoid, targetTransactionPoid);
        copyExpDtlRecords(sourceTransactionPoid, targetTransactionPoid);
        copyExpPayDtlRecords(sourceTransactionPoid, targetTransactionPoid);
    }

    private void copyImpDtlRecords(Long sourceTransactionPoid, Long targetTransactionPoid) {
        List<ShipLineTariffImpDtl> sourceList = impDtlRepository.findByTransactionPoidOrderByDetRowId(sourceTransactionPoid);
        Long maxDetRowId = 0L;
        for (ShipLineTariffImpDtl source : sourceList) {
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
    }

    private void copyImpPayDtlRecords(Long sourceTransactionPoid, Long targetTransactionPoid) {
        List<ShipLineTariffImpPayDtl> sourceList = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(sourceTransactionPoid);
        Long maxDetRowId = 0L;
        for (ShipLineTariffImpPayDtl source : sourceList) {
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
    }

    private void copyExpDtlRecords(Long sourceTransactionPoid, Long targetTransactionPoid) {
        List<ShipLineTariffExpDtl> sourceList = expDtlRepository.findByTransactionPoidOrderByDetRowId(sourceTransactionPoid);
        Long maxDetRowId = 0L;
        for (ShipLineTariffExpDtl source : sourceList) {
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
    }

    private void copyExpPayDtlRecords(Long sourceTransactionPoid, Long targetTransactionPoid) {
        List<ShipLineTariffExpPayDtl> sourceList = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(sourceTransactionPoid);
        Long maxDetRowId = 0L;
        for (ShipLineTariffExpPayDtl source : sourceList) {
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
     * Validate TariffCreateDTO
     */
    private void validateTariffCreateDTO(LineTariffCreateDTO dto, Long groupPoid) {
        validatePeriodRange(dto.getPeriodFrom(), dto.getPeriodTo());
        validateOverlapForCreate(dto, groupPoid);
        validateDocRefUniqueness(dto.getDocRef());
        validateMutuallyExclusiveFlags(
                dto.getDmgFromSameday(),
                dto.getDmgFromNextday(),
                dto.getDtnFromSameday(),
                dto.getDtnFromNextday()
        );
    }

    /**
     * Validate TariffUpdateDTO
     */
    private void validateTariffUpdateDTO(LineTariffUpdateDTO dto, Long excludeTransactionPoid, Long groupPoid) {
        validatePeriodRange(dto.getPeriodFrom(), dto.getPeriodTo());

        // Get existing tariff to check linePoid if not provided in update
        ShipLineTariffHdr existing = tariffHdrRepository.findById(excludeTransactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LINE_TARIFF, TRANSACTION_POID, excludeTransactionPoid.toString()));

        Long linePoid = dto.getLinePoid() != null ? dto.getLinePoid() : existing.getLinePoid();
        LocalDate periodFrom = dto.getPeriodFrom() != null ? dto.getPeriodFrom() : existing.getPeriodFrom();
        LocalDate periodTo = dto.getPeriodTo() != null ? dto.getPeriodTo() : existing.getPeriodTo();

        validateOverlapForUpdate(linePoid, periodFrom, periodTo, groupPoid, excludeTransactionPoid);

        // Check if document reference already exists (excluding current transaction)
        if (dto.getDocRef() != null
                && !dto.getDocRef().isEmpty()
                && tariffHdrRepository.existsByDocRefExcludingPoid(dto.getDocRef(), excludeTransactionPoid)) {
            throw new ValidationException("Document reference already exists");
        }

        // Validate mutually exclusive flags
        String dmgFromSameday = dto.getDmgFromSameday() != null ? dto.getDmgFromSameday() : existing.getDmgFromSameday();
        String dmgFromNextday = dto.getDmgFromNextday() != null ? dto.getDmgFromNextday() : existing.getDmgFromNextday();
        String dtnFromSameday = dto.getDtnFromSameday() != null ? dto.getDtnFromSameday() : existing.getDtnFromSameday();
        String dtnFromNextday = dto.getDtnFromNextday() != null ? dto.getDtnFromNextday() : existing.getDtnFromNextday();
        validateMutuallyExclusiveFlags(dmgFromSameday, dmgFromNextday, dtnFromSameday, dtnFromNextday);
    }

    private void validatePeriodRange(LocalDate periodFrom, LocalDate periodTo) {
        if (periodFrom != null && periodTo != null && periodFrom.isAfter(periodTo)) {
            throw new ValidationException("Period from date must be less than or equal to period to date");
        }
    }

    private void validateOverlapForCreate(LineTariffCreateDTO dto, Long groupPoid) {
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
    }

    private void validateOverlapForUpdate(
            Long linePoid,
            LocalDate periodFrom,
            LocalDate periodTo,
            Long groupPoid,
            Long excludeTransactionPoid
    ) {
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
    }

    private void validateDocRefUniqueness(String docRef) {
        if (docRef != null && !docRef.trim().isEmpty()) {
            String trimmedDocRef = docRef.trim();
            if (tariffHdrRepository.existsByDocRef(trimmedDocRef)) {
                throw new ValidationException("Document Reference " + trimmedDocRef + " already exists. Please use a different reference.");
            }
        }
    }

    @Override
    @Transactional(timeout = 120)
    public void loadContainerTypes(Long transactionPoid, String type) {
        log.info("Loading container types for transactionPoid: {}, type: {}", transactionPoid, type);

        ShipLineTariffHdr hdr = tariffHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LINE_TARIFF, TRANSACTION_POID, transactionPoid.toString()));

        if ("IMP".equalsIgnoreCase(type)) {
            impDtlRepository.bulkInsertFromLine(transactionPoid, hdr.getLinePoid());
            impPayDtlRepository.bulkInsertFromLine(transactionPoid, hdr.getLinePoid());
        } else {
            expDtlRepository.bulkInsertFromLine(transactionPoid, hdr.getLinePoid());
            expPayDtlRepository.bulkInsertFromLine(transactionPoid, hdr.getLinePoid());
        }
    }

    private Map<Long, ShipContainerTypeMaster> buildContainerTypeMap(
            List<ShipLineTariffImpDtl> impDtlList,
            List<ShipLineTariffImpPayDtl> impPayDtlList,
            List<ShipLineTariffExpDtl> expDtlList,
            List<ShipLineTariffExpPayDtl> expPayDtlList) {

        Set<Long> poids = Stream.of(
                impDtlList.stream().map(ShipLineTariffImpDtl::getContainerTypePoid),
                impPayDtlList.stream().map(ShipLineTariffImpPayDtl::getContainerTypePoid),
                expDtlList.stream().map(ShipLineTariffExpDtl::getContainerTypePoid),
                expPayDtlList.stream().map(ShipLineTariffExpPayDtl::getContainerTypePoid)
        ).flatMap(s -> s).filter(java.util.Objects::nonNull).collect(Collectors.toSet());

        if (poids.isEmpty()) return Map.of();

        return containerTypeRepository.findAllById(poids).stream()
                .collect(Collectors.toMap(ShipContainerTypeMaster::getContainerTypePoid, ct -> ct));
    }

    private void validateContainerTypeExists(Long containerTypePoid) {
        if (containerTypePoid == null || containerTypePoid <= 0) {
            throw new ValidationException("Container type is required");
        }
        if (!containerTypeRepository.existsById(containerTypePoid)) {
            throw new ValidationException("Invalid container type: " + containerTypePoid);
        }
    }

    private void validateMutuallyExclusiveFlags(
            String dmgFromSameday,
            String dmgFromNextday,
            String dtnFromSameday,
            String dtnFromNextday
    ) {
        if ("Y".equals(dmgFromSameday) && "Y".equals(dmgFromNextday)) {
            throw new ValidationException("Same day and Next day both cannot be selected for Demurrage");
        }
        if ("Y".equals(dtnFromSameday) && "Y".equals(dtnFromNextday)) {
            throw new ValidationException("Same day and Next day both cannot be selected for Detention");
        }
    }

    @Override
    @Transactional
    public void copySlabsToPayable(Long id, String type) {
        log.info("Copying slabs to payable for transactionPoid: {}, type: {}", id, type);
        if ("DMG".equalsIgnoreCase(type)) {
            List<ShipLineTariffImpDtl> collectables = impDtlRepository.findByTransactionPoidOrderByDetRowId(id);
            List<ShipLineTariffImpPayDtl> payables = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(id);
            Map<Long, ShipLineTariffImpPayDtl> payableByContainerType = payables.stream()
                    .filter(p -> p.getContainerTypePoid() != null)
                    .collect(Collectors.toMap(ShipLineTariffImpPayDtl::getContainerTypePoid, p -> p, (a, b) -> a));
            for (ShipLineTariffImpDtl col : collectables) {
                if (col.getContainerTypePoid() == null) continue;
                ShipLineTariffImpPayDtl pay = payableByContainerType.get(col.getContainerTypePoid());
                if (pay == null) continue;
                pay.setFreeDays(col.getFreeDays());
                pay.setSlab1Tilldays(col.getSlab1Tilldays()); pay.setSlab1Rate(col.getSlab1Rate());
                pay.setSlab2Tilldays(col.getSlab2Tilldays()); pay.setSlab2Rate(col.getSlab2Rate());
                pay.setSlab3Tilldays(col.getSlab3Tilldays()); pay.setSlab3Rate(col.getSlab3Rate());
                pay.setSlab4Tilldays(col.getSlab4Tilldays()); pay.setSlab4Rate(col.getSlab4Rate());
                pay.setSlab5Tilldays(col.getSlab5Tilldays()); pay.setSlab5Rate(col.getSlab5Rate());
                pay.setSlab6Tilldays(col.getSlab6Tilldays()); pay.setSlab6Rate(col.getSlab6Rate());
                pay.setSlab7Tilldays(col.getSlab7Tilldays()); pay.setSlab7Rate(col.getSlab7Rate());
                impPayDtlRepository.save(pay);
            }
        } else if ("DTN".equalsIgnoreCase(type)) {
            List<ShipLineTariffExpDtl> collectables = expDtlRepository.findByTransactionPoidOrderByDetRowId(id);
            List<ShipLineTariffExpPayDtl> payables = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(id);
            Map<Long, ShipLineTariffExpPayDtl> payableByContainerType = payables.stream()
                    .filter(p -> p.getContainerTypePoid() != null)
                    .collect(Collectors.toMap(ShipLineTariffExpPayDtl::getContainerTypePoid, p -> p, (a, b) -> a));
            for (ShipLineTariffExpDtl col : collectables) {
                if (col.getContainerTypePoid() == null) continue;
                ShipLineTariffExpPayDtl pay = payableByContainerType.get(col.getContainerTypePoid());
                if (pay == null) continue;
                pay.setFreeDays(col.getFreeDays());
                pay.setSlab1Tilldays(col.getSlab1Tilldays()); pay.setSlab1Rate(col.getSlab1Rate());
                pay.setSlab2Tilldays(col.getSlab2Tilldays()); pay.setSlab2Rate(col.getSlab2Rate());
                pay.setSlab3Tilldays(col.getSlab3Tilldays()); pay.setSlab3Rate(col.getSlab3Rate());
                pay.setSlab4Tilldays(col.getSlab4Tilldays()); pay.setSlab4Rate(col.getSlab4Rate());
                pay.setSlab5Tilldays(col.getSlab5Tilldays()); pay.setSlab5Rate(col.getSlab5Rate());
                pay.setSlab6Tilldays(col.getSlab6Tilldays()); pay.setSlab6Rate(col.getSlab6Rate());
                pay.setSlab7Tilldays(col.getSlab7Tilldays()); pay.setSlab7Rate(col.getSlab7Rate());
                expPayDtlRepository.save(pay);
            }
        } else {
            throw new ValidationException("Invalid type. Must be DMG or DTN");
        }
        log.info("Successfully copied slabs to payable for transactionPoid: {}, type: {}", id, type);
    }
}

