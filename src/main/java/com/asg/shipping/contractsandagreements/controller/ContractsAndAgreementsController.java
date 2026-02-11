package com.asg.shipping.contractsandagreements.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementHdrDto;
import com.asg.shipping.contractsandagreements.service.ContractsAndAgreementsService;
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
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@Slf4j
@RestController
@RequestMapping("v1/contracts-and-agreements")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "contracts-and-agreements")
public class ContractsAndAgreementsController {

    private final ContractsAndAgreementsService service;
    private final LoggingService loggingService;

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody AdminContractsAgreementHdrDto createDTO
    ) {
        AdminContractsAgreementHdrDto response = service.createContractsAndAgreements(createDTO);
        return success("Contracts and Agreements created successfully", response);

    }


    @GetMapping("/{transactionPoid}")
    public ResponseEntity<?> getById(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid
    ) {

        AdminContractsAgreementHdrDto response = service.getContractsAndAgreementsById(transactionPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        return success("Contracts and Agreements retrieved successfully", response);
    }


    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> delete(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid, @Valid @RequestBody DeleteReasonDto deleteReasonDto
            ) {
        service.deleteContractsAndAgreements(transactionPoid,deleteReasonDto);
        return success("Contracts and Agreements deleted successfully", null);

    }


    @AllowedAction(UserRolesRightsEnum.VIEW)
    @Operation(
            summary = "List Contracts and Agreements",
            description = """
                    Fetch Contracts and Agreements records using filters and pagination.
               """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    examples = @ExampleObject(
                            name = "Contracts and Agreements Filters",
                            value = """
                                    {
                                      "operator": "AND",
                                      "isDeleted": "N",
                                      "filters": [
                                        {
                                          "searchField": "DESCRIPTION",
                                          "searchValue": "Auto Charge"
                                        },
                                        {
                                          "searchField": "CUSTOMER_POID",
                                          "searchValue": "1001"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contracts and Agreements list retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/list")
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters
    ) {
            log.info("for list api -------------->");
            Map<String, Object> response = service.list(filters, pageable);
            return success("Contracts and Agreements list retrieved successfully", response);

    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody AdminContractsAgreementHdrDto updateDTO
    ) {

        AdminContractsAgreementHdrDto response = service.updateContractsAndAgreements(id, updateDTO);
        return success("Contracts and Agreements updated successfully", response);

    }

}

