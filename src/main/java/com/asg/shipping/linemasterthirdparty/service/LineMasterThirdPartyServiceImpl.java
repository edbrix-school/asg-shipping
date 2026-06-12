package com.asg.shipping.linemasterthirdparty.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.linemasterthirdparty.dto.*;
import com.asg.shipping.linemasterthirdparty.entity.ShipLineMaster;
import com.asg.shipping.linemasterthirdparty.repository.ShipLineMasterThirdPartyRepository;
import com.asg.shipping.linemasterthirdparty.util.LineMasterThirdPartyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
    private static final String THIRD_PARTY_LINE = "Third Party Line";
    private static final String LINE_POID_FIELD = "linePoid";
    private static final String LINE_NAME_FIELD = "LINE_NAME";
    private static final String LINE_POID_DB_FIELD = "LINE_POID";
    private static final String LINE_TYPE_FIELD = "LINE_TYPE";
    private static final String SHIP_LINE_MASTER = "SHIP_LINE_MASTER";
    private static final String COUNTRY_LOV = "COUNTRY";
    private static final String CURRENCY_LOV = "CURRENCY";
    private static final String CUSTOMER_MASTER_LOV = "CUSTOMER_MASTER";
    private static final String ACTIVE_FIELD = "Active";
    private static final String DOC_ID_LINE_MASTER = "100-013";
    
    // Log messages
    private static final String SEARCHING_THIRD_PARTY_LINES_MSG = "Searching third party lines with page: {}, size: {}";
    private static final String GETTING_THIRD_PARTY_LINE_MSG = "Getting third party line with id: {}";
    private static final String CREATING_THIRD_PARTY_LINE_MSG = "Creating third party line with code: {}, name: {}";
    private static final String UPDATING_THIRD_PARTY_LINE_MSG = "Updating third party line with id: {}";
    private static final String TOGGLING_ACTIVE_STATUS_MSG = "Toggling active status for third party line with id: {}";
    private static final String DELETING_THIRD_PARTY_LINE_MSG = "Deleting third party line with id: {}";
    private static final String SUCCESSFULLY_RETRIEVED_MSG = "Successfully retrieved third party line with id: {}";
    private static final String SUCCESSFULLY_CREATED_MSG = "Successfully created third party line with id: {}";
    private static final String SUCCESSFULLY_UPDATED_MSG = "Successfully updated third party line with id: {}";
    private static final String SUCCESSFULLY_TOGGLED_MSG = "Successfully toggled active status for third party line with id: {} to {}";
    private static final String SUCCESSFULLY_DELETED_MSG = "Successfully deleted third party line with id: {}";
    private static final String FAILED_TO_FETCH_LOV_MSG = "Failed to fetch some LOV data";
    
    // Validation messages
    private static final String LINE_CODE_EXISTS_MSG = "Line code already exists for this group";
    private static final String LINE_NAME_EXISTS_MSG = "Line name already exists for this group";
    private static final String COUNTRY_NOT_ACTIVE_MSG = "Country is not active";
    private static final String CURRENCY_NOT_ACTIVE_MSG = "Currency is not active";
    private static final String BILL_TO_NOT_ACTIVE_MSG = "Bill to is not active";
    
    // Log detail format
    private static final String KEY_ID_FORMAT = "KeyId = LINE_POID:%s";

    private final ShipLineMasterThirdPartyRepository lineRepository;
    private final LovDataService lovService;
    private final LineMasterThirdPartyMapper mapper;
    private final LoggingService loggingService;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchThirdPartyLines(com.asg.common.lib.dto.FilterRequestDto request, Pageable pageable) {
        log.info(SEARCHING_THIRD_PARTY_LINES_MSG, pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentSearchService.resolveOperator(request);
        String isDeleted = documentSearchService.resolveIsDeleted(request);
        List<FilterDto> filters = documentSearchService.resolveFilters(request);

        // Add mandatory LINE_TYPE filter to ensure only THIRD_PARTY records are returned
        filters.add(new FilterDto(LINE_TYPE_FIELD, LINE_TYPE_THIRD_PARTY));

        RawSearchResult raw = documentSearchService.search(DOC_ID_LINE_MASTER, filters, operator, pageable, isDeleted,
                LINE_NAME_FIELD,
                LINE_POID_DB_FIELD);

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public LineMasterThirdPartyDto getThirdPartyLine(Long id) {
        log.info(GETTING_THIRD_PARTY_LINE_MSG, id);

        Long groupPoid = getGroupPoid();
        ShipLineMaster line = lineRepository.findByLinePoidAndGroupPoidAndThirdParty(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(THIRD_PARTY_LINE, LINE_POID_FIELD, id.toString()));

        // Additional validation: ensure LINE_TYPE is THIRD_PARTY
        if (!LINE_TYPE_THIRD_PARTY.equals(line.getLineType())) {
            throw new ResourceNotFoundException(THIRD_PARTY_LINE, LINE_POID_FIELD, id.toString());
        }

        LineMasterThirdPartyDto dto = mapper.mapToDto(line);

        // Enrich with LOV data
        enrichDtoWithLovData(dto, line, groupPoid);

        log.info(SUCCESSFULLY_RETRIEVED_MSG, id);
        return dto;
    }

    @Override
    @Transactional
    public LineMasterThirdPartyDto createThirdPartyLine(LineMasterThirdPartyCreateDTO dto) {
        log.info(CREATING_THIRD_PARTY_LINE_MSG, dto.getLineCode(), dto.getLineName());

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
        loggingService.createLogSummaryEntry(UserContext.getDocumentId(), saved.getLinePoid().toString(), LogDetailsEnum.CREATED.getDescription());

        // Fetch and return with LOV data
        LineMasterThirdPartyDto result = mapper.mapToDto(saved);
        enrichDtoWithLovData(result, saved, groupPoid);

        log.info(SUCCESSFULLY_CREATED_MSG, saved.getLinePoid());
        return result;
    }

    @Override
    @Transactional
    public LineMasterThirdPartyDto updateThirdPartyLine(Long id, LineMasterThirdPartyUpdateDTO dto) {
        log.info(UPDATING_THIRD_PARTY_LINE_MSG, id);

        Long groupPoid = getGroupPoid();
        Long userPoid = getUserPoid();
        Long companyPoid = getCompanyPoid();

        // Find existing line (with LINE_TYPE = 'THIRD_PARTY' filter)
        ShipLineMaster line = lineRepository.findByLinePoidAndGroupPoidAndThirdParty(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(THIRD_PARTY_LINE, LINE_POID_FIELD, id.toString()));

        // Additional validation: ensure LINE_TYPE is THIRD_PARTY
        if (!LINE_TYPE_THIRD_PARTY.equals(line.getLineType())) {
            throw new ResourceNotFoundException(THIRD_PARTY_LINE, LINE_POID_FIELD, id.toString());
        }

        // Validate
        validateLineUpdateDTO(dto, groupPoid, id);

        // Store old values for logging
        ShipLineMaster oldLine = new ShipLineMaster();
        BeanUtils.copyProperties(line,oldLine);

        // Update main entity
        mapper.mapUpdateDTOToEntity(dto, line, groupPoid, userPoid, companyPoid);

        // Ensure LINE_TYPE remains THIRD_PARTY (cannot be changed)
        line.setLineType(LINE_TYPE_THIRD_PARTY);

        ShipLineMaster saved = lineRepository.save(line);

        // Log changes
        loggingService.logChanges(oldLine, saved, ShipLineMaster.class, UserContext.getDocumentId(), id.toString(), LogDetailsEnum.MODIFIED, LINE_POID_DB_FIELD);

        // Fetch and return with LOV data
        LineMasterThirdPartyDto result = mapper.mapToDto(saved);
        enrichDtoWithLovData(result, saved, groupPoid);

        log.info(SUCCESSFULLY_UPDATED_MSG, id);
        return result;
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        log.info(TOGGLING_ACTIVE_STATUS_MSG, id);

        Long groupPoid = getGroupPoid();
        ShipLineMaster line = lineRepository.findByLinePoidAndGroupPoidAndThirdParty(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(THIRD_PARTY_LINE, LINE_POID_FIELD, id.toString()));

        // Additional validation: ensure LINE_TYPE is THIRD_PARTY
        if (!LINE_TYPE_THIRD_PARTY.equals(line.getLineType())) {
            throw new ResourceNotFoundException(THIRD_PARTY_LINE, LINE_POID_FIELD, id.toString());
        }

        final String currentActive = line.getActive();
        line.setActive("Y".equals(currentActive) ? "N" : "Y");
        line.setLastModifiedBy(getCurrentUser());
        line.setLastModifiedDate(LocalDateTime.now());

        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, UserContext.getDocumentId(), id.toString());
        String logDetail = String.format(KEY_ID_FORMAT, id);
        String tableName = ShipLineMaster.class.getAnnotation(jakarta.persistence.Table.class).name();
        loggingService.createLogDetailsEntry(UserContext.getDocumentId(), id.toString(), ACTIVE_FIELD, currentActive, line.getActive(), logDetail, tableName);
        lineRepository.save(line);
        log.info(SUCCESSFULLY_TOGGLED_MSG, id, line.getActive());
    }

    @Override
    @Transactional
    public void deleteThirdPartyLine(Long id, DeleteReasonDto deleteReasonDto) {
        log.info(DELETING_THIRD_PARTY_LINE_MSG, id);

        Long groupPoid = getGroupPoid();
        ShipLineMaster line = lineRepository.findByLinePoidAndGroupPoidAndThirdParty(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(THIRD_PARTY_LINE, LINE_POID_FIELD, id.toString()));

        // Additional validation: ensure LINE_TYPE is THIRD_PARTY
        if (!LINE_TYPE_THIRD_PARTY.equals(line.getLineType())) {
            throw new ResourceNotFoundException(THIRD_PARTY_LINE, LINE_POID_FIELD, id.toString());
        }

        documentDeleteService.deleteDocument(
                id,
                SHIP_LINE_MASTER,
                LINE_POID_DB_FIELD,
                deleteReasonDto,
                null
        );

        log.info(SUCCESSFULLY_DELETED_MSG, id);
    }

    /**
     * Enrich DTO with LOV data
     */
    private void enrichDtoWithLovData(LineMasterThirdPartyDto dto, ShipLineMaster line, Long groupPoid) {
        Long companyPoid = getCompanyPoid();
        Long userPoid = getUserPoid();

        try {
            if (line.getCountryPoid() != null) {
                dto.setCountryDet(lovService.getDetailsByPoidAndLovName(line.getCountryPoid(), COUNTRY_LOV));
            }
            if (line.getCurrencyPoid() != null) {
                dto.setCurrencyDet(lovService.getDetailsByPoidAndLovName(line.getCurrencyPoid(), CURRENCY_LOV));
            }
            if (line.getBillTo() != null) {
                dto.setBillToDet(lovService.getLovItemByCodeFast(line.getBillTo(), CUSTOMER_MASTER_LOV));
            }
        } catch (Exception e) {
            log.warn(FAILED_TO_FETCH_LOV_MSG, e);
        }
    }

    /**
     * Validate LineCreateDTO
     */
    private void validateLineCreateDTO(LineMasterThirdPartyCreateDTO dto, Long groupPoid) {
        if (lineRepository.existsByLineCodeAndGroupPoidAndThirdParty(dto.getLineCode(), groupPoid)) {
            throw new ValidationException(LINE_CODE_EXISTS_MSG);
        }
        if (lineRepository.existsByLineNameAndGroupPoidAndThirdParty(dto.getLineName(), groupPoid)) {
            throw new ValidationException(LINE_NAME_EXISTS_MSG);
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
            throw new ValidationException(LINE_NAME_EXISTS_MSG);
        }

        validateCountry(dto.getCountryPoid());
        validateCurrency(dto.getCurrencyPoid());
        validateBillTo(dto.getBillTo());
    }


    private void validateCountry(Long countryPoid) {
        if (countryPoid != null) {
            LovGetListDto lovGetListDto = lovService.getDetailsByPoidAndLovName(countryPoid, COUNTRY_LOV);
            if (lovGetListDto == null || lovGetListDto.getPoid() == null)
                throw new ValidationException(COUNTRY_NOT_ACTIVE_MSG);
        }
    }

    private void validateCurrency(Long currencyPoid) {
        if (currencyPoid != null) {
            LovGetListDto lovGetListDto = lovService.getDetailsByPoidAndLovName(currencyPoid, CURRENCY_LOV);
            if (lovGetListDto == null || lovGetListDto.getPoid() == null)
                throw new ValidationException(CURRENCY_NOT_ACTIVE_MSG);
        }
    }

    private void validateBillTo(String billToCode) {
        if (billToCode != null) {
            LovGetListDto lovGetListDto = lovService.getLovItemByCodeFast(billToCode, CUSTOMER_MASTER_LOV);
            if (lovGetListDto == null || lovGetListDto.getPoid() == null)
                throw new ValidationException(BILL_TO_NOT_ACTIVE_MSG);
        }
    }
}
