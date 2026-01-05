package com.asg.shipping.collectionhandover.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverCreateDTO;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverDto;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverUpdateDTO;
import com.asg.shipping.collectionhandover.service.CollectionHandoverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.security.util.UserContext.getGroupPoid;
import static com.asg.common.lib.security.util.UserContext.getUserPoid;

/**
 * REST Controller for Collection Handover (Shipping) operations
 */
@RestController
@RequestMapping("/v1/collection-handover-shipping")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Collection Handover Management", description = "APIs for managing collection handover (day end close)")
public class CollectionHandoverController {

    private static final String DOC_ID = "300-106";

    private final CollectionHandoverService collectionHandoverService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/search")
    @Operation(
            summary = "Search collection handovers",
            description = "Retrieve paginated list of collection handovers with optional filtering and sorting using DocumentSearchService",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved collection handovers",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> searchCollectionHandovers(
            @RequestBody(required = false) com.asg.common.lib.dto.FilterRequestDto request,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction (e.g., 'docRef,asc')", example = "docRef,asc")
            @RequestParam(required = false) String sort) {

        log.info("Searching collection handovers with page: {}, size: {}, sort: {}", page, size, sort);

        Pageable pageable = createPageable(page, size, sort);
        Map<String, Object> result = collectionHandoverService.searchCollectionHandovers(DOC_ID, request, pageable);

        log.info("Successfully retrieved collection handovers");
        return ApiResponse.success("Collection handovers retrieved successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    @Operation(
            summary = "Get collection handover details",
            description = "Retrieve complete collection handover information by ID including all detail records and LOV data",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved collection handover",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CollectionHandoverDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Collection handover not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> getCollectionHandover(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long id) {

        log.info("Getting collection handover with id: {}", id);
        CollectionHandoverDto handover = collectionHandoverService.getCollectionHandover(id);
        log.info("Successfully retrieved collection handover with id: {}", id);
        return ApiResponse.success("Collection handover retrieved successfully", handover);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create new collection handover",
            description = "Create a new collection handover with nested detail records",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Successfully created collection handover",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CollectionHandoverDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Document reference already exists",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> createCollectionHandover(
            @Valid @RequestBody CollectionHandoverCreateDTO dto) {

        log.info("Creating collection handover");

        Long groupPoid = getGroupPoid();
        Long userPoid = getUserPoid();

        CollectionHandoverDto created = collectionHandoverService.createCollectionHandover(dto, groupPoid, userPoid);

        log.info("Successfully created collection handover with id: {}", created.getTransactionPoid());
        return ApiResponse.success("Collection handover created successfully", created);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    @Operation(
            summary = "Update collection handover",
            description = "Update an existing collection handover and manage nested detail records",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated collection handover",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CollectionHandoverDto.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Collection handover not found",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Document reference already exists",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> updateCollectionHandover(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @Valid @RequestBody CollectionHandoverUpdateDTO dto) {

        log.info("Updating collection handover with id: {}", id);

        Long groupPoid = getGroupPoid();
        Long userPoid = getUserPoid();

        CollectionHandoverDto updated = collectionHandoverService.updateCollectionHandover(id, dto, groupPoid, userPoid);

        log.info("Successfully updated collection handover with id: {}", id);
        return ApiResponse.success("Collection handover updated successfully", updated);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete collection handover",
            description = "Soft delete a collection handover by setting DELETED='Y'",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully deleted collection handover"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Collection handover not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> deleteCollectionHandover(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long id) {

        log.info("Deleting collection handover with id: {}", id);
        collectionHandoverService.deleteCollectionHandover(id);
        log.info("Successfully deleted collection handover with id: {}", id);
        return ApiResponse.success("Collection handover deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}/verify")
    @Operation(
            summary = "Toggle verify/receive status",
            description = "Update the verified received status and main office remarks",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully updated verify status"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Collection handover not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<?> toggleVerifyStatus(
            @Parameter(description = "Transaction POID", required = true, example = "12345")
            @PathVariable Long id,
            @Parameter(description = "Verified received status (Y or N)", required = true)
            @RequestParam String verifiedRcvd,
            @Parameter(description = "Main office remarks (optional)")
            @RequestParam(required = false) String mainOfcRemarks) {

        log.info("Toggling verify status for collection handover with id: {} to {}", id, verifiedRcvd);
        collectionHandoverService.toggleVerifyStatus(id, verifiedRcvd, mainOfcRemarks);
        log.info("Successfully updated verify status for collection handover with id: {}", id);
        return ApiResponse.success("Verify status updated successfully");
    }

    /**
     * Create Pageable from request parameters
     */
    private Pageable createPageable(int page, int size, String sort) {
        Sort sortObj = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            String[] sortParts = sort.split(",");
            if (sortParts.length == 2) {
                Sort.Direction direction = sortParts[1].trim().equalsIgnoreCase("desc")
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;
                sortObj = Sort.by(direction, sortParts[0].trim());
            } else if (sortParts.length == 1) {
                sortObj = Sort.by(Sort.Direction.ASC, sortParts[0].trim());
            }
        }
        return PageRequest.of(page, size, sortObj);
    }
}

