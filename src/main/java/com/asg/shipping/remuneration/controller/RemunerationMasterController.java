package com.asg.shipping.remuneration.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.remuneration.dto.ShipRemunerationMasterRequestDto;
import com.asg.shipping.remuneration.service.RemunerationMasterService;
import jakarta.xml.bind.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/remuneration-master")
public class RemunerationMasterController {

    private final RemunerationMasterService service;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listRemunerations(@ParameterObject Pageable pageable,
                                               @RequestBody(required = false) FilterRequestDto filters) {
        return ApiResponse.success("Remunerations retrieved successfully", service.listRemunerations(UserContext.getDocumentId(), filters, pageable));
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createRemuneration(@RequestBody ShipRemunerationMasterRequestDto requestDto) throws ValidationException {
        return ApiResponse.success("Remuneration created successfully", service.createRemuneration(requestDto));
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{remunerationPoid}")
    public ResponseEntity<?> update(@PathVariable Long remunerationPoid, @RequestBody ShipRemunerationMasterRequestDto requestDto) {
        return ApiResponse.success("Remuneration updated successfully", service.updateRemuneration(remunerationPoid, requestDto));
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{remunerationPoid}")
    public ResponseEntity<?> getById(@PathVariable Long remunerationPoid) {
        return ApiResponse.success("Remuneration retrieved successfully", service.getRemunerationById(remunerationPoid));
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{remunerationPoid}")
    public ResponseEntity<?> delete(@PathVariable Long remunerationPoid) {
        service.softDeleteRemuneration(remunerationPoid);
        return ApiResponse.success("Remuneration soft deleted successfully");
    }
}
