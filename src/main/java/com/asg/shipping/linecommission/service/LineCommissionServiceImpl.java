package com.asg.shipping.linecommission.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentSearchService;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.asg.common.lib.security.util.UserContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

        if(!containerRates.isEmpty()){
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
        }

        ShipLineCommHdrEntity hdr = mapper.toCreateHeaderEntity(request, groupPoid, companyPoid, userId);

        ShipLineCommHdrEntity saved = hdrRepository.saveAndFlush(hdr);
        entityManager.refresh(saved);
        SavedDetails savedDetails = saveAllDetails(saved.getTransactionPoid(), request, userId);

        LineCommissionResponse response = mapper.toResponse(saved, savedDetails.containerRates, savedDetails.otherRemunerations, savedDetails.localShares);
        enrich(response, groupPoid);
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

        if(!containerRates.isEmpty()) {
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
        }

        ShipLineCommHdrEntity hdr = hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("LineCommission", "transactionPoid", transactionPoid));

        mapper.applyUpdateHeader(hdr, request, companyPoid, userId);

        hdrRepository.save(hdr);

        SavedDetails savedDetails = updateAllDetails(transactionPoid, request, userId);

        // Build response without re-querying detail tables
        LineCommissionResponse response = mapper.toResponse(hdr, savedDetails.containerRates, savedDetails.otherRemunerations, savedDetails.localShares);
        enrich(response, groupPoid);
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

            if (!distinctContainerTypePoids.add(containerTypePoid)) {
                throw new ValidationException("Duplicate container type(s) not allowed in Container & Rates");
            }
        }

    }

    @Override
    @Transactional
    public void delete(Long transactionPoid, Long groupPoid, String userId) {
        if (transactionPoid == null) throw new ValidationException("transactionPoid is required");
        if (groupPoid == null) throw new ValidationException("groupPoid is required");
        ShipLineCommHdrEntity hdr = hdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("LineCommission", "transactionPoid", transactionPoid));
        hdr.setDeleted("Y");
        hdr.setLastModifiedBy(userId);
        hdr.setLastModifiedDate(LocalDateTime.now());
        hdrRepository.save(hdr);
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

    private SavedDetails saveAllDetails(long transactionPoid, LineCommissionRequest request, String userId) {
        List<ShipLineCommCntnrDtlEntity> containerRates = mapper.toContainerEntities(transactionPoid, request, userId);
        if (!containerRates.isEmpty()) {
            cntnrRepository.saveAll(containerRates);
        }

        List<ShipLineCommDtlEntity> otherRemunerations = mapper.toOtherRemunerationEntities(transactionPoid, request, userId);
        if (!otherRemunerations.isEmpty()) {
            dtlRepository.saveAll(otherRemunerations);
        }

        List<ShipLineCommLocalDtlEntity> localShares = mapper.toLocalShareEntities(transactionPoid, request, userId);
        if (!localShares.isEmpty()) {
            localRepository.saveAll(localShares);
        }

        return new SavedDetails(containerRates, otherRemunerations, localShares);
    }

    private SavedDetails updateAllDetails(long transactionPoid, LineCommissionRequest request, String userId) {
        updateContainerRates(transactionPoid, request, userId);
        updateOtherRemunerations(transactionPoid, request, userId);
        updateLocalShares(transactionPoid, request, userId);

        // Re-read for ordered, authoritative state
        List<ShipLineCommCntnrDtlEntity> containerRates = cntnrRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        List<ShipLineCommDtlEntity> otherRemunerations = dtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        List<ShipLineCommLocalDtlEntity> localShares = localRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        return new SavedDetails(containerRates, otherRemunerations, localShares);
    }

    private void updateContainerRates(long transactionPoid, LineCommissionRequest request, String userId) {
        List<ContainerRateDto> detailDtos =
                request.getContainerRates() == null ? List.of() : request.getContainerRates();

        List<ShipLineCommCntnrDtlEntity> existing = cntnrRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);

        Set<Long> existingDetRowIds = existing.stream()
                .map(ShipLineCommCntnrDtlEntity::getDetRowId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> requestDetRowIds = detailDtos.stream()
                .filter(Objects::nonNull)
                .map(ContainerRateDto::getDetRowId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Delete removed
        for (Long detRowId : existingDetRowIds) {
            if (!requestDetRowIds.contains(detRowId)) {
                cntnrRepository.deleteById(new ShipLineCommCntnrDtlId(transactionPoid, detRowId));
            }
        }

        long maxDetRowId = existingDetRowIds.stream().mapToLong(Long::longValue).max().orElse(0L);

        // Update existing / create new
        for (ContainerRateDto dto : detailDtos) {
            if (dto == null) continue;
            if (dto.getDetRowId() != null) {
                ShipLineCommCntnrDtlEntity e = cntnrRepository
                        .findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                        .orElseThrow(() -> new ResourceNotFoundException("ContainerRate", "sn", dto.getDetRowId()));
                mapper.applyUpdateContainerEntity(e, dto, userId);
                cntnrRepository.save(e);
            } else {
                maxDetRowId++;
                ShipLineCommCntnrDtlEntity e = mapper.toNewContainerEntity(transactionPoid, maxDetRowId, dto, userId);
                cntnrRepository.save(e);
            }
        }
    }

    private void updateOtherRemunerations(long transactionPoid, LineCommissionRequest request, String userId) {
        List<OtherRemunerationDto> detailDtos =
                request.getOtherRemunerations() == null ? List.of() : request.getOtherRemunerations();

        List<ShipLineCommDtlEntity> existing = dtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);

        Set<Long> existingDetRowIds = existing.stream()
                .map(ShipLineCommDtlEntity::getDetRowId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> requestDetRowIds = detailDtos.stream()
                .filter(Objects::nonNull)
                .map(OtherRemunerationDto::getDetRowId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Long detRowId : existingDetRowIds) {
            if (!requestDetRowIds.contains(detRowId)) {
                dtlRepository.deleteById(new ShipLineCommDtlId(transactionPoid, detRowId));
            }
        }

        long maxDetRowId = existingDetRowIds.stream().mapToLong(Long::longValue).max().orElse(0L);

        for (OtherRemunerationDto dto : detailDtos) {
            if (dto == null) continue;
            if (dto.getDetRowId() != null) {
                ShipLineCommDtlEntity e = dtlRepository
                        .findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                        .orElseThrow(() -> new ResourceNotFoundException("OtherRemuneration", "sn", dto.getDetRowId()));
                mapper.applyUpdateOtherRemunerationEntity(e, dto, userId);
                dtlRepository.save(e);
            } else {
                maxDetRowId++;
                ShipLineCommDtlEntity e = mapper.toNewOtherRemunerationEntity(transactionPoid, maxDetRowId, dto, userId);
                dtlRepository.save(e);
            }
        }
    }

    private void updateLocalShares(long transactionPoid, LineCommissionRequest request, String userId) {
        List<LocalShareDto> detailDtos =
                request.getLocalShares() == null ? List.of() : request.getLocalShares();

        List<ShipLineCommLocalDtlEntity> existing = localRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);

        Set<Long> existingDetRowIds = existing.stream()
                .map(ShipLineCommLocalDtlEntity::getDetRowId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> requestDetRowIds = detailDtos.stream()
                .filter(Objects::nonNull)
                .map(LocalShareDto::getDetRowId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Long detRowId : existingDetRowIds) {
            if (!requestDetRowIds.contains(detRowId)) {
                localRepository.deleteById(new ShipLineCommLocalDtlId(transactionPoid, detRowId));
            }
        }

        long maxDetRowId = existingDetRowIds.stream().mapToLong(Long::longValue).max().orElse(0L);

        for (LocalShareDto dto : detailDtos) {
            if (dto == null) continue;
            if (dto.getDetRowId() != null) {
                ShipLineCommLocalDtlEntity e = localRepository
                        .findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                        .orElseThrow(() -> new ResourceNotFoundException("LocalShare", "sn", dto.getDetRowId()));
                mapper.applyUpdateLocalShareEntity(e, dto, userId);
                localRepository.save(e);
            } else {
                maxDetRowId++;
                ShipLineCommLocalDtlEntity e = mapper.toNewLocalShareEntity(transactionPoid, maxDetRowId, dto, userId);
                localRepository.save(e);
            }
        }
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


