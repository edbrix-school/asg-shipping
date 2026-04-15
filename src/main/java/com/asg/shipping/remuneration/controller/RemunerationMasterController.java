package com.asg.shipping.remuneration.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.remuneration.dto.ShipRemunerationMasterRequestDto;
import com.asg.shipping.remuneration.service.RemunerationMasterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/remuneration-master")
public class RemunerationMasterController {

    private final RemunerationMasterService remunerationMasterService;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listRemunerations(@ParameterObject Pageable pageable, @RequestBody(required = false) FilterRequestDto filters) {
        return ApiResponse.success("Remunerations retrieved successfully", remunerationMasterService.listRemunerations(UserContext.getDocumentId(), filters, pageable));
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createRemuneration(@RequestBody ShipRemunerationMasterRequestDto requestDto) {
        return ApiResponse.success("Remuneration created successfully", remunerationMasterService.createRemuneration(requestDto));
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{remunerationPoid}")
    public ResponseEntity<?> update(@PathVariable Long remunerationPoid, @RequestBody ShipRemunerationMasterRequestDto requestDto) {
        return ApiResponse.success("Remuneration updated successfully", remunerationMasterService.updateRemuneration(remunerationPoid, requestDto));
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{remunerationPoid}")
    public ResponseEntity<?> getById(@PathVariable Long remunerationPoid) {
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), remunerationPoid.toString());
        return ApiResponse.success("Remuneration retrieved successfully", remunerationMasterService.getRemunerationById(remunerationPoid));
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{remunerationPoid}")
    public ResponseEntity<?> delete(@PathVariable Long remunerationPoid, @Valid @RequestBody DeleteReasonDto deleteReasonDto) {
        remunerationMasterService.deleteRemuneration(remunerationPoid, deleteReasonDto);
        return ApiResponse.success("Remuneration soft deleted successfully");
    }
}
