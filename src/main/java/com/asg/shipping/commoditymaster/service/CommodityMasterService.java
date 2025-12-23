package com.asg.shipping.commoditymaster.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.commoditymaster.dto.request.CommodityMasterRequest;
import com.asg.shipping.commoditymaster.dto.response.CommodityMasterResponse;
import com.asg.shipping.commoditymaster.entity.CommodityMaster;
import com.asg.shipping.commoditymaster.mapper.CommodityMapper;
import com.asg.shipping.commoditymaster.repository.CommodityMasterRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
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
public class CommodityMasterService {

    private final CommodityMasterRepository commodityMasterRepository;
    private final DocumentSearchService documentSearchService;
    private final CommodityMapper mapper;
    private final EntityManager entityManager;

    public Map<String, Object> listCommodities(String docId, FilterRequestDto request, Pageable pageable) {
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
        CommodityMaster commodity = commodityMasterRepository.findActiveByCommodityPoid(commodityPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Commodity", "commodityPoid", commodityPoid.toString()));

        return mapper.mapToDto(commodity);
    }

    @Transactional
    public CommodityMasterResponse createCommodity(CommodityMasterRequest request) {
        CommodityMaster commodity = new CommodityMaster();
        mapper.mapCreateDTOToEntity(request, commodity, UserContext.getGroupPoid(), UserContext.getUserName());
        CommodityMaster saved = commodityMasterRepository.save(commodity);
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
        CommodityMaster existing = commodityMasterRepository.findByCommodityPoid(commodityPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Commodity", "commodityPoid", commodityPoid.toString()));

        mapper.mapUpdateDTOToEntity(request, existing, UserContext.getUserName());
        CommodityMaster saved = commodityMasterRepository.save(existing);
        return mapper.mapToDto(saved);
    }

    @Transactional
    public void softDeleteCommodity(Long commodityPoid) {
        CommodityMaster commodity = commodityMasterRepository.findByCommodityPoid(commodityPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Commodity", "commodityPoid", commodityPoid.toString()));

        commodity.setDeleted("Y");
        commodity.setActive("N");
        commodity.setLastmodifiedBy(UserContext.getUserName());
        commodity.setLastmodifiedDate(Timestamp.from(Instant.now()));
        
        commodityMasterRepository.save(commodity);
    }
}