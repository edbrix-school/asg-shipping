package com.asg.shipping.linecommission.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.common.repository.ShipLineMasterTypeRepository;
import com.asg.shipping.containertypes.dto.ContainerTypeDto;
import com.asg.shipping.containertypes.entity.ShipContainerTypeMaster;
import com.asg.shipping.linecommission.dto.ContainerRateDto;
import com.asg.shipping.linecommission.dto.LineCommissionResponse;
import com.asg.shipping.linecommission.dto.LineCommissionRequest;
import com.asg.shipping.linecommission.dto.LocalShareDto;
import com.asg.shipping.linecommission.dto.OtherRemunerationDto;
import com.asg.shipping.linecommission.entity.*;
import com.asg.shipping.linecommission.repository.ShipLineCommCntnrDtlRepository;
import com.asg.shipping.linecommission.repository.ShipLineCommDtlRepository;
import com.asg.shipping.linecommission.repository.ShipLineCommHdrRepository;
import com.asg.shipping.linecommission.repository.ShipLineCommLocalDtlRepository;
import com.asg.shipping.linecommission.util.LineCommissionMapper;
import static com.asg.shipping.linecommission.util.Constants.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.asg.common.lib.security.util.UserContext;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LineCommissionServiceImpl implements LineCommissionService {

    private static final Logger log = LoggerFactory.getLogger(LineCommissionServiceImpl.class);

    private final DocumentSearchService documentService;
    private final ShipLineCommHdrRepository hdrRepository;
    private final ShipLineCommCntnrDtlRepository cntnrRepository;
    private final ShipLineCommDtlRepository dtlRepository;
    private final ShipLineCommLocalDtlRepository localRepository;
    private final ShipLineMasterTypeRepository lineTypeRepository;
    private final LineCommissionMapper mapper;
    private final EntityManager entityManager;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;



    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listLineCommissions(String docId, FilterRequestDto request, Pageable pageable) {
        log.info("Listing line commissions with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        // label/value based on list payload expectations
        RawSearchResult raw = documentService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "LINE_NAME",
                "TRANSACTION_POID"
        );

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public LineCommissionResponse getById(Long transactionPoid, Long groupPoid) {
        if (transactionPoid == null) throw new ValidationException("transactionPoid is required");
        if (groupPoid == null) throw new ValidationException("groupPoid is required");
        ShipLineCommHdrEntity hdr = hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("LineCommission", "transactionPoid", transactionPoid));

        List<ShipLineCommCntnrDtlEntity> cntnr = cntnrRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        List<ShipLineCommDtlEntity> dtl = dtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        List<ShipLineCommLocalDtlEntity> local = localRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);

        LineCommissionResponse response = mapper.toResponse(hdr, cntnr, dtl, local);
        enrich(response, groupPoid);

        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());

        return response;
    }

    @Override
    @Transactional
    public LineCommissionResponse create(LineCommissionRequest request, Long groupPoid, String userId, String docId) {
        if (groupPoid == null) throw new ValidationException("groupPoid is required");
        validatePeriod(request.getPeriodFrom(), request.getPeriodTo());
        Long companyPoid = UserContext.getCompanyPoid();

        List<ContainerRateDto> containerRates =
                request.getContainerRates() == null ? List.of() : request.getContainerRates();

        validateAndExtractDistinctContainerTypes(containerRates);

        long overlap = hdrRepository.countOverlapping(
                null,
                request.getLinePoid(),
                request.getPeriodFrom(),
                request.getPeriodTo(),
                groupPoid,
                companyPoid
        );
        if (overlap > 0) {
            throw new ValidationException("Period overlaps with an existing record for this line");
        }

        ShipLineCommHdrEntity hdr = mapper.toCreateHeaderEntity(request, groupPoid, companyPoid, userId);

        ShipLineCommHdrEntity saved = hdrRepository.saveAndFlush(hdr);
        entityManager.refresh(saved);
        SavedDetails savedDetails = saveAllDetails(saved.getTransactionPoid(), request, userId);

        LineCommissionResponse response = mapper.toResponse(saved, savedDetails.containerRates, savedDetails.otherRemunerations, savedDetails.localShares);
        enrich(response, groupPoid);

        String key = saved.getTransactionPoid().toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);

        return response;

    }

    @Override
    @Transactional
    public LineCommissionResponse update(Long transactionPoid, LineCommissionRequest request, Long groupPoid, String userId, String docId) {
        if (transactionPoid == null) throw new ValidationException("transactionPoid is required");
        if (groupPoid == null) throw new ValidationException("groupPoid is required");
        validatePeriod(request.getPeriodFrom(), request.getPeriodTo());
        Long companyPoid = UserContext.getCompanyPoid();

        List<ContainerRateDto> containerRates =
                request.getContainerRates() == null ? List.of() : request.getContainerRates();

        validateAndExtractDistinctContainerTypes(containerRates);

        long overlap = hdrRepository.countOverlapping(
                transactionPoid,
                request.getLinePoid(),
                request.getPeriodFrom(),
                request.getPeriodTo(),
                groupPoid,
                companyPoid
        );
        if (overlap > 0) {
            throw new ValidationException("Period overlaps with an existing record for this line");
        }

        ShipLineCommHdrEntity hdr = hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("LineCommission", "transactionPoid", transactionPoid));

        ShipLineCommHdrEntity oldEntity = new ShipLineCommHdrEntity();
        BeanUtils.copyProperties(hdr, oldEntity);

        mapper.applyUpdateHeader(hdr, request, companyPoid, userId);

        ShipLineCommHdrEntity saved = hdrRepository.save(hdr);

        SavedDetails savedDetails = updateAllDetails(transactionPoid, request, userId);

        // Build response without re-querying detail tables
        LineCommissionResponse response = mapper.toResponse(hdr, savedDetails.containerRates, savedDetails.otherRemunerations, savedDetails.localShares);
        enrich(response, groupPoid);

        String key = saved.getTransactionPoid().toString();
        loggingService.logChanges(oldEntity, saved, ShipLineCommHdrEntity.class, docId, key, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        return response;
    }

    private void validateAndExtractDistinctContainerTypes(List<ContainerRateDto> containerRates) {

        Set<Long> distinctContainerTypePoids = new HashSet<>();

        for (ContainerRateDto containerRate : containerRates) {
            if (containerRate == null) {
                continue;
            }

            Long containerTypePoid = containerRate.getContainerTypePoid(); // mandatory at UI/DTO level
            if (containerTypePoid == null) {
                throw new ValidationException("Container Type is required in Container & Rates");
            }

            String action = resolveActionType(containerRate.getActionType(), containerRate.getDetRowId());
            if (ACTION_IS_DELETED.equals(action)) {
                continue;
            }

            if (!distinctContainerTypePoids.add(containerTypePoid)) {
                throw new ValidationException("Duplicate container type(s) not allowed in Container & Rates");
            }
        }

    }

    @Override
    @Transactional
    public void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        if (transactionPoid == null) throw new ValidationException("transactionPoid is required");
        Long groupPoid = UserContext.getGroupPoid();
        hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("LineCommission", "transactionPoid", transactionPoid));
        documentDeleteService.deleteDocument(
                transactionPoid,
                "SHIP_LINE_COMM_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                LocalDate.now()
        );

    }

    @Override
    @Transactional
    public List<ContainerTypeDto> loadContainerTypes(Long linePoid, Long groupPoid, String userId) {
        if (linePoid == null) throw new ValidationException("Line is required");
        if (groupPoid == null) throw new ValidationException("groupPoid is required");

        // Just load container types linked to selected line (SHIP_LINE_MASTER_TYPE_DTL) and return master details.
       List<ShipContainerTypeMaster> masters =
                lineTypeRepository.findContainerTypeMastersByLine(linePoid);
        if (masters == null || masters.isEmpty()) {
            return List.of();
        }

        return mapper.toContainerTypeDtos(masters);
    }



    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) return; // handled by bean validation
        if (from.isAfter(to)) {
            throw new ValidationException("periodFrom must be less than or equal to periodTo");
        }
    }

    private SavedDetails saveAllDetails(Long transactionPoid, LineCommissionRequest request, String userId) {
        List<ShipLineCommCntnrDtlEntity> containerRates = mapper.toContainerEntities(transactionPoid, request, userId);
        if (!containerRates.isEmpty()) {
            containerRates = cntnrRepository.saveAll(containerRates);
            containerRates.forEach(containerRate -> {
                String logDetail = String.format("Row Created on Container Rate with detRowId: %s", containerRate.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString() , logDetail);
            });
        }

        List<ShipLineCommDtlEntity> otherRemunerations = mapper.toOtherRemunerationEntities(transactionPoid, request, userId);
        if (!otherRemunerations.isEmpty()) {
            otherRemunerations = dtlRepository.saveAll(otherRemunerations);
            otherRemunerations.forEach(otherRemuneration -> {
                String logDetail = String.format("Row Created on Other Remuneration with detRowId: %s", otherRemuneration.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString() , logDetail);
            });
        }

        List<ShipLineCommLocalDtlEntity> localShares = mapper.toLocalShareEntities(transactionPoid, request, userId);
        if (!localShares.isEmpty()) {
            localShares = localRepository.saveAll(localShares);
            localShares.forEach(localShare -> {
                String logDetail = String.format("Row Created on Local Share with detRowId: %s", localShare.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString() , logDetail);
            });
        }

        return new SavedDetails(containerRates, otherRemunerations, localShares);
    }

    private SavedDetails updateAllDetails(Long transactionPoid, LineCommissionRequest request, String userId) {
        List<ShipLineCommCntnrDtlEntity> containerRates = updateContainerRates(transactionPoid, request, userId);
        List<ShipLineCommDtlEntity> otherRemunerations = updateOtherRemunerations(transactionPoid, request, userId);
        List<ShipLineCommLocalDtlEntity> localShares = updateLocalShares(transactionPoid, request, userId);

        return new SavedDetails(containerRates, otherRemunerations, localShares);
    }

    private List<ShipLineCommCntnrDtlEntity> updateContainerRates(Long transactionPoid, LineCommissionRequest request, String userId) {
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        List<ContainerRateDto> detailDtos =
                request.getContainerRates() == null ? List.of() : request.getContainerRates();

        List<ShipLineCommCntnrDtlEntity> existing = cntnrRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        Map<Long, ShipLineCommCntnrDtlEntity> existingByDetRow = existing.stream()
                .filter(e -> e.getDetRowId() != null)
                .collect(Collectors.toMap(ShipLineCommCntnrDtlEntity::getDetRowId, entity -> entity));

        long maxDetRowId = existing.stream()
                .map(ShipLineCommCntnrDtlEntity::getDetRowId)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);

        List<ShipLineCommCntnrDtlEntity> deletions = new ArrayList<>();
        List<ShipLineCommCntnrDtlEntity> toSave = new ArrayList<>();
        List<ShipLineCommCntnrDtlEntity> toUpdate = new ArrayList<>();
        List<LogRequestDto<ShipLineCommCntnrDtlEntity>> logRequests = new ArrayList<>();

        for (ContainerRateDto dto : detailDtos) {
            if (dto == null) continue;
            String action = resolveActionType(dto.getActionType(), dto.getDetRowId());
            switch (action) {
                case ACTION_IS_CREATED -> {
                    Long candidateDetRowId = normalizeDetRowId(dto.getDetRowId());
                    long detRowId = candidateDetRowId == null ? ++maxDetRowId : candidateDetRowId;
                    maxDetRowId = Math.max(maxDetRowId, detRowId);
                    ShipLineCommCntnrDtlEntity containerRate = mapper.toNewContainerEntity(transactionPoid, detRowId, dto, userId);
                    toSave.add(containerRate);
                }
                case ACTION_IS_UPDATED -> {
                    Long detRowIdToUpdate = normalizeDetRowId(dto.getDetRowId());
                    if (detRowIdToUpdate == null) {
                        throw new ValidationException("detRowId is required for updating Container Rate");
                    }
                    ShipLineCommCntnrDtlEntity entity = existingByDetRow.get(detRowIdToUpdate);
                    if (entity == null) {
                        throw new ResourceNotFoundException("ContainerRate", "detRowId", detRowIdToUpdate);
                    }
                    ShipLineCommCntnrDtlEntity oldItem = new ShipLineCommCntnrDtlEntity();
                    BeanUtils.copyProperties(entity, oldItem);
                    mapper.applyUpdateContainerEntity(entity, dto, userId);
                    toUpdate.add(entity);
                    String logDetailForUpdate = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detRowIdToUpdate);
                    logRequests.add(new LogRequestDto<>(oldItem, entity, ShipLineCommCntnrDtlEntity.class, docId, docKeyPoid, logDetailForUpdate));
                }
                case ACTION_IS_DELETED -> {
                    Long detRowIdToDelete = normalizeDetRowId(dto.getDetRowId());
                    if (detRowIdToDelete == null) {
                        throw new ValidationException("detRowId is required for deleting Container Rate");
                    }
                    ShipLineCommCntnrDtlEntity entityToDelete = existingByDetRow.get(detRowIdToDelete);
                    if (entityToDelete == null) {
                        throw new ResourceNotFoundException("ContainerRate", "detRowId", detRowIdToDelete);
                    }
                    deletions.add(entityToDelete);
                }
            }
        }

        if (!deletions.isEmpty()) {
            cntnrRepository.deleteAll(deletions);
            deletions.forEach(deleted -> loggingService.logDelete(deleted, docId, docKeyPoid));
        }

        List<ShipLineCommCntnrDtlEntity> savedItems = List.of();
        if (!toSave.isEmpty()) {
            savedItems = cntnrRepository.saveAll(toSave);
            savedItems.forEach(containerRate -> {
                String logDetail = String.format("Row Created on Container Rate with detRowId: %s", containerRate.getDetRowId());
                loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
            });
        }

        List<ShipLineCommCntnrDtlEntity> updatedItems = List.of();
        if (!toUpdate.isEmpty()) {
            updatedItems = cntnrRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }

        List<ShipLineCommCntnrDtlEntity> allItems = new ArrayList<>(savedItems);
        allItems.addAll(updatedItems);
        allItems.sort(Comparator.comparing(ShipLineCommCntnrDtlEntity::getDetRowId, Comparator.nullsFirst(Long::compareTo)));
        return allItems;
    }

    private List<ShipLineCommDtlEntity> updateOtherRemunerations(Long transactionPoid, LineCommissionRequest request, String userId) {
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        List<OtherRemunerationDto> detailDtos =
                request.getOtherRemunerations() == null ? List.of() : request.getOtherRemunerations();

        List<ShipLineCommDtlEntity> existing = dtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        Map<Long, ShipLineCommDtlEntity> existingByDetRow = existing.stream()
                .filter(e -> e.getDetRowId() != null)
                .collect(Collectors.toMap(ShipLineCommDtlEntity::getDetRowId, entity -> entity));

        long maxDetRowId = existing.stream()
                .map(ShipLineCommDtlEntity::getDetRowId)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);

        List<ShipLineCommDtlEntity> deletions = new ArrayList<>();
        List<ShipLineCommDtlEntity> toSave = new ArrayList<>();
        List<ShipLineCommDtlEntity> toUpdate = new ArrayList<>();
        List<LogRequestDto<ShipLineCommDtlEntity>> logRequests = new ArrayList<>();

        for (OtherRemunerationDto dto : detailDtos) {
            if (dto == null) continue;
            String action = resolveActionType(dto.getActionType(), dto.getDetRowId());
            switch (action) {
                case ACTION_IS_CREATED -> {
                    Long candidateDetRowId = normalizeDetRowId(dto.getDetRowId());
                    long detRowId = candidateDetRowId == null ? ++maxDetRowId : candidateDetRowId;
                    maxDetRowId = Math.max(maxDetRowId, detRowId);
                    ShipLineCommDtlEntity entity = mapper.toNewOtherRemunerationEntity(transactionPoid, detRowId, dto, userId);
                    toSave.add(entity);
                }
                case ACTION_IS_UPDATED -> {
                    Long detRowIdToUpdate = normalizeDetRowId(dto.getDetRowId());
                    if (detRowIdToUpdate == null) {
                        throw new ValidationException("detRowId is required for updating Other Remuneration");
                    }
                    ShipLineCommDtlEntity entity = existingByDetRow.get(detRowIdToUpdate);
                    if (entity == null) {
                        throw new ResourceNotFoundException("OtherRemuneration", "detRowId", detRowIdToUpdate);
                    }
                    ShipLineCommDtlEntity oldItem = new ShipLineCommDtlEntity();
                    BeanUtils.copyProperties(entity, oldItem);
                    mapper.applyUpdateOtherRemunerationEntity(entity, dto, userId);
                    toUpdate.add(entity);
                    String logDetailForUpdate = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detRowIdToUpdate);
                    logRequests.add(new LogRequestDto<>(oldItem, entity, ShipLineCommDtlEntity.class, docId, docKeyPoid, logDetailForUpdate));
                }
                case ACTION_IS_DELETED -> {
                    Long detRowIdToDelete = normalizeDetRowId(dto.getDetRowId());
                    if (detRowIdToDelete == null) {
                        throw new ValidationException("detRowId is required for deleting Other Remuneration");
                    }
                    ShipLineCommDtlEntity entityToDelete = existingByDetRow.get(detRowIdToDelete);
                    if (entityToDelete == null) {
                        throw new ResourceNotFoundException("OtherRemuneration", "detRowId", detRowIdToDelete);
                    }
                    deletions.add(entityToDelete);
                }
            }
        }

        if (!deletions.isEmpty()) {
            dtlRepository.deleteAll(deletions);
            deletions.forEach(deleted -> loggingService.logDelete(deleted, docId, docKeyPoid));
        }

        List<ShipLineCommDtlEntity> savedItems = List.of();
        if (!toSave.isEmpty()) {
            savedItems = dtlRepository.saveAll(toSave);
            savedItems.forEach(otherRemuneration -> {
                String logDetail = String.format("Row Created on Other Remuneration with detRowId: %s", otherRemuneration.getDetRowId());
                loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
            });
        }

        List<ShipLineCommDtlEntity> updatedItems = List.of();
        if (!toUpdate.isEmpty()) {
            updatedItems = dtlRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }

        List<ShipLineCommDtlEntity> allItems = new ArrayList<>(savedItems);
        allItems.addAll(updatedItems);
        allItems.sort(Comparator.comparing(ShipLineCommDtlEntity::getDetRowId, Comparator.nullsFirst(Long::compareTo)));
        return allItems;
    }

    private List<ShipLineCommLocalDtlEntity> updateLocalShares(Long transactionPoid, LineCommissionRequest request, String userId) {
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        List<LocalShareDto> detailDtos =
                request.getLocalShares() == null ? List.of() : request.getLocalShares();

        List<ShipLineCommLocalDtlEntity> existing = localRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        Map<Long, ShipLineCommLocalDtlEntity> existingByDetRow = existing.stream()
                .filter(e -> e.getDetRowId() != null)
                .collect(Collectors.toMap(ShipLineCommLocalDtlEntity::getDetRowId, entity -> entity));

        long maxDetRowId = existing.stream()
                .map(ShipLineCommLocalDtlEntity::getDetRowId)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);

        List<ShipLineCommLocalDtlEntity> deletions = new ArrayList<>();
        List<ShipLineCommLocalDtlEntity> toSave = new ArrayList<>();
        List<ShipLineCommLocalDtlEntity> toUpdate = new ArrayList<>();
        List<LogRequestDto<ShipLineCommLocalDtlEntity>> logRequests = new ArrayList<>();

        for (LocalShareDto dto : detailDtos) {
            if (dto == null) continue;
            String action = resolveActionType(dto.getActionType(), dto.getDetRowId());
            switch (action) {
                case ACTION_IS_CREATED -> {
                    Long candidateDetRowId = normalizeDetRowId(dto.getDetRowId());
                    long detRowId = candidateDetRowId == null ? ++maxDetRowId : candidateDetRowId;
                    maxDetRowId = Math.max(maxDetRowId, detRowId);
                    ShipLineCommLocalDtlEntity entity = mapper.toNewLocalShareEntity(transactionPoid, detRowId, dto, userId);
                    toSave.add(entity);
                }
                case ACTION_IS_UPDATED -> {
                    Long detRowIdToUpdate = normalizeDetRowId(dto.getDetRowId());
                    if (detRowIdToUpdate == null) {
                        throw new ValidationException("detRowId is required for updating Local Share");
                    }
                    ShipLineCommLocalDtlEntity entity = existingByDetRow.get(detRowIdToUpdate);
                    if (entity == null) {
                        throw new ResourceNotFoundException("LocalShare", "detRowId", detRowIdToUpdate);
                    }
                    ShipLineCommLocalDtlEntity oldItem = new ShipLineCommLocalDtlEntity();
                    BeanUtils.copyProperties(entity, oldItem);
                    mapper.applyUpdateLocalShareEntity(entity, dto, userId);
                    toUpdate.add(entity);
                    String logDetailForUpdate = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detRowIdToUpdate);
                    logRequests.add(new LogRequestDto<>(oldItem, entity, ShipLineCommLocalDtlEntity.class, docId, docKeyPoid, logDetailForUpdate));
                }
                case ACTION_IS_DELETED -> {
                    Long detRowIdToDelete = normalizeDetRowId(dto.getDetRowId());
                    if (detRowIdToDelete == null) {
                        throw new ValidationException("detRowId is required for deleting Local Share");
                    }
                    ShipLineCommLocalDtlEntity entityToDelete = existingByDetRow.get(detRowIdToDelete);
                    if (entityToDelete == null) {
                        throw new ResourceNotFoundException("LocalShare", "detRowId", detRowIdToDelete);
                    }
                    deletions.add(entityToDelete);
                }
            }
        }

        if (!deletions.isEmpty()) {
            localRepository.deleteAll(deletions);
            deletions.forEach(deleted -> loggingService.logDelete(deleted, docId, docKeyPoid));
        }

        List<ShipLineCommLocalDtlEntity> savedItems = List.of();
        if (!toSave.isEmpty()) {
            savedItems = localRepository.saveAll(toSave);
            savedItems.forEach(localShare -> {
                String logDetail = String.format("Row Created on Local Share with detRowId: %s", localShare.getDetRowId());
                loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
            });
        }

        List<ShipLineCommLocalDtlEntity> updatedItems = List.of();
        if (!toUpdate.isEmpty()) {
            updatedItems = localRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }

        List<ShipLineCommLocalDtlEntity> allItems = new ArrayList<>(savedItems);
        allItems.addAll(updatedItems);
        allItems.sort(Comparator.comparing(ShipLineCommLocalDtlEntity::getDetRowId, Comparator.nullsFirst(Long::compareTo)));
        return allItems;
    }

    private String resolveActionType(String actionType, Long detRowId) {
        if (StringUtils.isBlank(actionType)) {
            return normalizeDetRowId(detRowId) == null ? ACTION_IS_CREATED : ACTION_IS_UPDATED;
        }
        return actionType.trim();
    }

    private Long normalizeDetRowId(Long detRowId) {
        if (detRowId == null || detRowId == 0L) {
            return null;
        }
        return detRowId;
    }

    private record SavedDetails(
            List<ShipLineCommCntnrDtlEntity> containerRates,
            List<ShipLineCommDtlEntity> otherRemunerations,
            List<ShipLineCommLocalDtlEntity> localShares
    ) {}

    public void enrich(LineCommissionResponse response, Long groupPoid) {
        if (response == null) return;

        Set<Long> linePoids = new HashSet<>();
        Set<Long> currencyPoids = new HashSet<>();
        Set<Long> containerTypePoids = new HashSet<>();
        Set<Long> chargePoids = new HashSet<>();
        Set<Long> remunerationPoids = new HashSet<>();

        if (response.getLinePoid() != null) {
            linePoids.add(response.getLinePoid());
        }
        if (response.getCurrencyPoid() != null) {
            currencyPoids.add(response.getCurrencyPoid());
        }

        if (response.getContainerRates() != null) {
            for (ContainerRateDto r : response.getContainerRates()) {
                if (r != null && r.getContainerTypePoid() != null) {
                    containerTypePoids.add(r.getContainerTypePoid());
                }
            }
        }

        if (response.getOtherRemunerations() != null) {
            for (OtherRemunerationDto r : response.getOtherRemunerations()) {
                if (r == null) continue;
                if (r.getRemunerationPoid() != null) remunerationPoids.add(r.getRemunerationPoid());
                if (r.getCurrencyPoid() != null) currencyPoids.add(r.getCurrencyPoid());
            }
        }

        if (response.getLocalShares() != null) {
            for (LocalShareDto r : response.getLocalShares()) {
                if (r != null && r.getChargePoid() != null) {
                    chargePoids.add(r.getChargePoid());
                }
            }
        }

        LovMaps maps = fetchLovMaps(
                safeInList(linePoids),
                safeInList(currencyPoids),
                safeInList(containerTypePoids),
                safeInList(chargePoids),
                safeInList(remunerationPoids),
                groupPoid
        );

        // Header
        if (response.getLinePoid() != null) {
            response.setLineDet(maps.lineByPoid.get(response.getLinePoid()));
        }
        if (response.getCurrencyPoid() != null) {
            response.setCurrencyDet(maps.currencyByPoid.get(response.getCurrencyPoid()));
        }

        // Details
        if (response.getContainerRates() != null) {
            for (ContainerRateDto r : response.getContainerRates()) {
                if (r == null || r.getContainerTypePoid() == null) continue;
                r.setContainerTypeDet(maps.containerTypeByPoid.get(r.getContainerTypePoid()));
            }
        }

        if (response.getOtherRemunerations() != null) {
            for (OtherRemunerationDto r : response.getOtherRemunerations()) {
                if (r == null) continue;
                if (r.getRemunerationPoid() != null) {
                    r.setRemunerationDet(maps.remunerationByPoid.get(r.getRemunerationPoid()));
                }
                if (r.getCurrencyPoid() != null) {
                    r.setCurrencyDet(maps.currencyByPoid.get(r.getCurrencyPoid()));
                }
            }
        }

        if (response.getLocalShares() != null) {
            for (LocalShareDto r : response.getLocalShares()) {
                if (r == null || r.getChargePoid() == null) continue;
                r.setChargeDet(maps.chargeByPoid.get(r.getChargePoid()));
            }
        }
    }

    private LovMaps fetchLovMaps(List<Long> linePoids,
                                 List<Long> currencyPoids,
                                 List<Long> containerTypePoids,
                                 List<Long> chargePoids,
                                 List<Long> remunerationPoids,
                                 Long groupPoid) {

        Map<Long, LovGetListDto> lineByPoid = fetchLineDetails(linePoids, groupPoid);
        Map<Long, LovGetListDto> currencyByPoid = fetchCurrencyDetails(currencyPoids);
        Map<Long, LovGetListDto> containerTypeByPoid = fetchContainerTypeDetails(containerTypePoids);
        Map<Long, LovGetListDto> chargeByPoid = fetchChargeDetails(chargePoids);
        Map<Long, LovGetListDto> remunerationByPoid = fetchRemunerationDetails(remunerationPoids);

        return new LovMaps(lineByPoid, currencyByPoid, containerTypeByPoid, chargeByPoid, remunerationByPoid);
    }

    private Map<Long, LovGetListDto> fetchLineDetails(List<Long> linePoids, Long groupPoid) {
        String sql = """
                SELECT LINE_POID AS POID,
                       LINE_CODE AS CODE,
                       LINE_NAME AS DESCRIPTION
                  FROM SHIP_LINE_MASTER
                 WHERE LINE_POID IN (:poids)
                   AND (:groupPoid IS NULL OR GROUP_POID = :groupPoid)
                """;
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("poids", linePoids);
        q.setParameter("groupPoid", groupPoid);
        return toLovMap(q);
    }

    private Map<Long, LovGetListDto> fetchCurrencyDetails(List<Long> currencyPoids) {
        String sql = """
                SELECT CURRENCY_POID AS POID,
                       CURRENCY_CODE AS CODE,
                       CURRENCY_NAME AS DESCRIPTION
                  FROM GLOBAL_CURRENCY_MASTER
                 WHERE CURRENCY_POID IN (:poids)
                """;
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("poids", currencyPoids);
        return toLovMap(q);
    }

    private Map<Long, LovGetListDto> fetchContainerTypeDetails(List<Long> containerTypePoids) {
        String sql = """
                SELECT CONTAINER_TYPE_POID AS POID,
                       CONTAINER_TYPE_CODE AS CODE,
                       (CONTAINER_TYPE_CODE || '-' || CONTAINER_TYPE_NAME) AS DESCRIPTION
                  FROM SHIP_CONTAINER_TYPE_MASTER
                 WHERE CONTAINER_TYPE_POID IN (:poids)
                   AND ACTIVE = 'Y'
                """;
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("poids", containerTypePoids);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        Map<Long, LovGetListDto> out = new HashMap<>();
        for (Object[] row : rows) {
            // row[0]=POID, row[1]=CODE, row[2]=DESCRIPTION
            if (row == null || row.length < 3) continue;
            Long poid = toLong(row[0]);
            if (poid == null) continue;
            String code = row[1] != null ? row[1].toString() : null;
            String desc = row[2] != null ? row[2].toString() : null;
            out.put(poid, toLovGetListDto(poid, code, desc));
        }
        return out;
    }

    private Map<Long, LovGetListDto> fetchChargeDetails(List<Long> chargePoids) {
        String sql = """
                SELECT CHARGE_POID AS POID,
                       CHARGE_CODE AS CODE,
                       CHARGE_NAME AS DESCRIPTION
                  FROM SHIP_CHARGE_MASTER
                 WHERE CHARGE_POID IN (:poids)
                   AND NVL(ACTIVE,'N')='Y'
                   AND DIVISION_CODE IN ('SH','ALL')
                """;
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("poids", chargePoids);
        return toLovMap(q);
    }

    private Map<Long, LovGetListDto> fetchRemunerationDetails(List<Long> remunerationPoids) {
        String sql = """
                SELECT REMUNERATION_POID AS POID,
                       REMUN_CODE AS CODE,
                       (REMUN_DESCRIPTION || ' - ' || IMP_EXP_TYPE) AS DESCRIPTION
                  FROM SHIP_REMUNERATION_MASTER
                 WHERE REMUNERATION_POID IN (:poids)
                """;
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("poids", remunerationPoids);
        return toLovMap(q);
    }

    private Map<Long, LovGetListDto> toLovMap(Query q) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        Map<Long, LovGetListDto> out = new HashMap<>();
        for (Object[] row : rows) {
            // row[0]=POID, row[1]=CODE, row[2]=DESCRIPTION
            if (row == null || row.length < 3) continue;
            Long poid = toLong(row[0]);
            if (poid == null) continue;
            String code = row[1] != null ? row[1].toString() : null;
            String desc = row[2] != null ? row[2].toString() : null;
            out.put(poid, toLovGetListDto(poid, code, desc));
        }
        return out;
    }

    private static LovGetListDto toLovGetListDto(Long poid, String code, String description) {
        // We only enrich what our endpoints need: POID + CODE + display text.
        // Keep unused fields null to avoid misleading semantics.
        // Convention requested:
        // - label = description (UI display)
        // - value = poid (common-lib LOV usage)
        // - seqNo/users left null
        return new LovGetListDto(poid, code, description, poid, description, null, null);
    }

    private static List<Long> safeInList(Set<Long> set) {
        if (set == null || set.isEmpty()) {
            return List.of(-1L);
        }
        return new ArrayList<>(set);
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof BigDecimal bd) return bd.longValue();
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private record LovMaps(
            Map<Long, LovGetListDto> lineByPoid,
            Map<Long, LovGetListDto> currencyByPoid,
            Map<Long, LovGetListDto> containerTypeByPoid,
            Map<Long, LovGetListDto> chargeByPoid,
            Map<Long, LovGetListDto> remunerationByPoid
    ) {}
}


