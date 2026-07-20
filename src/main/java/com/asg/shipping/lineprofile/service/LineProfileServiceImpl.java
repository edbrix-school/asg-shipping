package com.asg.shipping.lineprofile.service;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.lineprofile.dto.LineProfileAgreementDetailsResponse;
import com.asg.shipping.lineprofile.dto.LineProfileContactDto;
import com.asg.shipping.lineprofile.dto.LineProfileLineDetailsResponse;
import com.asg.shipping.lineprofile.dto.LineProfileRequest;
import com.asg.shipping.lineprofile.dto.LineProfileResponse;
import com.asg.shipping.lineprofile.entity.ShipLineProfileContactDtlEntity;
import com.asg.shipping.lineprofile.entity.ShipLineProfileMasterEntity;
import com.asg.shipping.lineprofile.repository.ShipLineProfileContactDtlRepository;
import com.asg.shipping.lineprofile.repository.ShipLineProfileMasterRepository;
import com.asg.shipping.lineprofile.util.LineProfileMapper;
import com.asg.shipping.tradelanemaster.entity.ShipTradelaneMaster;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.asg.common.lib.security.util.UserContext;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.asg.shipping.lineprofile.util.Constants.ACTION_IS_CREATED;
import static com.asg.shipping.lineprofile.util.Constants.ACTION_IS_DELETED;
import static com.asg.shipping.lineprofile.util.Constants.ACTION_IS_UPDATED;
import static com.asg.shipping.lineprofile.util.Constants.ACTION_NO_CHANGE;

@Service
@RequiredArgsConstructor
@Slf4j
public class LineProfileServiceImpl implements LineProfileService {

    private final DocumentSearchService documentService;
    private final ShipLineProfileMasterRepository masterRepository;
    private final ShipLineProfileContactDtlRepository contactRepository;
    private final DocumentDeleteService deleteService;
    private final LineProfileMapper mapper;
    private final EntityManager entityManager;
    private final LoggingService loggingService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listLineProfiles(String docId, FilterRequestDto request, Pageable pageable) {
        log.info("Listing line profiles with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "LINE_NAME",
                "LINE_PROFILE_POID"
        );

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public LineProfileResponse getById(Long lineProfilePoid, Long groupPoid) {
        if (lineProfilePoid == null) throw new ValidationException("lineProfilePoid is required");
        if (groupPoid == null) throw new ValidationException("groupPoid is required");

        ShipLineProfileMasterEntity master = masterRepository
                .findByLineProfilePoidAndGroupPoid(lineProfilePoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("LineProfile", "lineProfilePoid", lineProfilePoid));

        List<ShipLineProfileContactDtlEntity> contacts =
                contactRepository.findByLineProfilePoidOrderByDetRowId(lineProfilePoid);

        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), lineProfilePoid.toString());

