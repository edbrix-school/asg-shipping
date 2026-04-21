package com.asg.shipping.contractsandagreements.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementHdrDto;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementRenewalDto;
import com.asg.shipping.contractsandagreements.dto.ContractRenewalRequest;
import com.asg.shipping.contractsandagreements.dto.ContractRenewalResponse;
import com.asg.shipping.contractsandagreements.service.ContractsAndAgreementsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create Contracts and Agreements",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Sample Request",
                                    value = """
                                            {
                                              "transactionDate": "2026-02-05T16:11:13",
                                              "agreementName": "test agreement name",
                                              "agreementType": "SERVICE",
                                              "agreementCategory": "COMMERCIAL",
                                              "agreementDescription": "Dummy agreement for testing",
                                              "agreementStatus": "ACTIVE",
                                              "agreementSource": "MANUAL",
                                              "agreementCompanyPoid": 2001,
                                              "partyType": "CUSTOMER",
                                              "partyPoid": 5925,
                                              "newParty": "N",
                                              "newPartyName": null,
                                              "linePoid": 4001,
                                              "partyContactPerson": "Jo",
                                              "partyContactEmail": "jo@test.com",
                                              "partyContactPhone": "1234567810",
                                              "partyAddress": "IN",
                                              "referenceDate": "2026-02-05T16:11:13",
                                              "effectiveDate": "2026-02-05T16:11:13",
                                              "expiryDate": "2027-02-05T16:11:13",
                                              "noticePeriodDays": 30,
                                              "renewalType": "AUTO",
                                              "renewalCycle": "YEARLY",
                                              "renewalDueDate": "2027-01-05T16:11:13",
                                              "lastRenewalDate": null,
                                              "totalContractValue": 1000000,
                                              "annualValue": 100000,
                                              "paymentTerms": "NET 30",
                                              "paymentFrequency": "MONTHLY",
                                              "terminated": "N",
                                              "terminationDate": null,
                                              "terminationReason": null,
                                              "agreementCaption": "Service Agreement Caption",
                                              "agreementContent": "Dummy agreement content",
                                              "signatory": "Authorized Signatory",
                                              "agreementContentDetails": [
                                                {
                                                  "departmentPoid": 25,
                                                  "handledUserPoid": 3005,
                                                  "periodFrom": "2026-01-01T23:59:59",
                                                  "periodTo": "2026-12-31T23:59:59",
                                                  "remarks": "Handled by operations team"
                                                },
                                                {
                                                  "departmentPoid": 30,
                                                  "handledUserPoid": 3010,
                                                  "periodFrom": "2026-02-01T23:59:59",
                                                  "periodTo": "2026-12-31T23:59:59",
                                                  "remarks": "Secondary department support"
                                                }
                                              ],
                                              "renewalDetails": [
                                                {
                                                  "effectiveStartDate": "2026-01-01T00:00:00",
                                                  "expiryDate": "2026-12-31T23:59:59",
                                                  "renewalDate": "2026-12-01T00:00:00"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
    )
    public ResponseEntity<?> create(
            @Valid @RequestBody AdminContractsAgreementHdrDto createDTO
    ) {
        AdminContractsAgreementHdrDto response = service.createContractsAndAgreements(createDTO);
        return success("Contracts and Agreements created successfully", response);
    }


    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    @Operation(
            summary = "Get Contracts and Agreements by Transaction POID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Contracts and Agreements retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AdminContractsAgreementHdrDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Contracts and Agreements not found"
                    )
            }
    )
    public ResponseEntity<?> getById(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid
    ) {

        AdminContractsAgreementHdrDto response =
                service.getContractsAndAgreementsById(transactionPoid);

        loggingService.createLogSummaryEntry(
                LogDetailsEnum.VIEWED,
                UserContext.getDocumentId(),
                transactionPoid.toString()
        );

        return success("Contracts and Agreements retrieved successfully", response);
    }


    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    public ResponseEntity<?> delete(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long transactionPoid, @Valid @RequestBody DeleteReasonDto deleteReasonDto
    ) {
        service.deleteContractsAndAgreements(transactionPoid, deleteReasonDto);
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
        Map<String, Object> response = service.list(filters, pageable);
        return success("Contracts and Agreements list retrieved successfully", response);

    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    @Operation(
            summary = "Update Contracts and Agreements",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Sample Update Request",
                                    value = """
                                            {
                                              "transactionDate": "2026-02-05T16:11:13",
                                              "agreementName": "Service Agreement supplier test with child 3",
                                              "agreementType": "SERVICE",
                                              "agreementCategory": "COMMERCIAL",
                                              "agreementDescription": "Dummy agreement for 2  for testing testing",
                                              "agreementStatus": "ACTIVE",
                                              "agreementSource": "MANUAL",
                                              "agreementCompanyPoid": 2001,
                                              "partyType": "CUSTOMER",
                                              "partyPoid": 5925,
                                              "newParty": "N",
                                              "newPartyName": null,
                                              "linePoid": 4001,
                                              "partyContactPerson": "Jo",
                                              "partyContactEmail": "jo@test.com",
                                              "partyContactPhone": "1234567810",
                                              "partyAddress": "IN",
                                              "referenceDate": "2026-02-05T16:11:13",
                                              "effectiveDate": "2026-02-05T16:11:13",
                                              "expiryDate": "2027-02-05T16:11:13",
                                              "noticePeriodDays": 30,
                                              "renewalType": "AUTO",
                                              "renewalCycle": "YEARLY",
                                              "renewalDueDate": "2027-01-05T16:11:13",
                                              "lastRenewalDate": null,
                                              "totalContractValue": 1000000,
                                              "annualValue": 100000,
                                              "paymentTerms": "NET 30 test",
                                              "paymentFrequency": "MONTHLY",
                                              "terminated": "N",
                                              "terminationDate": null,
                                              "terminationReason": null,
                                              "agreementCaption": "Service Agreement Caption",
                                              "agreementContent": "Dummy agreement content",
                                              "signatory": "Authorized Signatory",
                                              "agreementContentDetails": [
                                                {
                                                  "detRowId": 3,
                                                  "departmentPoid": 25,
                                                  "handledUserPoid": 3005,
                                                  "periodFrom": "2026-01-01T23:59:59",
                                                  "periodTo": "2026-12-31T23:59:59",
                                                  "remarks": "Handled by operations team create",
                                                  "actionType": "isUpdated"
                                                }
                                              ],
                                              "renewalDetails": [
                                                {
                                                  "detRowId": 1,
                                                  "effectiveStartDate": "2026-01-01T00:00:00",
                                                  "expiryDate": "2026-12-31T23:59:59",
                                                  "renewalDate": "2026-12-05T00:00:00",
                                                  "actionType": "isUpdated"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
    )
    public ResponseEntity<?> update(
            @Parameter(description = "Transaction POID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody AdminContractsAgreementHdrDto updateDTO
    ) {

        AdminContractsAgreementHdrDto response =
                service.updateContractsAndAgreements(id, updateDTO);

        return success("Contracts and Agreements updated successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/renew")
    @Operation(
            summary = "Renew Contracts and Agreements",
            description = "Renew an existing contract by providing new expiry and renewal dates."
    )
    public ResponseEntity<?> renew(
            @Valid @RequestBody ContractRenewalRequest renewalRequest
    ) {
        ContractRenewalResponse response = service.renewContractsAndAgreements(renewalRequest);
        return success("Contracts and Agreements renewed successfully", response);
    }

}

