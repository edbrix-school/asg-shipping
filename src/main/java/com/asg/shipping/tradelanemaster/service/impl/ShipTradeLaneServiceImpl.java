package com.asg.shipping.tradelanemaster.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.tradelanemaster.dto.request.ShipTradelaneRequest;
import com.asg.shipping.tradelanemaster.dto.response.ShipTradelaneResponse;
import com.asg.shipping.tradelanemaster.entity.ShipTradelaneMaster;
import com.asg.shipping.tradelanemaster.repository.ShipTradeLaneMasterRepository;
import com.asg.shipping.tradelanemaster.service.ShipTradeLaneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
    private final DocumentDeleteService deleteService;
    private final LoggingService loggingService;
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

        entity = shipTradelaneMasterRepository.save(entity);
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED,UserContext.getDocumentId(),entity.getTradeLanePoid().toString());
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public ShipTradelaneResponse update(Long tradeLanePoid, ShipTradelaneRequest request) {

        if (tradeLanePoid == null) {
            throw new ValidationException("Trade Lane POID is required");
        }


        ShipTradelaneMaster existingEntity = findEntityById(tradeLanePoid);

        ShipTradelaneMaster oldEntity =  new ShipTradelaneMaster();
        BeanUtils.copyProperties(existingEntity, oldEntity);


        validateUpdateRequest(request, existingEntity);

        existingEntity.setTradeLaneCode(request.getTradeLaneCode());
        existingEntity.setTradeLaneName(request.getTradeLaneName());
        existingEntity.setTradeLaneName2(request.getTradeLaneName2());
        existingEntity.setRegionPoid(request.getRegionPoid());
        existingEntity.setActive(Boolean.TRUE.equals(request.getActive()) ? FLAG_YES : FLAG_NO);
        existingEntity.setSeqNo(request.getSeqNo().longValue());

        existingEntity = shipTradelaneMasterRepository.save(existingEntity);
        loggingService.logChanges(oldEntity,existingEntity, ShipTradelaneMaster.class,UserContext.getDocumentId(),tradeLanePoid.toString(),LogDetailsEnum.MODIFIED,"TRADELANE_POID");

        return mapToResponse(existingEntity);
    }

    @Override
    public ShipTradelaneResponse getById(Long tradeLanePoid) {
        ShipTradelaneMaster entity = findEntityById(tradeLanePoid);
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public void delete(Long tradeLanePoid, DeleteReasonDto deleteReasonDto) {
        ShipTradelaneMaster entity = findEntityById(tradeLanePoid);

        deleteService.deleteDocument(tradeLanePoid,"SHIP_TRADELANE_MASTER",
                "TRADELANE_POID",deleteReasonDto,null);
    }

    @Override
    public Map<String, Object> list(FilterRequestDto filters, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveFilters(filters);

        RawSearchResult raw = documentService.search(
                UserContext.getDocumentId(),
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
