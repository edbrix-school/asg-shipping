package com.asg.shipping.commoditymaster.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.commoditymaster.dto.request.CommodityMasterRequest;
import com.asg.shipping.commoditymaster.dto.response.CommodityMasterResponse;
import com.asg.shipping.commoditymaster.entity.CommodityMaster;
import com.asg.shipping.commoditymaster.mapper.CommodityMapper;
import com.asg.shipping.commoditymaster.repository.CommodityMasterRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommodityMasterService {

    private final CommodityMasterRepository commodityMasterRepository;
    private final DocumentSearchService documentSearchService;
    private final CommodityMapper mapper;
    private final LoggingService loggingService;
    private final EntityManager entityManager;

    public Map<String, Object> listCommodities(String docId, FilterRequestDto request, Pageable pageable) {
        log.info("Listing commodities with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(), pageable.getPageSize());
        String operator = documentSearchService.resolveOperator(request);
        String isDeleted = documentSearchService.resolveIsDeleted(request);
        List<FilterDto> filters = documentSearchService.resolveFilters(request);

        RawSearchResult raw = documentSearchService.search(docId, filters, operator, pageable, isDeleted,
                "COMODITY_NAME",
                "COMODITY_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    public CommodityMasterResponse getCommodityById(Long commodityPoid) {
        log.info("Getting commodity with id: {}", commodityPoid);
        CommodityMaster commodity = commodityMasterRepository.findActiveByCommodityPoid(commodityPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Commodity", "commodityPoid", commodityPoid.toString()));

        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), commodityPoid.toString());

        log.info("Successfully retrieved commodity with id: {}", commodityPoid);
        return mapper.mapToDto(commodity);
    }

    @Transactional
    public CommodityMasterResponse createCommodity(CommodityMasterRequest request) {
        log.info("Creating commodity with name: {}", request.getCommodityName());
        CommodityMaster commodity = new CommodityMaster();
        mapper.mapCreateDTOToEntity(request, commodity, UserContext.getGroupPoid(), UserContext.getUserName());
        CommodityMaster saved = commodityMasterRepository.save(commodity);

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), saved.getCommodityPoid().toString());

        log.info("Successfully created commodity with id: {}", saved.getCommodityPoid());
        return mapper.mapToDto(saved);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public CommodityMasterResponse getRefreshedCommodity(Long commodityPoid) {
        CommodityMaster refreshed = commodityMasterRepository.findById(commodityPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Commodity", "commodityPoid", commodityPoid.toString()));
        return mapper.mapToDto(refreshed);
    }

    @Transactional
    public CommodityMasterResponse updateCommodity(Long commodityPoid, CommodityMasterRequest request) {
        log.info("Updating commodity with id: {}", commodityPoid);
        CommodityMaster existing = commodityMasterRepository.findByCommodityPoid(commodityPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Commodity", "commodityPoid", commodityPoid.toString()));

        CommodityMaster oldCommodity = new CommodityMaster();
        BeanUtils.copyProperties(existing, oldCommodity);

        mapper.mapUpdateDTOToEntity(request, existing, UserContext.getUserName());
        CommodityMaster saved = commodityMasterRepository.save(existing);

        loggingService.logChanges(oldCommodity, saved, CommodityMaster.class, UserContext.getDocumentId(), commodityPoid.toString(), LogDetailsEnum.MODIFIED, "COMODITY_POID");

        log.info("Successfully updated commodity with id: {}", commodityPoid);
        return mapper.mapToDto(saved);
    }

    @Transactional
    public void softDeleteCommodity(Long commodityPoid) {
        log.info("Deleting commodity with id: {}", commodityPoid);

        CommodityMaster commodity = commodityMasterRepository.findByCommodityPoid(commodityPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Commodity", "commodityPoid", commodityPoid.toString()));

        if ("Y".equals(commodity.getDeleted())) {
            log.info("Commodity with id: {} is already deleted", commodityPoid);
            return;
        }

        commodity.setDeleted("Y");
        commodity.setActive("N");
        commodity.setLastmodifiedBy(UserContext.getUserName());
        commodity.setLastmodifiedDate(Timestamp.from(Instant.now()));
        
        commodityMasterRepository.save(commodity);

        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, UserContext.getDocumentId(), commodityPoid.toString());
        String logDetail = String.format("KeyId = COMODITY_POID:%s", commodityPoid);
        String tableName = CommodityMaster.class.getAnnotation(jakarta.persistence.Table.class).name();
        loggingService.createLogDetailsEntry(UserContext.getDocumentId(), commodityPoid.toString(), "Deleted", "N", "Y", logDetail, tableName);
        loggingService.createLogDetailsEntry(UserContext.getDocumentId(), commodityPoid.toString(), "Active", "Y", "N", logDetail, tableName);

        log.info("Successfully deleted commodity with id: {}", commodityPoid);
    }
}