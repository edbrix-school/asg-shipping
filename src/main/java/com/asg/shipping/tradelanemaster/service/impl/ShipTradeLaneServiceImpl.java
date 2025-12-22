package com.asg.shipping.tradelanemaster.service.impl;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.tradelanemaster.dto.request.ShipTradelaneRequest;
import com.asg.shipping.tradelanemaster.dto.response.ShipTradelaneResponse;
import com.asg.shipping.tradelanemaster.entity.ShipTradelaneMaster;
import com.asg.shipping.tradelanemaster.repository.ShipTradeLaneMasterRepository;
import com.asg.shipping.tradelanemaster.service.ShipTradeLaneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;
import static com.asg.common.lib.utility.ASGHelperUtils.getGroupId;


@Service
@Slf4j
@RequiredArgsConstructor
public class ShipTradeLaneServiceImpl implements ShipTradeLaneService {

    private final ShipTradeLaneMasterRepository shipTradelaneMasterRepository;
    private final DocumentSearchService documentService;

    private static final String FLAG_YES = "Y";
    private static final String FLAG_NO = "N";

    @Override
    @Transactional
    public ShipTradelaneResponse create(ShipTradelaneRequest request) {

        validateRequest(request);

        ShipTradelaneMaster entity = new ShipTradelaneMaster();
        entity.setGroupPoid(getGroupId());
        entity.setTradeLaneCode(request.getTradeLaneCode());
        entity.setTradeLaneName(request.getTradeLaneName());
        entity.setTradeLaneName2(request.getTradeLaneName2());
        entity.setRegionPoid(request.getRegionPoid());
        entity.setActive(Boolean.TRUE.equals(request.getActive()) ? FLAG_YES : FLAG_NO);
        entity.setSeqNo(request.getSeqNo().longValue());
        entity.setDeleted(FLAG_NO);
        entity.setCreatedBy(getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());

        entity = shipTradelaneMasterRepository.save(entity);
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public ShipTradelaneResponse update(Long tradeLanePoid, ShipTradelaneRequest request) {

        if (tradeLanePoid == null) {
            throw new ValidationException("Trade Lane POID is required");
        }


        ShipTradelaneMaster entity = findEntityById(tradeLanePoid);

        if (FLAG_YES.equals(entity.getDeleted())) {
            throw new IllegalStateException("Cannot update deleted record");
        }

        validateUpdateRequest(request, entity);

        entity.setTradeLaneCode(request.getTradeLaneCode());
        entity.setTradeLaneName(request.getTradeLaneName());
        entity.setTradeLaneName2(request.getTradeLaneName2());
        entity.setRegionPoid(request.getRegionPoid());
        entity.setActive(Boolean.TRUE.equals(request.getActive()) ? FLAG_YES : FLAG_NO);
        entity.setSeqNo(request.getSeqNo().longValue());
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());

        entity = shipTradelaneMasterRepository.save(entity);
        return mapToResponse(entity);
    }

    @Override
    public ShipTradelaneResponse getById(Long tradeLanePoid) {
        ShipTradelaneMaster entity = findEntityById(tradeLanePoid);
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public void delete(Long tradeLanePoid) {
        ShipTradelaneMaster entity = findEntityById(tradeLanePoid);

        entity.setDeleted(FLAG_YES);
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());
        shipTradelaneMasterRepository.save(entity);
    }

    @Override
    public Map<String, Object> list(String documentId, FilterRequestDto filters, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveFilters(filters);

        RawSearchResult raw = documentService.search(
                documentId,
                filterList,
                operator,
                pageable,
                isDeleted,
                "TRADELANE_NAME",
                "TRADELANE_POID"
        );

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private ShipTradelaneMaster findEntityById(Long tradeLanePoid) {
        return shipTradelaneMasterRepository.findById(tradeLanePoid)
                .orElseThrow(() -> new ResourceNotFoundException("Ship Trade Lane", "tradeLanePoid", tradeLanePoid));
    }


    private ShipTradelaneResponse mapToResponse(ShipTradelaneMaster entity) {
        return ShipTradelaneResponse.builder()
                .tradeLanePoid(entity.getTradeLanePoid())
                .tradeLaneCode(entity.getTradeLaneCode())
                .tradeLaneName(entity.getTradeLaneName())
                .tradeLaneName2(entity.getTradeLaneName2())
                .regionPoid(entity.getRegionPoid())
                .active(FLAG_YES.equals(entity.getActive()))
                .seqNo(entity.getSeqNo() != null ? entity.getSeqNo().intValue() : null)
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    private void validateRequest(ShipTradelaneRequest request) {

        if (request.getTradeLaneCode() == null || request.getTradeLaneCode().trim().isEmpty()) {
            log.error("Trade Lane Code is required");
            throw new ValidationException("Trade Lane Code is required");
        }

        if (request.getTradeLaneName() == null || request.getTradeLaneName().trim().isEmpty()) {
            log.error("Trade Lane Name is required");
            throw new ValidationException("Trade Lane Name is required");
        }

        if (request.getRegionPoid() == null || request.getRegionPoid() <= 0) {
            log.error("Region POID is required");
            throw new ValidationException("Region POID is required");
        }

        if (shipTradelaneMasterRepository.existsByTradeLaneCode(request.getTradeLaneCode())) {
            log.error("Trade Lane Code already exists: {}", request.getTradeLaneCode());
            throw new DuplicateKeyException(
                    "Trade Lane Code already exists: " + request.getTradeLaneCode()
            );
        }

        if (shipTradelaneMasterRepository.existsByTradeLaneName(request.getTradeLaneName())) {
            log.error("Trade Lane Name already exists: {}", request.getTradeLaneName());
            throw new DuplicateKeyException(
                    "Trade Lane Name already exists: " + request.getTradeLaneName()
            );
        }
    }



    private void validateUpdateRequest(ShipTradelaneRequest request, ShipTradelaneMaster existingEntity) {

        if (request.getTradeLaneCode() == null || request.getTradeLaneCode().trim().isEmpty()) {
            throw new ValidationException("Trade Lane Code is required");
        }

        if (request.getTradeLaneName() == null || request.getTradeLaneName().trim().isEmpty()) {
            throw new ValidationException("Trade Lane Name is required");
        }

        if (request.getRegionPoid() == null || request.getRegionPoid() <= 0) {
            throw new ValidationException("Region POID is required");
        }

        if (!existingEntity.getTradeLaneCode().equals(request.getTradeLaneCode()) &&
            shipTradelaneMasterRepository.existsByTradeLaneCodeExcluding(request.getTradeLaneCode(), existingEntity.getTradeLanePoid())) {
            throw new DuplicateKeyException("Trade Lane Code already exists: " + request.getTradeLaneCode());
        }

        if (!existingEntity.getTradeLaneName().equals(request.getTradeLaneName()) &&
            shipTradelaneMasterRepository.existsByTradeLaneNameExcluding(request.getTradeLaneName(), existingEntity.getTradeLanePoid())) {
            throw new DuplicateKeyException("Trade Lane Name already exists: " + request.getTradeLaneName());
        }
    }
}
