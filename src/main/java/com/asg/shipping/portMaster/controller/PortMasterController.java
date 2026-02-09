package com.asg.shipping.portMaster.controller;

import static com.asg.common.lib.dto.response.ApiResponse.success;

import java.util.Map;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.asg.shipping.portMaster.dto.PortMasterRequest;
import com.asg.shipping.portMaster.dto.PortMasterResponse;
import com.asg.shipping.portMaster.service.PortMasterService;

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
@RequestMapping("/v1/port-master")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class PortMasterController {

	private final PortMasterService service;
	private final LoggingService loggingService;

	@AllowedAction(UserRolesRightsEnum.CREATE)
	@Operation(summary = "Create a new Port", description = "Creates a new Port Master record for the given group", responses = {
			@ApiResponse(responseCode = "200", description = "Port created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortMasterResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid input"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") }, security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping
	public ResponseEntity<?> create(
			@Parameter(description = "Group POID", required = true, example = "1001") @RequestParam Long groupPoid,

			@Parameter(description = "User ID performing the action", required = true, example = "admin") @RequestParam String userPoid,

			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Port details to be created", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortMasterRequest.class))) @Valid @RequestBody PortMasterRequest request) {
		Map<String,Object >response=service.createPort(groupPoid, request, userPoid);
		return success("Port created successfully",response);
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@Operation(summary = "Update an existing Port", description = "Updates the Port Master details for the given port POID", responses = {
			@ApiResponse(responseCode = "200", description = "Port updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortMasterResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid input"),
			@ApiResponse(responseCode = "404", description = "Port not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") }, security = @SecurityRequirement(name = "bearerAuth"))
	@PutMapping("/{portPoid}")
	public ResponseEntity<?> update(
			@Parameter(description = "Group POID", required = true, example = "1001") @RequestParam Long groupPoid,

			@Parameter(description = "User ID performing the action", required = true, example = "admin") @RequestParam String userPoid,

			@Parameter(description = "Port POID to be updated", required = true, example = "5001") @PathVariable Long portPoid,

			@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated Port details", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortMasterRequest.class))) @Valid @RequestBody PortMasterRequest request) {
		PortMasterResponse response = service.updatePort(groupPoid, portPoid, request, userPoid);
		return success("Port updated successfully", response);
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@Operation(summary = "Get all Ports", description = "Fetches all Port Master records for the given group", responses = {
			@ApiResponse(responseCode = "200", description = "Ports fetched successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortMasterResponse.class))),
			@ApiResponse(responseCode = "401", description = "Unauthorized") }, security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping("/list")
	public ResponseEntity<?> getAll(@ParameterObject Pageable pageable,
			@RequestBody(required = false) FilterRequestDto filters) {
		Map<String, Object> response = service.getAllPorts(UserContext.getDocumentId(), filters, pageable);
		return success("Ports fetched successfully", response);
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@Operation(summary = "Get Port by POID", description = "Fetches a specific Port Master by port POID", responses = {
			@ApiResponse(responseCode = "200", description = "Port fetched successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortMasterResponse.class))),
			@ApiResponse(responseCode = "404", description = "Port not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") }, security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/{portPoid}")
	public ResponseEntity<?> getById(
			@Parameter(description = "Group POID", required = true, example = "1001") @RequestParam Long groupPoid,

			@Parameter(description = "Port POID", required = true, example = "5001") @PathVariable Long portPoid) {
		PortMasterResponse response = service.getPortById(groupPoid, portPoid);
		loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(),portPoid.toString());
		return success("Port fetched successfully", response);
	}

	@AllowedAction(UserRolesRightsEnum.DELETE)
	@Operation(summary = "Delete a Port", description = "Soft deletes a Port Master record", responses = {
			@ApiResponse(responseCode = "200", description = "Port deleted successfully"),
			@ApiResponse(responseCode = "404", description = "Port not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized") }, security = @SecurityRequirement(name = "bearerAuth"))
	@DeleteMapping("/{portPoid}")
	public ResponseEntity<?> delete(
			@Parameter(description = "Group POID", required = true, example = "1001") @RequestParam Long groupPoid,

			@Parameter(description = "User ID performing the action", required = true, example = "admin") @RequestParam String userPoid,

			@Parameter(description = "Port POID to be deleted", required = true, example = "5001") @PathVariable Long portPoid) {
		service.deletePort(groupPoid, portPoid, userPoid);
		loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, UserContext.getDocumentId(),portPoid.toString());
		return success("Port deleted successfully");
	}

}
