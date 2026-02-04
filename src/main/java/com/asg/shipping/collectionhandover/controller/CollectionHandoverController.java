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

import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.security.util.UserContext.getGroupPoid;
import static com.asg.common.lib.security.util.UserContext.getUserPoid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/collection-handover-shipping")
@Slf4j
@Tag(name = "Collection Handover Management", description = "APIs for managing collection handover (day end close)")
public class CollectionHandoverController {

    private static final String DOC_ID = "300-106";
    private final CollectionHandoverService collectionHandoverService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/search")
    @Operation(summary = "Search collection handovers", description = "Retrieve paginated list of collection handovers")
    public ResponseEntity<?> searchCollectionHandovers(
            @RequestBody(required = false) com.asg.common.lib.dto.FilterRequestDto request,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction", example = "docRef,asc")
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        log.info("Searching collection handovers with page: {}, size: {}, sort: {}, startDate: {}, endDate: {}", page, size, sort, startDate, endDate);
        
        try {
            if((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
                return ApiResponse.badRequest("Both startDate and endDate should be specified or both dates should be empty.");
            }
            
            Pageable pageable = createPageable(page, size, sort);
            Map<String, Object> result = collectionHandoverService.searchCollectionHandovers(DOC_ID, request, pageable, startDate, endDate);
            return ApiResponse.success("Collection handovers retrieved successfully", result);
        } catch (Exception e) {
            return ApiResponse.internalServerError("Unable to fetch collection handovers: " + e.getMessage());
        }
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    @Operation(summary = "Get collection handover details")
    public ResponseEntity<?> getCollectionHandover(@PathVariable Long id) {
        log.info("Getting collection handover with id: {}", id);
        CollectionHandoverDto handover = collectionHandoverService.getCollectionHandover(id);
        return ApiResponse.success("Collection handover retrieved successfully", handover);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(summary = "Create new collection handover")
    public ResponseEntity<?> createCollectionHandover(@Valid @RequestBody CollectionHandoverCreateDTO dto) {
        log.info("Creating collection handover");
        CollectionHandoverDto created = collectionHandoverService.createCollectionHandover(dto, getGroupPoid(), getUserPoid());
        return ApiResponse.success("Collection handover created successfully", created);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    @Operation(summary = "Update collection handover")
    public ResponseEntity<?> updateCollectionHandover(@PathVariable Long id, @Valid @RequestBody CollectionHandoverUpdateDTO dto) {
        log.info("Updating collection handover with id: {}", id);
        CollectionHandoverDto updated = collectionHandoverService.updateCollectionHandover(id, dto, getGroupPoid(), getUserPoid());
        return ApiResponse.success("Collection handover updated successfully", updated);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete collection handover")
    public ResponseEntity<?> deleteCollectionHandover(@PathVariable Long id) {
        log.info("Deleting collection handover with id: {}", id);
        collectionHandoverService.deleteCollectionHandover(id);
        return ApiResponse.success("Collection handover deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}/verify")
    @Operation(summary = "Toggle verify/receive status")
    public ResponseEntity<?> toggleVerifyStatus(
            @PathVariable Long id,
            @RequestParam String verifiedRcvd,
            @RequestParam(required = false) String mainOfcRemarks) {
        log.info("Toggling verify status for collection handover with id: {} to {}", id, verifiedRcvd);
        collectionHandoverService.toggleVerifyStatus(id, verifiedRcvd, mainOfcRemarks);
        return ApiResponse.success("Verify status updated successfully");
    }

    private Pageable createPageable(int page, int size, String sort) {
        if (sort != null && !sort.isEmpty()) {
            String[] sortParts = sort.split(",");
            String field = sortParts[0];
            Sort.Direction direction = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1]) 
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of(page, size, Sort.by(direction, field));
        }
        return PageRequest.of(page, size);
    }
}