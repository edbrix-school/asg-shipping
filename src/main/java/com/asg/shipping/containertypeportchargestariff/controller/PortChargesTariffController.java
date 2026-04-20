package com.asg.shipping.containertypeportchargestariff.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.containertypeportchargestariff.dto.*;
import com.asg.shipping.containertypeportchargestariff.service.PortChargesTariffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.asg.shipping.common.ApiResponse.success;

@RestController
@RequestMapping("/v1/container-type-port-charges-tariff")
@RequiredArgsConstructor
@Slf4j
public class PortChargesTariffController {

    private final PortChargesTariffService portChargesTariffService;
    final LoggingService loggingService;

    @PostMapping("/list")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    public ResponseEntity<?> listPortChargesTariff(@Valid @RequestBody FilterRequestDto filterRequest, @ParameterObject Pageable pageable) {
        return success("Port charges tariff fetched successfully", portChargesTariffService.listPortChargesTariff(UserContext.getDocumentId(), filterRequest, pageable));
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    public ResponseEntity<?> getPortChargesTariff(@PathVariable Long id) {
        log.info("Getting port charges tariff with id: {}", id);
        PortChargesTariffDto tariff = portChargesTariffService.getPortChargesTariff(id);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());
        log.info("Successfully retrieved port charges tariff with id: {}", id);
        return success("Port charges tariff retrieved successfully", tariff);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createPortChargesTariff(@Valid @RequestBody PortChargesTariffCreateDto dto) {
        log.info("Creating port charges tariff for port: {}, groupId: {}, userPoid: {}",
                dto.getPortPoid(), UserContext.getGroupPoid(), UserContext.getUserPoid());
        PortChargesTariffDto result = portChargesTariffService.createPortChargesTariff(dto, UserContext.getGroupPoid(), UserContext.getUserPoid());
        log.info("Successfully created port charges tariff with id: {}", result.getTransactionPoid());
        return success("Port charges tariff created successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePortChargesTariff(@PathVariable Long id, @Valid @RequestBody PortChargesTariffUpdateDto dto) {
        log.info("Updating port charges tariff with id: {}, groupId: {}, userPoid: {}",
                id, UserContext.getGroupPoid(), UserContext.getUserPoid());
        PortChargesTariffDto result = portChargesTariffService.updatePortChargesTariff(id, dto, UserContext.getGroupPoid(), UserContext.getUserPoid());
        log.info("Successfully updated port charges tariff with id: {}", id);
        return success("Port charges tariff updated successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePortChargesTariff(@PathVariable Long id,
                                                     @Valid @RequestBody DeleteReasonDto deleteReasonDto) {
        log.info("Deleting port charges tariff with id: {}", id);
        portChargesTariffService.deletePortChargesTariff(id, deleteReasonDto);
        log.info("Successfully deleted port charges tariff with id: {}", id);
        return success("Port charges tariff deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/validate-overlap")
    public ResponseEntity<?> validateOverlap(@Valid @RequestBody ValidateOverlapRequestDto request) {
        log.info("Validating overlap for port: {}, line: {}, division: {}",
                request.getPortPoid(), request.getChargeLinePoid(), request.getChargeDivision());
        ValidateOverlapResponseDto result = portChargesTariffService.validateOverlap(request);
        return success("Overlap validation completed", result);
    }
}