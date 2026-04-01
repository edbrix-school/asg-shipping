package com.asg.shipping.shippingFFChargeMaster.service;


import com.asg.common.lib.dto.*;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeCreateDTO;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeDto;
import com.asg.shipping.shippingFFChargeMaster.dto.ChargeUpdateDTO;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.shippingFFChargeMaster.dto.ShippingChargeLineResponseDto;
import com.asg.shipping.shippingFFChargeMaster.entity.ShipChargeMaster;
import com.asg.shipping.shippingFFChargeMaster.repository.ShipChargeMasterRepository;
import com.asg.shipping.shippingFFChargeMaster.repository.ShippingChargeLineViewRepository;
import com.asg.shipping.shippingFFChargeMaster.util.ChargeMasterMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingFFChargeMasterServiceImpl implements ShippingFFChargeMasterService{

    private final ShipChargeMasterRepository chargeRepository;
    private final ChargeMasterMapper mapper;
    private final DocumentDeleteService documentDeleteService;
    private final DocumentSearchService documentService;
    private final ShippingChargeLineViewRepository shippingChargeLineViewRepository;
    private final LoggingService loggingService;
    private final LovDataService lovService;

    @Override
    @Transactional
    public ChargeDto createCharge(ChargeCreateDTO dto, Long groupPoid, Long userPoid) {
        log.info("Creating charge with code: {}, groupId: {}, userPoid: {}", dto.getChargeCode(), groupPoid, userPoid);

        ShipChargeMaster charge = new ShipChargeMaster();
        mapper.mapCreateDTOToEntity(dto, charge, groupPoid, userPoid);

        validateCreateUniqueness(dto);
        validateTaxMaster(dto.getTaxPoid());
        validateChargeGroupMaster(dto.getChargeGroupPoid());
        validateChargeMasterFF(dto.getShFfChargeMap());
        validateDivision(dto.getDivisionCode());

        ShipChargeMaster saved = chargeRepository.save(charge);

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), saved.getChargePoid().toString());

        log.info("Successfully created charge with id: {}", saved.getChargePoid());
        return mapper.mapToDto(saved);
    }

    @Override
    @Transactional
    public ChargeDto updateCharge(Long id, ChargeUpdateDTO dto, Long groupPoid, Long userPoid) {
        log.info("Updating charge with id: {}, groupId: {}, userPoid: {}", id, groupPoid, userPoid);

        ShipChargeMaster charge = chargeRepository.findByChargePoid(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charge", "chargePoid", id.toString()));

        ShipChargeMaster oldChargeMaster = new ShipChargeMaster();
        BeanUtils.copyProperties(charge, oldChargeMaster);

        mapper.mapUpdateDTOToEntity(dto, charge, groupPoid, userPoid);

        validateUpdateUniqueness(id, dto);
        validateTaxMaster(dto.getTaxPoid());
        validateChargeGroupMaster(dto.getChargeGroupPoid());
        validateChargeMasterFF(dto.getShFfChargeMap());
        validateDivision(dto.getDivisionCode());
        ShipChargeMaster saved = chargeRepository.save(charge);

        loggingService.logChanges(oldChargeMaster, saved, ShipChargeMaster.class, UserContext.getDocumentId(), id.toString(), LogDetailsEnum.MODIFIED, "CHARGE_POID");

        log.info("Successfully updated charge with id: {}", id);
        return mapper.mapToDto(saved);
    }

    @Override
    @Transactional
    public ChargeDto getCharge(Long id) {
        log.info("Getting charge with id: {}", id);


        ShipChargeMaster charge = chargeRepository.findByChargePoid(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charge", "chargePoid", id.toString()));

        ChargeDto dto = mapper.mapToDto(charge);


        List<ShippingChargeLineResponseDto> lines =
                shippingChargeLineViewRepository.findByChargePoid(id)
                        .stream()
                        .map(v -> new ShippingChargeLineResponseDto(
                                v.getChargePoid(),
                                v.getLinePoid(),
                                v.getLineName(),
                                v.getLineCode()
                        ))
                        .toList();


        dto.setShippingChargeLineResponseDtoList(lines);
        log.info("Successfully retrieved charge with id: {}", id);
        return dto;
    }

    @Override
    @Transactional
    public void deleteCharge(Long id, DeleteReasonDto deleteReasonDto) {


        ShipChargeMaster charge = chargeRepository.findByChargePoid(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charge", "chargePoid", id.toString()));


        documentDeleteService.deleteDocument(
                id,
                "SHIP_CHARGE_MASTER",
                "CHARGE_POID",
                deleteReasonDto,
                null
        );

    }

    private void validateCreateUniqueness(ChargeCreateDTO dto) {

        String code = dto.getChargeCode().trim();
        String name = dto.getChargeName().trim();
        String division = dto.getDivisionCode();

        if (chargeRepository.existsByChargeCodeAndDivisionCode(code, division)) {
            throw new ValidationException(
                    "Charge Code '" + code +
                            "' already exists for Division '" + division + "'"
            );
        }

        if (chargeRepository.existsByChargeNameIgnoreCaseAndDivisionCode(name, division)) {
            throw new ValidationException(
                    "Charge Name '" + name +
                            "' already exists for Division '" + division + "'"
            );
        }
    }

    private void validateUpdateUniqueness(Long id, ChargeUpdateDTO dto) {

       // String code = dto.getChargeCode().trim();
        String name = dto.getChargeName().trim();
        String division = dto.getDivisionCode();

      /*  if (chargeRepository.existsByChargeCodeAndDivisionCodeAndChargePoidNot(code, division, id)) {
            throw new ValidationException(
                    "Charge Code '" + code +
                            "' already exists for Division '" + division + "'"
            );
        }*/

        if (chargeRepository.existsByChargeNameIgnoreCaseAndDivisionCodeAndChargePoidNot(name, division, id)) {
            throw new ValidationException(
                    "Charge Name '" + name +
                            "' already exists for Division '" + division + "'"
            );
        }
    }

    @Override
    public Map<String, Object> searchCharges(String docId, FilterRequestDto request, Pageable pageable) {
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "CHARGE_NAME",   // label
                "CHARGE_POID");  // value

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private void validateTaxMaster(Long taxPoid) {
        if (taxPoid != null) {
            LovGetListDto lovGetListDto = lovService.getDetailsByPoidAndLovName(taxPoid, "TAX_MASTER");
            if (lovGetListDto == null || lovGetListDto.getPoid() == null)
                throw new ValidationException("TaxMaster is not active");
        }
    }

    private void validateChargeGroupMaster(Long chargeGroupPoid) {
        if (chargeGroupPoid != null) {
            LovGetListDto lovGetListDto = lovService.getDetailsByPoidAndLovName(chargeGroupPoid, "CHARGE_GROUP_MASTER");
            if (lovGetListDto == null || lovGetListDto.getPoid() == null)
                throw new ValidationException("ChargeGroupMaster is not active");
        }
    }


    private void validateChargeMasterFF(Long ssFFChargePoid) {
        if (ssFFChargePoid != null) {
            LovGetListDto lovGetListDto = lovService.getDetailsByPoidAndLovName(ssFFChargePoid, "CHARGE_MASTER_FF");
            if (lovGetListDto == null || lovGetListDto.getPoid() == null)
                throw new ValidationException("Charge Master FF is not active");
        }
    }



    private void validateDivision(String divisionCode) {
        if (divisionCode != null) {
            LovGetListDto lovGetListDto = lovService.getDetailsByCodeAndLovName(divisionCode, "SHIP_DIVISION_PRINT");
            if (lovGetListDto == null || lovGetListDto.getPoid() == null)
                throw new ValidationException("Division is not active");
        }
    }

}
