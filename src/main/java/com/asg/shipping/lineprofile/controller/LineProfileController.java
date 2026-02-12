package com.asg.shipping.lineprofile.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.lineprofile.dto.LineProfileAgreementDetailsResponse;
import com.asg.shipping.lineprofile.dto.LineProfileLineDetailsResponse;
import com.asg.shipping.lineprofile.dto.LineProfileRequest;
import com.asg.shipping.lineprofile.dto.LineProfileResponse;
import com.asg.shipping.lineprofile.service.LineProfileService;
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
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.shipping.common.ApiResponse.internalServerError;
import static com.asg.shipping.common.ApiResponse.notFound;
import static com.asg.shipping.common.ApiResponse.success;

@RestController
@RequestMapping("/v1/line-profile")
@RequiredArgsConstructor
@Validated
@Slf4j
public class LineProfileController {

    private final LineProfileService service;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "List Line Profiles with Search and Sort",
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
            @RequestHeader(value = "X-Document-Id", required = false) String docId) {
        try {
            String resolvedDocId = resolveDocId(docId);
            log.info("List LineProfile request | page={}, size={}, docId={}",
                    pageable.getPageNumber(), pageable.getPageSize(), resolvedDocId);
            Map<String, Object> result = service.listLineProfiles(resolvedDocId, filters, pageable);
            return success("Line profile list fetched successfully", result);
        } catch (Exception e) {
            return internalServerError("Unable to fetch line profiles: " + e.getMessage());
        }
    }

    @GetMapping(value = "/{lineProfilePoid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Get Line Profile by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> get(@PathVariable @NotNull @Positive Long lineProfilePoid,
                                 @RequestHeader(value = "X-Group-Poid", required = false) Long groupPoid) {
        Long resolvedGroupPoid = resolveGroupPoid(groupPoid);
        log.info("Get LineProfile request | lineProfilePoid={}, groupPoid={}",
                lineProfilePoid, resolvedGroupPoid);
        LineProfileResponse resp = service.getById(lineProfilePoid, resolvedGroupPoid);
        return success("Line profile fetched successfully", resp);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @Operation(summary = "Create Line Profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> create(@Valid @RequestBody LineProfileRequest request,
                                    @RequestHeader(value = "X-Group-Poid", required = false) Long groupPoid,
                                    @RequestHeader(value = "X-User-Id", required = false) String userId,
                                    @RequestHeader(value = "X-Document-Id", required = false) String docId) {
        Long resolvedGroupPoid = resolveGroupPoid(groupPoid);
        String resolvedUserId = resolveUserId(userId);
        String resolvedDocId = resolveDocId(docId);
        log.info("Create LineProfile request | groupPoid={}, userId={}, docId={}",
                resolvedGroupPoid, resolvedUserId, resolvedDocId);
        LineProfileResponse resp = service.create(request, resolvedGroupPoid, resolvedUserId, resolvedDocId);
        return success("Line profile created successfully", resp);
    }

    @PutMapping(value = "/{lineProfilePoid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @Operation(summary = "Update Line Profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> update(@PathVariable @NotNull @Positive Long lineProfilePoid,
                                    @Valid @RequestBody LineProfileRequest request,
                                    @RequestHeader(value = "X-Group-Poid", required = false) Long groupPoid,
                                    @RequestHeader(value = "X-User-Id", required = false) String userId,
                                    @RequestHeader(value = "X-Document-Id", required = false) String docId) {
        Long resolvedGroupPoid = resolveGroupPoid(groupPoid);
        String resolvedUserId = resolveUserId(userId);
        String resolvedDocId = resolveDocId(docId);
        log.info("Update LineProfile request | lineProfilePoid={}, groupPoid={}, userId={}, docId={}",
                lineProfilePoid, resolvedGroupPoid, resolvedUserId, resolvedDocId);
        LineProfileResponse resp = service.update(lineProfilePoid, request, resolvedGroupPoid, resolvedUserId, resolvedDocId);
        return success("Line profile updated successfully", resp);
    }

    @DeleteMapping(value = "/{lineProfilePoid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @Operation(summary = "Delete Line Profile (Soft delete)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> delete(@PathVariable @NotNull @Positive Long lineProfilePoid,
                                    @RequestHeader(value = "X-Group-Poid", required = false) Long groupPoid,
                                    @RequestHeader(value = "X-User-Id", required = false) String userId) {
        Long resolvedGroupPoid = resolveGroupPoid(groupPoid);
        String resolvedUserId = resolveUserId(userId);
        log.info("Delete LineProfile request | lineProfilePoid={}, groupPoid={}, userId={}",
                lineProfilePoid, resolvedGroupPoid, resolvedUserId);
        service.delete(lineProfilePoid, resolvedGroupPoid, resolvedUserId);
        return success("Line profile deleted successfully");
    }

    @GetMapping(value = "/lines/{linePoid}/details", produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Fetch Line Details using PROC_SH_LINE_PROFILE", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> fetchLineDetails(@PathVariable @NotNull @Positive Long linePoid,
                                              @RequestHeader(value = "X-Group-Poid", required = false) Long groupPoid,
                                              @RequestHeader(value = "X-Company-Poid", required = false) Long companyPoid,
                                              @RequestHeader(value = "X-User-Poid", required = false) Long userPoid) {
        Long resolvedGroupPoid = resolveGroupPoid(groupPoid);
        Long resolvedCompanyPoid = resolveCompanyPoid(companyPoid);
        Long resolvedUserPoid = resolveUserPoid(userPoid);
        LineProfileLineDetailsResponse response =
                service.fetchLineDetails(linePoid, resolvedGroupPoid, resolvedCompanyPoid, resolvedUserPoid);
        if (response == null) {
            return notFound("Line details not found");
        }
        return success("Line details fetched successfully", response);
    }

    @GetMapping(value = "/agreements/{transactionPoid}/details", produces = MediaType.APPLICATION_JSON_VALUE)
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(summary = "Fetch Agreement Details using PROC_SH_CONTRACTS_AGREEMENT", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> fetchAgreementDetails(@PathVariable @NotNull @Positive Long transactionPoid,
                                                   @RequestHeader(value = "X-Group-Poid", required = false) Long groupPoid,
                                                   @RequestHeader(value = "X-Company-Poid", required = false) Long companyPoid,
                                                   @RequestHeader(value = "X-User-Poid", required = false) Long userPoid) {
        Long resolvedGroupPoid = resolveGroupPoid(groupPoid);
        Long resolvedCompanyPoid = resolveCompanyPoid(companyPoid);
        Long resolvedUserPoid = resolveUserPoid(userPoid);
        LineProfileAgreementDetailsResponse response =
                service.fetchAgreementDetails(transactionPoid, resolvedGroupPoid, resolvedCompanyPoid, resolvedUserPoid);
        if (response == null) {
            return notFound("Agreement details not found");
        }
        return success("Agreement details fetched successfully", response);
    }

    private Long resolveGroupPoid(Long groupPoid) {
        return groupPoid != null ? groupPoid : UserContext.getGroupPoid();
    }

    private String resolveUserId(String userId) {
        return StringUtils.isNotBlank(userId) ? userId : UserContext.getUserId();
    }

    private Long resolveUserPoid(Long userPoid) {
        return userPoid != null ? userPoid : UserContext.getUserPoid();
    }

    private Long resolveCompanyPoid(Long companyPoid) {
        return companyPoid != null ? companyPoid : UserContext.getCompanyPoid();
    }

    private String resolveDocId(String docId) {
        return StringUtils.isNotBlank(docId) ? docId : UserContext.getDocumentId();
    }
}

