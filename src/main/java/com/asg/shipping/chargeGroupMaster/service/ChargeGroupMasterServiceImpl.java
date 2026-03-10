package com.asg.shipping.chargeGroupMaster.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.chargeGroupMaster.dto.ChargeGroupMasterRequestDto;
import com.asg.shipping.chargeGroupMaster.dto.ChargeGroupMasterResponseDto;
import com.asg.shipping.chargeGroupMaster.entity.ShipChargeGroupMaster;
import com.asg.shipping.chargeGroupMaster.repository.ShipChargeGroupMasterRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ChargeGroupMasterServiceImpl implements ChargeGroupMasterService{

    private final ShipChargeGroupMasterRepository repository;
    private final LoggingService loggingService;
    private final DocumentSearchService documentService;

    @Override
    public ChargeGroupMasterResponseDto create(ChargeGroupMasterRequestDto request) {

        repository.findByChargeGroupCode(request.getChargeGroupCode())
                .ifPresent(e -> {
                    throw new IllegalArgumentException("Charge Group Code already exists");
                });
        repository.findByChargeGroupName(request.getChargeGroupName())
                .ifPresent(e -> {
                    throw new IllegalArgumentException("Charge Group Name already exists");
                });

        ShipChargeGroupMaster entity = ShipChargeGroupMaster.builder()
                .groupPoid(UserContext.getGroupPoid())
                .chargeGroupCode(request.getChargeGroupCode())
                .chargeGroupName(request.getChargeGroupName().trim().toUpperCase())
                .chargeGroupName2(request.getChargeGroupName2())
                .chargeGlPayable(request.getChargeGlPayable())
                .chargeGlSale(request.getChargeGlSale())
                .chargeGlCostSale(request.getChargeGlCostSale())
                .linewisePayablePosting(request.getLinewisePayablePosting())
               // .glPrefix(request.getGlPrefix())
                .active(request.getActive())
                .deleted("N")
                .seqNo(request.getSeqNo())
                .build();

        ShipChargeGroupMaster saveChargeGroup =repository.save(entity);

        String docId = UserContext.getDocumentId();
        String key = saveChargeGroup.getChargeGroupPoid().toString();

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);
        loggingService.logChanges(new ShipChargeGroupMaster(), saveChargeGroup, ShipChargeGroupMaster.class, docId, key, LogDetailsEnum.CREATED, "CHARGE_GROUP_POID");
        return mapToResponse(entity);
    }

    @Override
    public ChargeGroupMasterResponseDto update(Long poid, ChargeGroupMasterRequestDto request) {

        ShipChargeGroupMaster entity = repository.findById(poid)
                .orElseThrow(() -> new ResourceNotFoundException("Charge Group not found","ChargeGroupPoid",poid));

        ShipChargeGroupMaster oldChargeMaster = new ShipChargeGroupMaster();
        BeanUtils.copyProperties(entity, oldChargeMaster);

        entity.setChargeGroupName(request.getChargeGroupName());
        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setChargeGroupName2(request.getChargeGroupName2());
        entity.setChargeGlPayable(request.getChargeGlPayable());
        entity.setChargeGlSale(request.getChargeGlSale());
        entity.setChargeGlCostSale(request.getChargeGlCostSale());
       // entity.setGlPrefix(request.getGlPrefix());
        entity.setLinewisePayablePosting(request.getLinewisePayablePosting());
        entity.setActive(request.getActive());
        entity.setSeqNo(request.getSeqNo());

        ShipChargeGroupMaster updatedEntity = repository.save(entity);

        String docId = UserContext.getDocumentId();
        String key = updatedEntity.getChargeGroupPoid().toString();

        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, docId, key);
        loggingService.logChanges(oldChargeMaster, updatedEntity, ShipChargeGroupMaster.class, docId, key, LogDetailsEnum.MODIFIED, "CHARGE_GROUP_POID");

        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public ChargeGroupMasterResponseDto findById(Long poid) {
        return repository.findById(poid)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Charge Group not found","ChargeGroupPoid",poid));
    }


    @Override
    public void delete(Long poid) {
        ShipChargeGroupMaster entity = repository.findById(poid)
                .orElseThrow(() -> new ResourceNotFoundException("Charge Group not found","ChargeGroupPoid",poid));
        entity.setDeleted("Y");
        entity.setActive("N");
        String docId = UserContext.getDocumentId();
        String key = poid.toString();

        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, docId, key);

        loggingService.logSimpleFieldChange(ShipChargeGroupMaster.class, docId, key, "deleted", "N", "Y", "ChargeGroupMaster soft deleted");
        loggingService.logSimpleFieldChange(ShipChargeGroupMaster.class, docId, key, "active", "Y", "N", "ChargeGroupMaster soft deleted");

    }

    @Override
    public Map<String, Object> listChargeGroupMaster(String docId, FilterRequestDto request, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "CHARGE_GROUP_CODE",   // label
                "CHARGE_GROUP_POID");  // value

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private ChargeGroupMasterResponseDto mapToResponse(ShipChargeGroupMaster e) {
        return ChargeGroupMasterResponseDto.builder()
                .chargeGroupPoid(e.getChargeGroupPoid())
                .groupPoid(e.getGroupPoid())
                .chargeGroupCode(e.getChargeGroupCode())
                .chargeGroupName(e.getChargeGroupName())
                .chargeGroupName2(e.getChargeGroupName2())
                .chargeGlPayable(e.getChargeGlPayable())
                .chargeGlSale(e.getChargeGlSale())
                .chargeGlCostSale(e.getChargeGlCostSale())
                .glPrefix(e.getGlPrefix())
                .linewisePayablePosting(e.getLinewisePayablePosting())
                .active(e.getActive())
                .seqNo(e.getSeqNo())
                .createdBy(e.getCreatedBy())
                .createdDate(e.getCreatedDate())
                .lastModifiedBy(e.getLastModifiedBy())
                .lastModifiedDate(e.getLastModifiedDate())
                .build();
    }

    public static String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }
}
