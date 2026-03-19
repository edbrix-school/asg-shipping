package com.asg.shipping.shipcommisiontransfer.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.shipcommisiontransfer.dto.CalculateCommissionRequestDTO;
import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionTransferCreateDTO;
import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionTransferDto;
import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionTransferUpdateDTO;
import com.asg.shipping.shipcommisiontransfer.service.ShipCommissionTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.asg.shipping.common.ApiResponse.internalServerError;
import static com.asg.shipping.common.ApiResponse.success;

/**
 * REST Controller for Ship Commission Transfer operations
 */
@RestController
@RequestMapping("/v1/ship-commission-transfer")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ShipCommissionTransferController {

    private final ShipCommissionTransferService commissionTransferService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "List Ship Commission Transfer with Search and Sort",
            description = "Provide search filters (supports GLOBALSEARCH and field filters).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestHeader("X-Document-Id") String docId,
            @RequestHeader(value = "X-Action-Requested", required = false) String actionRequested
    ) {
        try {
            log.info("List Ship Commission Transfer request | page={}, size={}, docId={}, actionRequested={}",
                    pageable.getPageNumber(), pageable.getPageSize(), docId, actionRequested);
            Map<String, Object> result = commissionTransferService.searchShipCommissionTransfer(docId, filters, pageable);
            return success("Ship Commission Transfer list fetched successfully", result);
        } catch (Exception e) {
            return internalServerError("Unable to fetch ship commission transfers: " + e.getMessage());
        }
    }

    @GetMapping(value = "/{transactionPoid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get Ship Commission Transfer by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> get(
            @PathVariable @NotNull @Positive Long transactionPoid,
            @RequestHeader(value = "X-Action-Requested", required = false) String actionRequested) {
        log.info("Get Ship Commission Transfer request | transactionPoid={}, actionRequested={}", transactionPoid, actionRequested);
        ShipCommissionTransferDto dto = commissionTransferService.getShipCommissionTransfer(transactionPoid);
        return success("Ship Commission Transfer fetched successfully", dto);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @Operation(summary = "Create Ship Commission Transfer", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> create(
            @Valid @RequestBody ShipCommissionTransferCreateDTO createDTO,
            @RequestHeader("X-Document-Id") String docId,
            @RequestHeader(value = "X-Action-Requested", required = false) String actionRequested) {
        log.info("Create Ship Commission Transfer request | docId={}, actionRequested={}", docId, actionRequested);
        ShipCommissionTransferDto dto = commissionTransferService.createShipCommissionTransfer(createDTO);
        return success("Ship Commission Transfer created successfully", dto);
    }

    @PutMapping(value = "/{transactionPoid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Update Ship Commission Transfer", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> update(
            @PathVariable @NotNull @Positive Long transactionPoid,
            @Valid @RequestBody ShipCommissionTransferUpdateDTO updateDTO,
            @RequestHeader("X-Document-Id") String docId,
            @RequestHeader(value = "X-Action-Requested", required = false) String actionRequested) {
        log.info("Update Ship Commission Transfer request | transactionPoid={}, docId={}, actionRequested={}",
                transactionPoid, docId, actionRequested);
        ShipCommissionTransferDto dto = commissionTransferService.updateShipCommissionTransfer(transactionPoid, updateDTO);
        return success("Ship Commission Transfer updated successfully", dto);
    }

    @DeleteMapping(value = "/{transactionPoid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @Operation(summary = "Delete Ship Commission Transfer (Soft delete)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> delete(
            @PathVariable @NotNull @Positive Long transactionPoid,
            @RequestHeader(value = "X-Action-Requested", required = false) String actionRequested) {
        log.info("Delete Ship Commission Transfer request | transactionPoid={}, actionRequested={}", transactionPoid, actionRequested);
        commissionTransferService.deleteShipCommissionTransfer(transactionPoid);
        return success("Ship Commission Transfer deleted successfully");
    }

    @PostMapping(value = "/{transactionPoid}/calculate-commission", produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Calculate commission amounts", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> calculateCommission(
            @PathVariable @NotNull @Positive Long transactionPoid,
            @RequestBody(required = false) CalculateCommissionRequestDTO request,
            @RequestHeader(value = "X-Action-Requested", required = false) String actionRequested) {
        log.info("Calculate commission request | transactionPoid={}, actionRequested={}", transactionPoid, actionRequested);
        if (request == null) {
            request = CalculateCommissionRequestDTO.builder().recalculateAll(true).build();
        }
        Map<String, Object> result = commissionTransferService.calculateCommission(transactionPoid, request);
        return success("Commission calculated successfully", result);
    }

    @PostMapping(value = "/{transactionPoid}/load-from-voyage", produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Load commission data from voyage/manifest", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> loadFromVoyage(
            @PathVariable @NotNull @Positive Long transactionPoid,
            @RequestHeader(value = "X-Action-Requested", required = false) String actionRequested) {
        log.info("Load from voyage request | transactionPoid={}, actionRequested={}", transactionPoid, actionRequested);
        Map<String, Object> result = commissionTransferService.loadFromVoyage(transactionPoid);
        return success("Commission data loaded from voyage successfully", result);
    }

    @PostMapping(value = "/{transactionPoid}/insert-pda", produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Insert commission data into PDA system", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> insertPdaCommission(
            @PathVariable @NotNull @Positive Long transactionPoid,
            @RequestHeader(value = "X-Action-Requested", required = false) String actionRequested) {
        log.info("Insert PDA commission request | transactionPoid={}, actionRequested={}", transactionPoid, actionRequested);
        Map<String, Object> result = commissionTransferService.insertPdaCommission(transactionPoid);
        return success("Commission data inserted into PDA successfully", result);
    }

    @GetMapping("/voyage/{voyageId}")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    public ResponseEntity<?> getVoyageCurrency(@PathVariable Long voyageId) {

        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();

        try {
            var result = commissionTransferService.getCurrencyExchangeForVoyage(groupPoid,companyPoid,userPoid,voyageId);
            return success("Currency exchange fetched successfully", result);
        } catch (Exception e) {
            return internalServerError("Error: " + e.getMessage());
        }
    }

    @GetMapping("/pda-fda-details/{transactionPoid}")
    @AllowedAction(UserRolesRightsEnum.VIEW)
    public ResponseEntity<?> getPdaFdaDetails(@PathVariable Long transactionPoid) {

        try {
            var result = commissionTransferService.getPdaFdaDetails(transactionPoid);
            return success("Data fetched successfully", result);
        } catch (Exception e) {
            return internalServerError("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{voyageId}")
    @AllowedAction(UserRolesRightsEnum.EDIT)
    public ResponseEntity<?>  getCommission(
            @PathVariable Long voyageId, @RequestParam(required = false) Long transactionId) {

        try {
            List<Object[]>  result = commissionTransferService.getCommissionByVoyage(voyageId, transactionId);
            return success("Data fetched successfully", result);
        } catch (Exception e) {
            return internalServerError("Error: " + e.getMessage());
        }
    }
}
