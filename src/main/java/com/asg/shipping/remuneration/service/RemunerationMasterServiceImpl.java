package com.asg.shipping.remuneration.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.common.repository.GlMasterRepository;
import com.asg.shipping.remuneration.dto.ShipRemunerationMasterRequestDto;
import com.asg.shipping.remuneration.dto.ShipRemunerationMasterResponseDto;
import com.asg.shipping.remuneration.entity.ShipRemunerationMaster;
import com.asg.shipping.remuneration.mapper.ShipRemunerationMasterMapper;
import com.asg.shipping.remuneration.repository.ShipRemunerationMasterRepository;
import com.asg.shipping.shippingffchargemaster.repository.ShipChargeMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RemunerationMasterServiceImpl implements RemunerationMasterService {

    private final GlMasterRepository glMasterRepository;
    private final DocumentSearchService documentService;
    private final ShipRemunerationMasterRepository repository;
    private final ShipChargeMasterRepository shipChargeMasterRepository;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;

    private static final String REMUNERATION = "Remuneration";
    private static final String REMUNERATION_POID = "REMUNERATION_POID";

    @Override
    public Map<String, Object> listRemunerations(String docId, FilterRequestDto request, Pageable pageable) {

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "REMUN_DESCRIPTION",   // label
                REMUNERATION_POID);    // value

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public ShipRemunerationMasterResponseDto createRemuneration(ShipRemunerationMasterRequestDto requestDto) {
        log.info("Creating remuneration with code: {}", requestDto.getRemunCode());

        if (repository.existsByRemunCodeIgnoreCase(requestDto.getRemunCode())) {
            throw new ResourceAlreadyExistsException("Remuneration Code", requestDto.getRemunCode());
        }
        if (requestDto.getRemunChargeCodePoid() != null && !shipChargeMasterRepository.existsByChargePoid(requestDto.getRemunChargeCodePoid())) {
            throw new ResourceNotFoundException("Charge Master", "Remuneration Charge Code Poid", requestDto.getRemunChargeCodePoid());
        }
        if (requestDto.getGlPoid() != null && !glMasterRepository.existsByGlPoid(requestDto.getGlPoid())) {
            throw new ResourceNotFoundException("Gl Master", "Gl Poid", requestDto.getGlPoid());
        }

        ShipRemunerationMaster entity = ShipRemunerationMasterMapper.toEntity(requestDto);
        ShipRemunerationMaster saved = repository.save(entity);

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), saved.getRemunerationPoid().toString());

        log.info("Successfully created remuneration with id: {}", saved.getRemunerationPoid());
        return getRemunerationById(saved.getRemunerationPoid());
    }

    @Override
    public ShipRemunerationMasterResponseDto updateRemuneration(Long remunerationPoid, ShipRemunerationMasterRequestDto requestDto) {
        log.info("Updating remuneration with id: {}", remunerationPoid);

        ShipRemunerationMaster entity = repository.findById(remunerationPoid)
                .orElseThrow(() -> new ResourceNotFoundException(REMUNERATION, REMUNERATION_POID, remunerationPoid));

        ShipRemunerationMaster oldEntity = new ShipRemunerationMaster();
        BeanUtils.copyProperties(entity, oldEntity);

        if (requestDto.getRemunChargeCodePoid() != null && !shipChargeMasterRepository.existsByChargePoid(requestDto.getRemunChargeCodePoid())) {
            throw new ResourceNotFoundException("Charge Master", "Remuneration Charge Code Poid", requestDto.getRemunChargeCodePoid());
        }
        if (requestDto.getGlPoid() != null && !glMasterRepository.existsByGlPoid(requestDto.getGlPoid())) {
            throw new ResourceNotFoundException("Gl Master", "Gl Poid", requestDto.getGlPoid());
        }

        ShipRemunerationMasterMapper.updateEntity(requestDto, entity);
        ShipRemunerationMaster updated = repository.save(entity);

        String docId = UserContext.getDocumentId();
        String key = updated.getRemunerationPoid().toString();

        loggingService.logChanges(oldEntity, updated, ShipRemunerationMaster.class, docId, key, LogDetailsEnum.MODIFIED, REMUNERATION_POID);

        log.info("Successfully updated remuneration with id: {}", remunerationPoid);
        return ShipRemunerationMasterMapper.toResponseDto(updated);
    }

    @Override
    public ShipRemunerationMasterResponseDto getRemunerationById(Long remunerationPoid) {
        log.info("Getting remuneration with id: {}", remunerationPoid);

        ShipRemunerationMaster entity = repository.findById(remunerationPoid)
                .orElseThrow(() -> new ResourceNotFoundException(REMUNERATION, "Remuneration Poid", remunerationPoid));

        log.info("Successfully retrieved remuneration with id: {}", remunerationPoid);
        return ShipRemunerationMasterMapper.toResponseDto(entity);
    }

    @Override
    public void deleteRemuneration(Long remunerationPoid, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting remuneration with id: {}", remunerationPoid);

        ShipRemunerationMaster entity = repository.findById(remunerationPoid)
                .orElseThrow(() -> new ResourceNotFoundException(REMUNERATION, "Remuneration Poid", remunerationPoid));

        documentDeleteService.deleteDocument(
                remunerationPoid,
                "SHIP_REMUNERATION_MASTER",
                REMUNERATION_POID,
                deleteReasonDto,
                LocalDate.from(entity.getCreatedDate())
        );
    }
}
