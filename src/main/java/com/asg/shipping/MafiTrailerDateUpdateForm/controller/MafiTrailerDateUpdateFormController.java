package com.asg.shipping.MafiTrailerDateUpdateForm.controller;

import static com.asg.common.lib.dto.response.ApiResponse.success;

import java.util.Map;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.MafiTrailerDateUpdateFormRequest;
import com.asg.shipping.MafiTrailerDateUpdateForm.dto.MafiTrailerDateUpdateFormResponse;
import com.asg.shipping.MafiTrailerDateUpdateForm.service.MafiTrailerDateUpdateFormService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1/mafi-trailer-date-update")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class MafiTrailerDateUpdateFormController {

	private final MafiTrailerDateUpdateFormService service;
	private final LoggingService loggingService;

	@Operation(summary = "Get all Mafi Trailer date update", description = "Fetches all the records for the Mafi trailer date update", responses = {
			@ApiResponse(responseCode = "200", description = "Mafi trailers fetched successfully."),
			@ApiResponse(responseCode = "401", description = "Unauthorized") }, security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping("/list")
	public ResponseEntity<?> getAll(@ParameterObject Pageable pageable,
			@RequestBody(required = false) FilterRequestDto filters) {
		Map<String, Object> response = service.getAll(UserContext.getDocumentId(), filters, pageable);
		return success("Mafi trailers fetched successfully.", response);
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@Operation(summary = "Get Mafi Trailer date update form by POID", description = "Fetches a specific Mafi Trailer date update by port POID", responses = {
			@ApiResponse(responseCode = "200", description = "Mafi Trailer date update form fetched successfully"),
			@ApiResponse(responseCode = "404", description = "Port not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") }, security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/{transactionPoid}")
	public ResponseEntity<?> getById(
			@Parameter(description = "Transaction POID", required = true, example = "1001") @PathVariable("transactionPoid") Long transactionPoid,

			@Parameter(description = "Group POID", required = true, example = "1001") @RequestParam("groupPoid") Long groupPoid,

			@Parameter(description = "Company POID", required = true, example = "5001") @RequestParam("companyPoid") Long companyPoid) {
		MafiTrailerDateUpdateFormResponse response = service.getById(transactionPoid, groupPoid, companyPoid);
		loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(),transactionPoid.toString());
		return success("Mafi Trailer date update fetched successfully", response);
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@Operation(summary = "Update an existing Mafi Trailer date update form", description = "Updates the Port Master details for the given port POID", responses = {
			@ApiResponse(responseCode = "200", description = "Mafi Trailer date update form updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MafiTrailerDateUpdateFormResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid input"),
			@ApiResponse(responseCode = "404", description = "Port not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") }, security = @SecurityRequirement(name = "bearerAuth"))
	@PutMapping("/{transactionPoid}")
	public ResponseEntity<?> update(
			@Parameter(description = "Transaction POID", required = true, example = "1001") @PathVariable("transactionPoid") Long transactionPoid,

			@Parameter(description = "Group POID", required = true, example = "1001") @RequestParam("groupPoid") Long groupPoid,

			@Parameter(description = "Company POID", required = true, example = "5001") @RequestParam("companyPoid") Long companyPoid,

			@Parameter(description = "User ID performing the action", required = true, example = "admin") @RequestParam("userPoid") String userPoid,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated Port details", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = MafiTrailerDateUpdateFormRequest.class))) @Valid @RequestBody MafiTrailerDateUpdateFormRequest request) {
		service.update(transactionPoid, request, groupPoid, companyPoid, userPoid);
		return success("Mafi Trailer date update form updated successfully");
	}
}
