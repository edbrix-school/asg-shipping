package com.asg.shipping.remuneration.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.common.repository.GlMasterRepository;
import com.asg.shipping.remuneration.dto.ShipRemunerationMasterRequestDto;
import com.asg.shipping.remuneration.dto.ShipRemunerationMasterResponseDto;
import com.asg.shipping.remuneration.entity.ShipRemunerationMaster;
import com.asg.shipping.remuneration.mapper.ShipRemunerationMasterMapper;
import com.asg.shipping.remuneration.repository.ShipRemunerationMasterRepository;
import com.asg.shipping.shippingFFChargeMaster.repository.ShipChargeMasterRepository;
import jakarta.xml.bind.ValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RemunerationMasterServiceImpl implements RemunerationMasterService {

    private final GlMasterRepository glMasterRepository;
    private final DocumentSearchService documentService;
    private final ShipRemunerationMasterRepository repository;
    private final ShipChargeMasterRepository shipChargeMasterRepository;

    @Override
    public Map<String, Object> listRemunerations(String docId, FilterRequestDto request, Pageable pageable) {

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "REMUN_DESCRIPTION",   // label
                "REMUNERATION_POID");    // value);

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public ShipRemunerationMasterResponseDto createRemuneration(ShipRemunerationMasterRequestDto requestDto) throws ValidationException {
        if (StringUtils.isBlank(requestDto.getRemunCode())) {
            throw new ValidationException("Remuneration code is required", String.valueOf(400));
        }
        if (repository.existsByRemunCodeIgnoreCase(requestDto.getRemunCode())) {
            throw new ResourceAlreadyExistsException("Remuneration Code", requestDto.getRemunCode());
        }
        if (!shipChargeMasterRepository.existsByChargePoid(requestDto.getRemunChargeCodePoid())) {
            throw new ResourceNotFoundException("Charge Master", "Remuneration Charge Code Poid", requestDto.getRemunChargeCodePoid());
        }
        if (!glMasterRepository.existsByGlPoid(requestDto.getGlPoid())) {
            throw new ResourceNotFoundException("Gl Master", "Gl Poid", requestDto.getGlPoid());
        }

        ShipRemunerationMaster entity = ShipRemunerationMasterMapper.toEntity(requestDto);
        ShipRemunerationMaster saved = repository.save(entity);
        return getRemunerationById(saved.getRemunerationPoid());
    }

    @Override
    public ShipRemunerationMasterResponseDto updateRemuneration(Long remunerationPoid, ShipRemunerationMasterRequestDto requestDto) {
        ShipRemunerationMaster entity = repository.findById(remunerationPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Remuneration", "Remuneration Poid", remunerationPoid));

        if (!shipChargeMasterRepository.existsByChargePoid(requestDto.getRemunChargeCodePoid())) {
            throw new ResourceNotFoundException("Charge Master", "Remuneration Charge Code Poid", requestDto.getRemunChargeCodePoid());
        }
        if (!glMasterRepository.existsByGlPoid(requestDto.getGlPoid())) {
            throw new ResourceNotFoundException("Gl Master", "Gl Poid", requestDto.getGlPoid());
        }

        ShipRemunerationMasterMapper.updateEntity(requestDto, entity);
        ShipRemunerationMaster updated = repository.save(entity);
        return ShipRemunerationMasterMapper.toResponseDto(updated);
    }

    @Override
    public ShipRemunerationMasterResponseDto getRemunerationById(Long remunerationPoid) {
        ShipRemunerationMaster entity = repository.findById(remunerationPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Remuneration", "Remuneration Poid", remunerationPoid));

        return ShipRemunerationMasterMapper.toResponseDto(entity);
    }

    @Override
    public void softDeleteRemuneration(Long remunerationPoid) {
        ShipRemunerationMaster entity = repository.findById(remunerationPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Remuneration", "Remuneration Poid", remunerationPoid));
        entity.setDeleted("Y");
        entity.setActive("N");
        entity.setLastModifiedBy(UserContext.getUserId());
        entity.setLastModifiedDate(LocalDateTime.now());
        repository.save(entity);
    }
}