        LineProfileResponse response = mapper.toResponse(master, contacts);
        enrich(response, groupPoid);
        return response;
    }

    @Override
    @Transactional
    public LineProfileResponse create(LineProfileRequest request, Long groupPoid, String userId, String docId) {
        if (groupPoid == null) throw new ValidationException("groupPoid is required");
        if (userId == null) throw new ValidationException("userId is required");

        if (request.getLinePoid() != null && masterRepository.existsByLinePoidAndGroupPoidAndDeleted(request.getLinePoid(), groupPoid, "N"))
            throw new ValidationException("Duplicate Line Profile already exists");

        ShipLineProfileMasterEntity master = mapper.toCreateEntity(request, groupPoid, userId);
        ShipLineProfileMasterEntity saved = masterRepository.saveAndFlush(master);
        entityManager.refresh(saved);

        upsertContactDetails(saved.getLineProfilePoid(), request.getContactDetails(), userId, docId);

        List<ShipLineProfileContactDtlEntity> contacts =
                contactRepository.findByLineProfilePoidOrderByDetRowId(saved.getLineProfilePoid());

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, saved.getLineProfilePoid().toString());
        LineProfileResponse response = mapper.toResponse(saved, contacts);
        enrich(response, groupPoid);
        return response;
    }

    @Override
    @Transactional
    public LineProfileResponse update(Long lineProfilePoid, LineProfileRequest request, Long groupPoid, String userId, String docId) {
        if (lineProfilePoid == null) throw new ValidationException("lineProfilePoid is required");
        if (groupPoid == null) throw new ValidationException("groupPoid is required");
        if (userId == null) throw new ValidationException("userId is required");

        ShipLineProfileMasterEntity master = masterRepository
                .findByLineProfilePoidAndGroupPoid(lineProfilePoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("LineProfile", "lineProfilePoid", lineProfilePoid));

        ShipLineProfileMasterEntity oldEntity = new ShipLineProfileMasterEntity();
        BeanUtils.copyProperties(master, oldEntity);

        if (request.getLinePoid() != null && masterRepository.existsByLinePoidAndGroupPoidAndDeletedAndLineProfilePoidNot(request.getLinePoid(), groupPoid, "N", lineProfilePoid))
            throw new ValidationException("Duplicate Line Profile already exists");

        mapper.applyUpdate(master, request, userId);
        ShipLineProfileMasterEntity saved = masterRepository.save(master);

        upsertContactDetails(lineProfilePoid, request.getContactDetails(), userId, docId);

        List<ShipLineProfileContactDtlEntity> contacts =
                contactRepository.findByLineProfilePoidOrderByDetRowId(lineProfilePoid);

        loggingService.logChanges(
                oldEntity,
                saved,
                ShipLineProfileMasterEntity.class,
                docId,
                saved.getLineProfilePoid().toString(),
                LogDetailsEnum.MODIFIED,
                "LINE_PROFILE_POID"
        );
        LineProfileResponse response = mapper.toResponse(saved, contacts);
        enrich(response, groupPoid);
        return response;
    }

    @Override
    @Transactional
    public void delete(Long lineProfilePoid, DeleteReasonDto deleteReasonDto) {

        ShipLineProfileMasterEntity master = masterRepository
                .findByLineProfilePoidAndGroupPoid(lineProfilePoid, UserContext.getGroupPoid())
                .orElseThrow(() -> new ResourceNotFoundException("LineProfile", "lineProfilePoid", lineProfilePoid));

        deleteService.deleteDocument(lineProfilePoid,"SH_LINE_PROFILE_MASTER",
                    "LINE_PROFILE_POID",deleteReasonDto,null);
        }


    @Override
    @Transactional(readOnly = true)
    public LineProfileLineDetailsResponse fetchLineDetails(Long linePoid, Long groupPoid, Long companyPoid, Long userPoid) {
        if (linePoid == null) throw new ValidationException("linePoid is required");

        try {
            var query = entityManager.createStoredProcedureQuery("PROC_SH_LINE_PROFILE");
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LINE_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            query.setParameter("P_LOGIN_GROUP_POID", groupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
            query.setParameter("P_LOGIN_USER_POID", userPoid);
            query.setParameter("P_COMPANY_POID", companyPoid);
            query.setParameter("P_LINE_POID", String.valueOf(linePoid));

            query.execute();

            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
            if (rs == null || !rs.next()) {
                return null;
            }

            LineProfileLineDetailsResponse response = new LineProfileLineDetailsResponse();
            response.setLinePoid(toLong(rs.getObject("LINE_POID")));
            response.setLineCode(rs.getString("LINE_CODE"));
            response.setLineName(rs.getString("LINE_NAME"));
            Long countryPoid = toLong(rs.getObject("COUNTRY_POID"));
            response.setCountryPoid(countryPoid);
            if (countryPoid != null) {
                response.setCountryDet(toLovGetListDto(
                        countryPoid,
                        rs.getString("COUNTRY_CODE"),
                        rs.getString("COUNTRY_NAME")
                ));
            }
            String agencyTypeCode = rs.getString("MIS_LINE_CATEGORY");
            LovGetListDto agencyLov = resolveAgencyTypeLov(agencyTypeCode);
            if (agencyLov != null) {
                response.setAgencyPoid(agencyLov.getPoid());
                response.setAgencyTypeDet(agencyLov);
            }

            return response;
        } catch (Exception e) {
            throw new ValidationException("Error executing PROC_SH_LINE_PROFILE: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LineProfileAgreementDetailsResponse fetchAgreementDetails(Long transactionPoid, Long groupPoid, Long companyPoid, Long userPoid) {
        if (transactionPoid == null) throw new ValidationException("transactionPoid is required");

        try {
            var query = entityManager.createStoredProcedureQuery("PROC_SH_CONTRACTS_AGREEMENT");
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_TRANSACTION_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            query.setParameter("P_LOGIN_GROUP_POID", groupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
            query.setParameter("P_LOGIN_USER_POID", userPoid);
            query.setParameter("P_COMPANY_POID", companyPoid);
            query.setParameter("P_TRANSACTION_POID", String.valueOf(transactionPoid));

            query.execute();

            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
            if (rs == null || !rs.next()) {
                return null;
            }

            LineProfileAgreementDetailsResponse response = new LineProfileAgreementDetailsResponse();
            response.setAgreementPoid(toLong(rs.getObject("TRANSACTION_POID")));
            response.setAgreementId(rs.getString("AGREEMENT_ID"));
            response.setAgreementType(rs.getString("AGREEMENT_TYPE"));
            response.setAgreementStatus(rs.getString("AGREEMENT_STATUS"));
            response.setEffectiveDate(toLocalDate(rs.getObject("EFFECTIVE_DATE")));
            response.setExpiryDate(toLocalDate(rs.getObject("EXPIRY_DATE")));
            response.setRenewalCycle(rs.getString("RENEWAL_CYCLE"));
            return response;
        } catch (Exception e) {
            throw new ValidationException("Error executing PROC_SH_CONTRACTS_AGREEMENT: " + e.getMessage());
        }
    }

    private void upsertContactDetails(Long lineProfilePoid, List<LineProfileContactDto> detailDtos, String userId, String docId) {
        String docKeyPoid = lineProfilePoid.toString();
        List<ShipLineProfileContactDtlEntity> existing =
                contactRepository.findByLineProfilePoidOrderByDetRowId(lineProfilePoid);

        if (detailDtos == null || detailDtos.isEmpty()) {
            return;
        }

        Map<Long, ShipLineProfileContactDtlEntity> existingByDetRow = existing.stream()
                .filter(e -> e.getDetRowId() != null)
                .collect(Collectors.toMap(ShipLineProfileContactDtlEntity::getDetRowId, entity -> entity));

        long maxDetRowId = existing.stream()
                .map(ShipLineProfileContactDtlEntity::getDetRowId)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);

        List<ShipLineProfileContactDtlEntity> deletions = new ArrayList<>();
        List<ShipLineProfileContactDtlEntity> toSave = new ArrayList<>();
        List<ShipLineProfileContactDtlEntity> toUpdate = new ArrayList<>();
        List<LogRequestDto<ShipLineProfileContactDtlEntity>> logRequests = new ArrayList<>();

        for (LineProfileContactDto dto : detailDtos) {
            if (dto == null) continue;
            String action = resolveActionType(dto.getActionType(), dto.getDetRowId());
            switch (action) {
                case ACTION_NO_CHANGE -> {
                    // no-op
                }
                case ACTION_IS_CREATED -> {
                    Long candidateDetRowId = normalizeDetRowId(dto.getDetRowId());
                    long detRowId = candidateDetRowId == null ? ++maxDetRowId : candidateDetRowId;
                    maxDetRowId = Math.max(maxDetRowId, detRowId);
                    ShipLineProfileContactDtlEntity entity =
                            mapper.toNewContactEntity(lineProfilePoid, detRowId, dto, userId);
                    toSave.add(entity);
                }
                case ACTION_IS_UPDATED -> {
                    Long detRowIdToUpdate = normalizeDetRowId(dto.getDetRowId());
                    if (detRowIdToUpdate == null) {
                        throw new ValidationException("detRowId is required for updating Contact Details");
                    }
                    ShipLineProfileContactDtlEntity entity = existingByDetRow.get(detRowIdToUpdate);
                    if (entity == null) {
                        throw new ResourceNotFoundException("LineProfileContact", "detRowId", detRowIdToUpdate);
                    }
                    ShipLineProfileContactDtlEntity oldItem = new ShipLineProfileContactDtlEntity();
                    BeanUtils.copyProperties(entity, oldItem);
                    mapper.applyUpdateContactEntity(entity, dto, userId);
                    toUpdate.add(entity);
                    String logDetailForUpdate = String.format("KeyId = LINE_PROFILE_POID: %s DET_ROW_ID: %s", lineProfilePoid, detRowIdToUpdate);
                    logRequests.add(new LogRequestDto<>(oldItem, entity, ShipLineProfileContactDtlEntity.class, docId, docKeyPoid, logDetailForUpdate));
                }
                case ACTION_IS_DELETED -> {
                    Long detRowIdToDelete = normalizeDetRowId(dto.getDetRowId());
                    if (detRowIdToDelete == null) {
                        throw new ValidationException("detRowId is required for deleting Contact Details");
                    }
                    ShipLineProfileContactDtlEntity entityToDelete = existingByDetRow.get(detRowIdToDelete);
                    if (entityToDelete == null) {
                        throw new ResourceNotFoundException("LineProfileContact", "detRowId", detRowIdToDelete);
                    }
                    deletions.add(entityToDelete);
                }
            }
        }

        if (!deletions.isEmpty()) {
            contactRepository.deleteAll(deletions);
            deletions.forEach(deleted -> loggingService.logDelete(deleted, docId, docKeyPoid));
        }
        if (!toSave.isEmpty()) {
            List<ShipLineProfileContactDtlEntity> savedItems = contactRepository.saveAll(toSave);
            savedItems.forEach(item -> {
                String logDetail = String.format("Row Created on Contact Details with detRowId: %s", item.getDetRowId());
                loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
            });
        }
        if (!toUpdate.isEmpty()) {
            contactRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
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

    private void enrich(LineProfileResponse response, Long groupPoid) {
        if (response == null) return;

        if (response.getRegionPoids() != null) {
            Map<Long, LovGetListDto> regionByPoid = fetchRegionDetails(response.getRegionPoids());
            if (response.getRegionDet() != null) {
                List<LovGetListDto> regionDet = new ArrayList<>();
                for (Long poid : response.getRegionPoids()) {
                    LovGetListDto dto = regionByPoid.get(poid);
                    if (dto != null) {
                        regionDet.add(dto);
                    }
                }
                response.setRegionDet(regionDet);
            }
        }
        Long linePoid = response.getLinePoid();
        if (linePoid != null) {
            LineProfileLineDetailsResponse lineDetails =
                    fetchLineDetails(linePoid, groupPoid, UserContext.getCompanyPoid(), UserContext.getUserPoid());
            response.setLineDetails(lineDetails);
        }

        if (response.getAgreementPoid() != null) {
            LineProfileAgreementDetailsResponse agreementDetails =
                    fetchAgreementDetails(response.getAgreementPoid(), groupPoid, UserContext.getCompanyPoid(), UserContext.getUserPoid());
            response.setAgreementDetails(agreementDetails);
        }
    }

    private Map<Long, LovGetListDto> fetchRegionDetails(List<Long> regionPoids) {
        String sql = """
                SELECT REGION_POID AS POID,
                       REGION_CODE AS CODE,
                       REGION_NAME AS DESCRIPTION
                  FROM SHIP_REGION_MASTER
                 WHERE REGION_POID IN (:poids)
                   AND NVL(ACTIVE,'Y') = 'Y'
                """;
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("poids", regionPoids);
        return toLovMap(q);
    }


    private Map<Long, LovGetListDto> toLovMap(Query q) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        Map<Long, LovGetListDto> out = new HashMap<>();
        for (Object[] row : rows) {
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
        return new LovGetListDto(poid, code, description, poid, description, null, null);
    }

    private static LovGetListDto resolveAgencyTypeLov(String code) {
        if (StringUtils.isBlank(code)) return null;
        String normalized = code.trim().toUpperCase();
        if ("MLO".equals(normalized)) {
            return new LovGetListDto(1L, "MLO", "Main Line Operator", 1L, "Main Line Operator", null, null);
        }
        if ("NVOC".equals(normalized)) {
            return new LovGetListDto(2L, "NVOC", "Non Vessel Operator", 2L, "Non Vessel Operator", null, null);
        }
        return null;
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

    private static LocalDate toLocalDate(Object v) {
        if (v == null) return null;
        if (v instanceof java.sql.Date d) return d.toLocalDate();
        if (v instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        if (v instanceof LocalDate ld) return ld;
        try {
            return LocalDate.parse(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

}

