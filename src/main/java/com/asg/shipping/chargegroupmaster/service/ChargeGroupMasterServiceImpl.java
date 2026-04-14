package com.asg.shipping.chargegroupmaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.chargegroupmaster.dto.ChargeGroupMasterRequestDto;
import com.asg.shipping.chargegroupmaster.dto.ChargeGroupMasterResponseDto;
import com.asg.shipping.chargegroupmaster.entity.ShipChargeGroupMaster;
import com.asg.shipping.chargegroupmaster.repository.ShipChargeGroupMasterRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ChargeGroupMasterServiceImpl implements ChargeGroupMasterService{

    private final ShipChargeGroupMasterRepository repository;
    private final LoggingService loggingService;
    private final LovDataService lovService;
    private final DocumentDeleteService documentDeleteService;
    private final DocumentSearchService documentService;
    private static final String CHARGE_GROUP_NOT_FOUND = "Charge Group not found";
    private static final String CHARGE_POID = "chargeGroupPoid";
    private static final String CHARGE_GROUP_POID = "CHARGE_GROUP_POID";


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
        return mapToResponse(entity);
    }

    @Override
    public ChargeGroupMasterResponseDto update(Long poid, ChargeGroupMasterRequestDto request) {

        ShipChargeGroupMaster entity = repository.findById(poid)
                .orElseThrow(() -> new ResourceNotFoundException(CHARGE_GROUP_NOT_FOUND,CHARGE_POID,poid));

        //  UNIQUE NAME VALIDATION
        if (repository.existsByChargeGroupNameAndChargeGroupPoidNot(
                request.getChargeGroupName(), poid)) {
            throw new RuntimeException("Charge Group Name already exists");
        }

        //  UNIQUE CODE VALIDATION (recommended)
        if (repository.existsByChargeGroupCodeAndChargeGroupPoidNot(
                request.getChargeGroupCode(), poid)) {
            throw new RuntimeException("Charge Group Code already exists");
        }


        ShipChargeGroupMaster oldChargeMaster = new ShipChargeGroupMaster();
        BeanUtils.copyProperties(entity, oldChargeMaster);
        entity.setChargeGroupCode(request.getChargeGroupCode());
        entity.setChargeGroupName(request.getChargeGroupName());
        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setChargeGroupName2(request.getChargeGroupName2());
        entity.setChargeGlPayable(request.getChargeGlPayable());
        entity.setChargeGlSale(request.getChargeGlSale());
        entity.setChargeGlCostSale(request.getChargeGlCostSale());
        entity.setLinewisePayablePosting(request.getLinewisePayablePosting());
        entity.setActive(request.getActive());
        entity.setSeqNo(request.getSeqNo());

        ShipChargeGroupMaster updatedEntity = repository.save(entity);

        String docId = UserContext.getDocumentId();
        String key = updatedEntity.getChargeGroupPoid().toString();

        loggingService.logChanges(oldChargeMaster, updatedEntity, ShipChargeGroupMaster.class, docId, key, LogDetailsEnum.MODIFIED, CHARGE_GROUP_POID);

        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public ChargeGroupMasterResponseDto findById(Long poid) {
        return repository.findById(poid)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(CHARGE_GROUP_NOT_FOUND,CHARGE_POID,poid));
    }


    @Override
    public void delete(Long poid, DeleteReasonDto deleteReasonDto) {
      repository.findById(poid)
                .orElseThrow(() -> new ResourceNotFoundException(CHARGE_GROUP_NOT_FOUND,CHARGE_POID,poid));

        documentDeleteService.deleteDocument(
                poid,
                "SHIP_CHARGE_GROUP_MASTER",
                "CHARGE_GROUP_POID",
                deleteReasonDto,
                null
        );
    }

    @Override
    public Map<String, Object> listChargeGroupMaster(String docId, FilterRequestDto request, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "CHARGE_GROUP_CODE",   // label
                CHARGE_GROUP_POID);  // value

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
