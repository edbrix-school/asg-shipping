package com.asg.shipping.tradelanemaster.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import org.springframework.dao.DuplicateKeyException;
import com.asg.shipping.tradelanemaster.dto.request.ShipTradelaneRequest;
import com.asg.shipping.tradelanemaster.dto.response.ShipTradelaneResponse;
import com.asg.shipping.tradelanemaster.service.ShipTradeLaneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("v1/trade-lane-master")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "ship-tradelane-controller", description = "Manage Ship TradeLane Master records")
public class ShipTradeLaneController {

    private final ShipTradeLaneService shipTradeLaneService;

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @Operation(
            summary = "Create Ship Trade Lane",
            description = """
                    Create a new Ship Trade Lane Master record.
                    
                    ### Business Rules
                    - **Trade Lane Code** is mandatory and must be unique
                    - **Trade Lane Name** is mandatory and must be unique
                    - **Active** status is mandatory
                    - **Sequence Number** is mandatory
                    
                    ### Authorization Parameters
                    - **documentId:** Document identifier
                    - **actionRequested:** Action being performed (CREATE)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ship Trade Lane created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate code/name"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<?> create(

            @Valid @RequestBody ShipTradelaneRequest request
    ) {
        try {
            ShipTradelaneResponse response = shipTradeLaneService.create(request);
            return success("Ship Trade Lane created successfully", response);
        } catch (ValidationException e) {
            return badRequest(e.getMessage());
        } catch (DuplicateKeyException e) {
            return conflict(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to create Ship Trade Lane: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(
            summary = "Update Ship Trade Lane",
            description = """
                    Update an existing Ship Trade Lane Master record.
                    
                    ### Business Rules
                    - Cannot update deleted records
                    - **Trade Lane Code** must be unique
                    - **Trade Lane Name** must be unique
                    
                    ### Authorization Parameters
                    - **documentId:** Document identifier
                    - **actionRequested:** Action being performed (EDIT)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ship Trade Lane updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate code/name"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Ship Trade Lane not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{tradeLanePoid}")
    public ResponseEntity<?> update(
            @Parameter(description = "Trade Lane POID", required = true)
            @PathVariable Long tradeLanePoid,
            @Valid @RequestBody ShipTradelaneRequest request
    ) {
        try {
            ShipTradelaneResponse response = shipTradeLaneService.update(tradeLanePoid, request);
            return success("Ship Trade Lane updated successfully", response);
        } catch (ValidationException | IllegalArgumentException | IllegalStateException e) {
            return badRequest(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to update Ship Trade Lane: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "Get Ship Trade Lane by ID",
            description = """
                    Retrieve a single Ship Trade Lane Master record by its POID.
                    
                    ### Authorization Parameters
                    - **documentId:** Document identifier
                    - **actionRequested:** Action being performed (VIEW)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ship Trade Lane retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Ship Trade Lane not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{tradeLanePoid}")
    public ResponseEntity<?> getById(
            @Parameter(description = "Trade Lane POID", required = true)
            @PathVariable Long tradeLanePoid
    ) {
        try {
            ShipTradelaneResponse response = shipTradeLaneService.getById(tradeLanePoid);
            return success("Ship Trade Lane retrieved successfully", response);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to retrieve Ship Trade Lane: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @Operation(
            summary = "Delete Ship Trade Lane",
            description = """
                    Soft delete a Ship Trade Lane Master record.
                    
                    ### Authorization Parameters
                    - **documentId:** Document identifier
                    - **actionRequested:** Action being performed (DELETE)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ship Trade Lane deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Ship Trade Lane not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{tradeLanePoid}")
    public ResponseEntity<?> delete(
            @Parameter(description = "Trade Lane POID", required = true)
            @PathVariable Long tradeLanePoid
    ) {
        try {
            shipTradeLaneService.delete(tradeLanePoid);
            return success("Ship Trade Lane deleted successfully", null);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Failed to delete Ship Trade Lane: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "List Ship Trade Lanes",
            description = """
                    Fetch Ship Trade Lane Masters using filters and pagination.
                    
                    Valid `searchField` values: TRADELANE_CODE, TRADELANE_NAME, TRADELANE_NAME2, ACTIVE
                    
                    ### Authorization Parameters
                    - **documentId:** Document identifier
                    - **actionRequested:** Action being performed (VIEW)
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            content = @Content(
                    examples = @ExampleObject(
                            name = "Ship Trade Lane Filters",
                            value = """
                                    {
                                      "operator": "AND",
                                      "isDeleted": "N",
                                      "filters": [
                                        {
                                          "searchField": "TRADELANE_CODE",
                                          "searchValue": "TL001"
                                        },
                                        {
                                          "searchField": "TRADELANE_NAME",
                                          "searchValue": "Asia Pacific"
                                        },
                                        {
                                          "searchField": "ACTIVE",
                                          "searchValue": "Y"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/list")
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,

            @Parameter(description = "Document identifier", required = true)
            @RequestParam String documentId
    ) {
        try {
            Map<String, Object> response = shipTradeLaneService.list(documentId, filters, pageable);
            return success("Ship Trade Lanes list retrieved successfully", response);
        } catch (Exception e) {
            return internalServerError("Failed to retrieve Ship Trade Lanes: " + e.getMessage());
        }
    }
}
