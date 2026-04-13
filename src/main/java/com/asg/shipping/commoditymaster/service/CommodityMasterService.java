package com.asg.shipping.commoditymaster.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.DocumentDeleteService;
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

import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class CommodityMasterService {

    private final CommodityMasterRepository commodityMasterRepository;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final CommodityMapper mapper;
    private final LoggingService loggingService;
    private final EntityManager entityManager;

    private static final String COMMODITY_NOT_FOUND_MSG = "Commodity";
    private static final String COMMODITY_POID_FIELD = "commodityPoid";
    private static final String COMODITY_NAME_FIELD = "COMODITY_NAME";
    private static final String COMODITY_POID_FIELD = "COMODITY_POID";
    private static final String SHIP_COMODITY_MASTER = "SHIP_COMODITY_MASTER";
    private static final String LISTING_COMMODITIES_MSG = "Listing commodities with docId: {}, page: {}, size: {}";
    private static final String GETTING_COMMODITY_MSG = "Getting commodity with id: {}";
    private static final String CREATING_COMMODITY_MSG = "Creating commodity with name: {}";
    private static final String UPDATING_COMMODITY_MSG = "Updating commodity with id: {}";
    private static final String DELETING_COMMODITY_MSG = "Deleting commodity with id: {}";
    private static final String SUCCESSFULLY_RETRIEVED_MSG = "Successfully retrieved commodity with id: {}";
    private static final String SUCCESSFULLY_CREATED_MSG = "Successfully created commodity with id: {}";
    private static final String SUCCESSFULLY_UPDATED_MSG = "Successfully updated commodity with id: {}";
    private static final String SUCCESSFULLY_DELETED_MSG = "Successfully deleted commodity with id: {}";

    public Map<String, Object> listCommodities(String docId, FilterRequestDto request, Pageable pageable) {
        log.info(LISTING_COMMODITIES_MSG, docId, pageable.getPageNumber(), pageable.getPageSize());
        String operator = documentSearchService.resolveOperator(request);
        String isDeleted = documentSearchService.resolveIsDeleted(request);
        List<FilterDto> filters = documentSearchService.resolveFilters(request);

        RawSearchResult raw = documentSearchService.search(docId, filters, operator, pageable, isDeleted,
                COMODITY_NAME_FIELD,
                COMODITY_POID_FIELD);

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    public CommodityMasterResponse getCommodityById(Long commodityPoid) {
        log.info(GETTING_COMMODITY_MSG, commodityPoid);
        CommodityMaster commodity = commodityMasterRepository.findActiveByCommodityPoid(commodityPoid)
                .orElseThrow(() -> new ResourceNotFoundException(COMMODITY_NOT_FOUND_MSG, COMMODITY_POID_FIELD, commodityPoid.toString()));

        log.info(SUCCESSFULLY_RETRIEVED_MSG, commodityPoid);
        return mapper.mapToDto(commodity);
    }

    @Transactional
    public CommodityMasterResponse createCommodity(CommodityMasterRequest request) {
        log.info(CREATING_COMMODITY_MSG, request.getCommodityName());
        CommodityMaster commodity = new CommodityMaster();
        mapper.mapCreateDTOToEntity(request, commodity, UserContext.getGroupPoid(), UserContext.getUserName());
        CommodityMaster saved = commodityMasterRepository.save(commodity);

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), String.format("%s %s", LogDetailsEnum.CREATED, saved.getCommodityName()));

        log.info(SUCCESSFULLY_CREATED_MSG, saved.getCommodityPoid());
        return mapper.mapToDto(saved);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public CommodityMasterResponse getRefreshedCommodity(Long commodityPoid) {
        CommodityMaster refreshed = commodityMasterRepository.findById(commodityPoid)
                .orElseThrow(() -> new ResourceNotFoundException(COMMODITY_NOT_FOUND_MSG, COMMODITY_POID_FIELD, commodityPoid.toString()));
        return mapper.mapToDto(refreshed);
    }

    @Transactional
    public CommodityMasterResponse updateCommodity(Long commodityPoid, CommodityMasterRequest request) {
        log.info(UPDATING_COMMODITY_MSG, commodityPoid);
        CommodityMaster existing = commodityMasterRepository.findByCommodityPoid(commodityPoid)
                .orElseThrow(() -> new ResourceNotFoundException(COMMODITY_NOT_FOUND_MSG, COMMODITY_POID_FIELD, commodityPoid.toString()));

        CommodityMaster oldCommodity = new CommodityMaster();
        BeanUtils.copyProperties(existing, oldCommodity);

        mapper.mapUpdateDTOToEntity(request, existing, UserContext.getUserName());
        CommodityMaster saved = commodityMasterRepository.save(existing);

        loggingService.logChanges(oldCommodity, saved, CommodityMaster.class, UserContext.getDocumentId(), commodityPoid.toString(), LogDetailsEnum.MODIFIED, COMODITY_POID_FIELD);

        log.info(SUCCESSFULLY_UPDATED_MSG, commodityPoid);
        return mapper.mapToDto(saved);
    }

    @Transactional
    public void softDeleteCommodity(Long commodityPoid, DeleteReasonDto deleteReasonDto) {
        log.info(DELETING_COMMODITY_MSG, commodityPoid);

        CommodityMaster commodity = commodityMasterRepository.findByCommodityPoid(commodityPoid)
                .orElseThrow(() -> new ResourceNotFoundException(COMMODITY_NOT_FOUND_MSG, COMMODITY_POID_FIELD, commodityPoid.toString()));

        documentDeleteService.deleteDocument(
                commodityPoid,
                SHIP_COMODITY_MASTER,
                COMODITY_POID_FIELD,
                deleteReasonDto,
                null
        );

        log.info(SUCCESSFULLY_DELETED_MSG, commodityPoid);
    }
}