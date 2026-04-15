package com.asg.shipping.linecommission.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.shipping.containertypes.dto.ContainerTypeDto;
import com.asg.shipping.linecommission.dto.LineCommissionResponse;
import com.asg.shipping.linecommission.dto.LineCommissionRequest;
import com.asg.shipping.linecommission.service.LineCommissionService;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.asg.shipping.common.ApiResponse.internalServerError;
import static com.asg.shipping.common.ApiResponse.success;

@RestController
@RequestMapping("/v1/line-commission")
@RequiredArgsConstructor
@Validated
@Slf4j
public class LineCommissionController {

    private final LineCommissionService service;

    /**
     * Controller signature mirrors ags-shipping pattern (see ContainerTerminalTypeController).
     * Uses common-lib FilterRequestDto and DocumentSearchService behind the scenes.
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "List Line Commission with Search and Sort",
            description = """
                    Provide search filters (supports GLOBALSEARCH and field filters).
                    Sorting is applied as configured in doc_master list_of_records_sql for the provided X-Document-Id.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            description = "Common filter request. operator can be AND/OR; isDeleted N/Y; filters list supports GLOBALSEARCH.",
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "operator": "OR",
                              "isDeleted": "N",
                              "filters": [
                                { "searchField": "GLOBALSEARCH", "searchValue": "MAERSK" }
                              ]
                            }
                            """)
            )
    )
    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestHeader("X-Document-Id") String docId,
            @RequestHeader(value = "X-Action-Requested", required = false) String actionRequested
    ) {
        try {
            log.info("List LineCommission request | page={}, size={}, docId={}, actionRequested={}",
                    pageable.getPageNumber(), pageable.getPageSize(), docId, actionRequested);
            Map<String, Object> result = service.listLineCommissions(docId, filters, pageable);
            return success("Line commission list fetched successfully", result);
        } catch (Exception e) {
            return internalServerError("Unable to fetch line commissions: " + e.getMessage());
        }
    }

    @GetMapping(value = "/{transactionPoid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get Line Commission by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> get(@PathVariable @NotNull @Positive Long transactionPoid,
                                 @RequestHeader("X-Group-Poid") Long groupPoid,
                                 @RequestHeader(value = "X-Action-Requested", required = false) String actionRequested) {
        log.info("Get LineCommission request | transactionPoid={}, groupPoid={}, actionRequested={}",
                transactionPoid, groupPoid, actionRequested);
        LineCommissionResponse resp = service.getById(transactionPoid, groupPoid);
        return success("Line commission fetched successfully", resp);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @Operation(summary = "Create Line Commission", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> create(@Valid @RequestBody LineCommissionRequest request,
                                    @RequestHeader("X-Group-Poid") Long groupPoid,
                                    @RequestHeader("X-User-Id") String userId,
                                    @RequestHeader("X-Document-Id") String docId,
                                    @RequestHeader(value = "X-Action-Requested", required = false) String actionRequested) {
        log.info("Create LineCommission request | groupPoid={}, userId={}, docId={}, actionRequested={}",
                groupPoid, userId, docId, actionRequested);
        LineCommissionResponse resp = service.create(request, groupPoid, userId, docId);
        return success("Line commission created successfully", resp);
    }

    @PutMapping(value = "/{transactionPoid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Update Line Commission", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> update(@PathVariable @NotNull @Positive Long transactionPoid,
                                    @Valid @RequestBody LineCommissionRequest request,
                                    @RequestHeader("X-Group-Poid") Long groupPoid,
                                    @RequestHeader("X-User-Id") String userId,
                                    @RequestHeader("X-Document-Id") String docId,
                                    @RequestHeader(value = "X-Action-Requested", required = false) String actionRequested) {
        log.info("Update LineCommission request | transactionPoid={}, groupPoid={}, userId={}, docId={}, actionRequested={}",
                transactionPoid, groupPoid, userId, docId, actionRequested);
        LineCommissionResponse resp = service.update(transactionPoid, request, groupPoid, userId, docId);
        return success("Line commission updated successfully", resp);
    }

    @DeleteMapping(value = "/{transactionPoid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @Operation(summary = "Delete Line Commission (Soft delete)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> delete(@PathVariable @NotNull @Positive Long transactionPoid,
                                    @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto,
                                    @RequestHeader(value = "X-Action-Requested", required = false) String actionRequested) {
        log.info("Delete LineCommission request | transactionPoid={}, actionRequested={}",
                transactionPoid, actionRequested);
        service.delete(transactionPoid, deleteReasonDto);
        return success("Line commission deleted successfully");
    }

    @GetMapping(value = "/lines/{linePoid}/container-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Load Container Types by Line (path)", description = "Preferred endpoint: /v1/line-commissions/lines/{linePoid}/container-types",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> loadContainerTypesByLinePath(@PathVariable("linePoid") @NotNull @Positive Long linePoid,
                                                          @RequestHeader("X-Group-Poid") Long groupPoid,
                                                          @RequestHeader("X-User-Id") String userId,
                                                          @RequestHeader(value = "X-Action-Requested", required = false) String actionRequested) {
        log.info("LoadContainerTypesByLinePath request | linePoid={}, groupPoid={}, userId={}, actionRequested={}",
                linePoid, groupPoid, userId, actionRequested);
        List<ContainerTypeDto> resp = service.loadContainerTypes(linePoid, groupPoid, userId);
        return success("Records loaded", resp);
    }
}


