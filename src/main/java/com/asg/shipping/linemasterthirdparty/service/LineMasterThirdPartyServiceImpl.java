package com.asg.shipping.linemasterthirdparty.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.linemasterthirdparty.dto.*;
import com.asg.shipping.linemasterthirdparty.entity.ShipLineMaster;
import com.asg.shipping.linemasterthirdparty.repository.ShipLineMasterThirdPartyRepository;
import com.asg.shipping.linemasterthirdparty.util.LineMasterThirdPartyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;
import static com.asg.common.lib.security.util.UserContext.getGroupPoid;
import static com.asg.common.lib.security.util.UserContext.getUserPoid;
import static com.asg.common.lib.security.util.UserContext.getCompanyPoid;

/**
 * Service implementation for Line Master Third Party operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LineMasterThirdPartyServiceImpl implements LineMasterThirdPartyService {

    private static final String LINE_TYPE_THIRD_PARTY = "THIRD_PARTY";

    private final ShipLineMasterThirdPartyRepository lineRepository;
    private final LovDataService lovService;
    private final LineMasterThirdPartyMapper mapper;
    private final LoggingService loggingService;
    private final DocumentSearchService documentSearchService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchThirdPartyLines(com.asg.common.lib.dto.FilterRequestDto request, Pageable pageable) {
        log.info("Searching third party lines with page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentSearchService.resolveOperator(request);
        String isDeleted = documentSearchService.resolveIsDeleted(request);
        List<FilterDto> filters = documentSearchService.resolveFilters(request);

        // Add mandatory LINE_TYPE filter to ensure only THIRD_PARTY records are returned
        filters.add(new FilterDto("LINE_TYPE", LINE_TYPE_THIRD_PARTY));

        RawSearchResult raw = documentSearchService.search("100-013", filters, operator, pageable, isDeleted,
                "LINE_NAME",
                "LINE_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public LineMasterThirdPartyDto getThirdPartyLine(Long id) {
        log.info("Getting third party line with id: {}", id);

        Long groupPoid = getGroupPoid();
        ShipLineMaster line = lineRepository.findByLinePoidAndGroupPoidAndThirdParty(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Third Party Line", "linePoid", id.toString()));

        // Additional validation: ensure LINE_TYPE is THIRD_PARTY
        if (!LINE_TYPE_THIRD_PARTY.equals(line.getLineType())) {
            throw new ResourceNotFoundException("Third Party Line", "linePoid", id.toString());
        }

        LineMasterThirdPartyDto dto = mapper.mapToDto(line);

        // Enrich with LOV data
        enrichDtoWithLovData(dto, line, groupPoid);

        // Log view
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());

        log.info("Successfully retrieved third party line with id: {}", id);
        return dto;
    }

    @Override
    @Transactional
    public LineMasterThirdPartyDto createThirdPartyLine(LineMasterThirdPartyCreateDTO dto) {
        log.info("Creating third party line with code: {}, name: {}", dto.getLineCode(), dto.getLineName());

        Long groupPoid = getGroupPoid();
        Long userPoid = getUserPoid();
        Long companyPoid = getCompanyPoid();

        // Validate
        validateLineCreateDTO(dto, groupPoid);

        // Create main entity
        ShipLineMaster line = new ShipLineMaster();
        mapper.mapCreateDTOToEntity(dto, line, groupPoid, userPoid, companyPoid);

        // Ensure LINE_TYPE is set to THIRD_PARTY (mandatory)
        line.setLineType(LINE_TYPE_THIRD_PARTY);

        // Save main entity
        ShipLineMaster saved = lineRepository.save(line);

        // Log creation
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), saved.getLinePoid().toString());

        // Fetch and return with LOV data
        LineMasterThirdPartyDto result = mapper.mapToDto(saved);
        enrichDtoWithLovData(result, saved, groupPoid);

        log.info("Successfully created third party line with id: {}", saved.getLinePoid());
        return result;
    }

    @Override
    @Transactional
    public LineMasterThirdPartyDto updateThirdPartyLine(Long id, LineMasterThirdPartyUpdateDTO dto) {
        log.info("Updating third party line with id: {}", id);

        Long groupPoid = getGroupPoid();
        Long userPoid = getUserPoid();
        Long companyPoid = getCompanyPoid();

        // Find existing line (with LINE_TYPE = 'THIRD_PARTY' filter)
        ShipLineMaster line = lineRepository.findByLinePoidAndGroupPoidAndThirdParty(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Third Party Line", "linePoid", id.toString()));

        // Additional validation: ensure LINE_TYPE is THIRD_PARTY
        if (!LINE_TYPE_THIRD_PARTY.equals(line.getLineType())) {
            throw new ResourceNotFoundException("Third Party Line", "linePoid", id.toString());
        }

        // Validate
        validateLineUpdateDTO(dto, groupPoid, id);

        // Store old values for logging
        ShipLineMaster oldLine = ShipLineMaster.builder()
                .lineName(line.getLineName())
                .lineName2(line.getLineName2())
                .lineAddress(line.getLineAddress())
                .countryPoid(line.getCountryPoid())
                .currencyPoid(line.getCurrencyPoid())
                .billTo(line.getBillTo())
                .active(line.getActive())
                .seqno(line.getSeqno())
                .build();

        // Update main entity
        mapper.mapUpdateDTOToEntity(dto, line, groupPoid, userPoid, companyPoid);

        // Ensure LINE_TYPE remains THIRD_PARTY (cannot be changed)
        line.setLineType(LINE_TYPE_THIRD_PARTY);

        ShipLineMaster saved = lineRepository.save(line);

        // Log changes
        loggingService.logChanges(oldLine, saved, ShipLineMaster.class, UserContext.getDocumentId(), id.toString(), LogDetailsEnum.MODIFIED, "LINE_POID");

        // Fetch and return with LOV data
        LineMasterThirdPartyDto result = mapper.mapToDto(saved);
        enrichDtoWithLovData(result, saved, groupPoid);

        log.info("Successfully updated third party line with id: {}", id);
        return result;
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        log.info("Toggling active status for third party line with id: {}", id);

        Long groupPoid = getGroupPoid();
        ShipLineMaster line = lineRepository.findByLinePoidAndGroupPoidAndThirdParty(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Third Party Line", "linePoid", id.toString()));

        // Additional validation: ensure LINE_TYPE is THIRD_PARTY
        if (!LINE_TYPE_THIRD_PARTY.equals(line.getLineType())) {
            throw new ResourceNotFoundException("Third Party Line", "linePoid", id.toString());
        }

        final String currentActive = line.getActive();
        line.setActive("Y".equals(currentActive) ? "N" : "Y");
        line.setLastModifiedBy(getCurrentUser());
        line.setLastModifiedDate(LocalDateTime.now());

        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, UserContext.getDocumentId(), id.toString());
        String logDetail = String.format("KeyId = LINE_POID:%s", id);
        String tableName = ShipLineMaster.class.getAnnotation(jakarta.persistence.Table.class).name();
        loggingService.createLogDetailsEntry(UserContext.getDocumentId(), id.toString(), "Active", currentActive, line.getActive(), logDetail, tableName);
        lineRepository.save(line);
        log.info("Successfully toggled active status for third party line with id: {} to {}", id, line.getActive());
    }

    @Override
    @Transactional
    public void deleteThirdPartyLine(Long id) {
        log.info("Deleting third party line with id: {}", id);

        Long groupPoid = getGroupPoid();
        ShipLineMaster line = lineRepository.findByLinePoidAndGroupPoidAndThirdParty(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Third Party Line", "linePoid", id.toString()));

        // Additional validation: ensure LINE_TYPE is THIRD_PARTY
        if (!LINE_TYPE_THIRD_PARTY.equals(line.getLineType())) {
            throw new ResourceNotFoundException("Third Party Line", "linePoid", id.toString());
        }

        if ("Y".equals(line.getDeleted())) {
            log.info("Third party line with id: {} is already deleted", id);
            return;
        }

        line.setDeleted("Y");
        line.setActive("N");
        line.setLastModifiedBy(getCurrentUser());
        line.setLastModifiedDate(LocalDateTime.now());

        lineRepository.save(line);

        // Log deletion
        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, UserContext.getDocumentId(), id.toString());
        String logDetail = String.format("KeyId = LINE_POID:%s", id);
        String tableName = ShipLineMaster.class.getAnnotation(jakarta.persistence.Table.class).name();
        loggingService.createLogDetailsEntry(UserContext.getDocumentId(), id.toString(), "Deleted", "N", "Y", logDetail, tableName);
        loggingService.createLogDetailsEntry(UserContext.getDocumentId(), id.toString(), "Active", "Y", "N", logDetail, tableName);

        log.info("Successfully deleted third party line with id: {}", id);
    }

    /**
     * Enrich DTO with LOV data
     */
    private void enrichDtoWithLovData(LineMasterThirdPartyDto dto, ShipLineMaster line, Long groupPoid) {
        Long companyPoid = getCompanyPoid();
        Long userPoid = getUserPoid();

        try {
            if (line.getCountryPoid() != null) {
                dto.setCountryDet(lovService.getDetailsByPoidAndLovName(line.getCountryPoid(), "COUNTRY"));
            }
            if (line.getCurrencyPoid() != null) {
                dto.setCurrencyDet(lovService.getDetailsByPoidAndLovName(line.getCurrencyPoid(), "CURRENCY"));
            }
            if (line.getBillTo() != null) {
                dto.setBillToDet(lovService.getLovItemByCodeFast(line.getBillTo(), "CUSTOMER_MASTER"));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch some LOV data", e);
        }
    }

    /**
     * Validate LineCreateDTO
     */
    private void validateLineCreateDTO(LineMasterThirdPartyCreateDTO dto, Long groupPoid) {
        if (lineRepository.existsByLineCodeAndGroupPoidAndThirdParty(dto.getLineCode(), groupPoid)) {
            throw new ValidationException("Line code already exists for this group");
        }
        if (lineRepository.existsByLineNameAndGroupPoidAndThirdParty(dto.getLineName(), groupPoid)) {
            throw new ValidationException("Line name already exists for this group");
        }

        validateCountry(dto.getCountryPoid());
        validateCurrency(dto.getCurrencyPoid());
        validateBillTo(dto.getBillTo());
    }

    /**
     * Validate LineUpdateDTO
     */
    private void validateLineUpdateDTO(LineMasterThirdPartyUpdateDTO dto, Long groupPoid, Long excludeLinePoid) {
        if (lineRepository.existsByLineNameAndGroupPoidAndThirdPartyExcluding(dto.getLineName(), groupPoid, excludeLinePoid)) {
            throw new ValidationException("Line name already exists for this group");
        }

        validateCountry(dto.getCountryPoid());
        validateCurrency(dto.getCurrencyPoid());
        validateBillTo(dto.getBillTo());
    }


    private void validateCountry(Long countryPoid) {
        if (countryPoid != null) {
            LovGetListDto lovGetListDto = lovService.getDetailsByPoidAndLovName(countryPoid, "COUNTRY");
            if (lovGetListDto == null || lovGetListDto.getPoid() == null)
                throw new ValidationException("Country is not active");
        }
    }

    private void validateCurrency(Long currencyPoid) {
        if (currencyPoid != null) {
            LovGetListDto lovGetListDto = lovService.getDetailsByPoidAndLovName(currencyPoid, "CURRENCY");
            if (lovGetListDto == null || lovGetListDto.getPoid() == null)
                throw new ValidationException("Currency is not active");
        }
    }

    private void validateBillTo(String billToCode) {
        if (billToCode != null) {
            LovGetListDto lovGetListDto = lovService.getLovItemByCodeFast(billToCode, "CUSTOMER_MASTER");
            if (lovGetListDto == null || lovGetListDto.getPoid() == null)
                throw new ValidationException("Bill to is not active");
        }
    }
}
