package com.asg.shipping.vesselvoyagecreation.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.vesselvoyagecreation.dto.CurrencyUpdateRequest;
import com.asg.shipping.vesselvoyagecreation.dto.TranshipmentTransferRequest;
import com.asg.shipping.vesselvoyagecreation.dto.TranshipmentUpdateRequest;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageBlFilter;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageBlTab;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageUpsertRequest;
import com.asg.shipping.vesselvoyagecreation.service.VesselVoyageService;
import com.asg.shipping.vesselvoyagecreation.util.FreightCargo;
import com.asg.shipping.vesselvoyagecreation.util.ImportExport;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/vessel-voyage-creation-line-edi")
@Validated
@Slf4j
public class VesselVoyageController {

	private final VesselVoyageService vesselVoyageService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    @Operation(summary = "List Vessel Voyages (List of Records)", description = "Uses DocumentSearchService (doc_master) and applies user line access via PROC_GLOB_USER_LINE_LISTING")
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filters,
            @RequestHeader(value = "X-Document-Id", required = false) String docId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        log.info("Action={} | List voyages | page={} size={} docId={} startDate={} endDate={} groupPoid={} companyPoid={} userPoid={}",
                UserContext.getActionRequested(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                docId != null ? docId : UserContext.getDocumentId(),
                startDate,
                endDate,
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid());
        
        try {
			
            if((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
                return ApiResponse.badRequest("Both startDate and endDate should be specified or both dates should be empty.");
            }
            
            return ApiResponse.success("Vessel voyages fetched successfully", vesselVoyageService.listVoyages(filters, pageable, docId, startDate, endDate));
        } catch (Exception e) {
            return ApiResponse.internalServerError("Unable to fetch vessel voyages: " + e.getMessage());
        }
    }

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@GetMapping("/{voyagePoid}")
	public ResponseEntity<?> get(@PathVariable Long voyagePoid) {
		log.info("Action={} | Get voyage | voyagePoid={} groupPoid={} companyPoid={} userPoid={}",
				UserContext.getActionRequested(), voyagePoid, UserContext.getGroupPoid(), UserContext.getCompanyPoid(),
				UserContext.getUserPoid());
		return ApiResponse.success("Vessel voyage fetched successfully", vesselVoyageService.getVoyage(voyagePoid));
	}

	@AllowedAction(UserRolesRightsEnum.CREATE)
	@PostMapping
	public ResponseEntity<?> create(@Valid @RequestBody VoyageUpsertRequest request) {
		log.info(
				"Action={} | Create voyage | voyageNo={} linePoid={} vesselPoid={} groupPoid={} companyPoid={} userId={}",
				UserContext.getActionRequested(), request.getVoyageNo(), request.getLinePoid(), request.getVesselPoid(),
				UserContext.getGroupPoid(), UserContext.getCompanyPoid(), UserContext.getUserId());
		return ApiResponse.success("Vessel voyage created successfully", vesselVoyageService.createVoyage(request));
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PutMapping("/{voyagePoid}")
	public ResponseEntity<?> update(@PathVariable Long voyagePoid, @Valid @RequestBody VoyageUpsertRequest request) {
		log.info("Action={} | Update voyage | voyagePoid={} voyageNo={} linePoid={} vesselPoid={} userId={}",
				UserContext.getActionRequested(), voyagePoid, request.getVoyageNo(), request.getLinePoid(),
				request.getVesselPoid(), UserContext.getUserId());
		return ApiResponse.success("Vessel voyage updated successfully",
				vesselVoyageService.updateVoyage(voyagePoid, request));
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@GetMapping("/{voyagePoid}/bls")
	public ResponseEntity<?> listBls(@PathVariable Long voyagePoid, @RequestParam VoyageBlTab tab,
			@RequestParam(required = false, defaultValue = "ALL") VoyageBlFilter filter,
			@ParameterObject Pageable pageable) {
		log.info("Action={} | List BLs | voyagePoid={} tab={} filter={} page={} size={}",
				UserContext.getActionRequested(), voyagePoid, tab, filter, pageable.getPageNumber(),
				pageable.getPageSize());
		return ApiResponse.success("BLs fetched successfully",
				vesselVoyageService.listBls(voyagePoid, tab, filter, pageable));
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@GetMapping("/{voyagePoid}/edi/errors")
	public ResponseEntity<?> ediErrors(@PathVariable Long voyagePoid) {
		log.info("Action={} | Get EDI errors | voyagePoid={}", UserContext.getActionRequested(), voyagePoid);
		return ApiResponse.success("EDI errors fetched successfully", vesselVoyageService.getEdiErrors(voyagePoid));
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PostMapping("/{voyagePoid}/edi/reprocess")
	public ResponseEntity<?> ediReprocess(@PathVariable Long voyagePoid) {
		log.info("Action={} | Reprocess EDI | voyagePoid={}", UserContext.getActionRequested(), voyagePoid);
		return ApiResponse.success("EDI reprocess completed", vesselVoyageService.reprocessEdi(voyagePoid));
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PostMapping(value = "/{voyagePoid}/edi/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> ediUpload(@PathVariable Long voyagePoid, @RequestPart("file") MultipartFile file) {
		log.info("Action={} | Upload EDI | voyagePoid={} fileName={} size={}", UserContext.getActionRequested(),
				voyagePoid, file != null ? file.getOriginalFilename() : null, file != null ? file.getSize() : 0);
		return ApiResponse.success("EDI upload processed", vesselVoyageService.uploadAndProcessEdi(voyagePoid, file));
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@GetMapping("/{voyagePoid}/transhipments")
	public ResponseEntity<?> listTranshipments(@PathVariable Long voyagePoid) {
		log.info("Action={} | List transhipments | voyagePoid={}", UserContext.getActionRequested(), voyagePoid);
		return ApiResponse.success("Transhipments fetched successfully",
				vesselVoyageService.listTranshipments(voyagePoid));
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PutMapping("/{voyagePoid}/transhipments")
	public ResponseEntity<?> updateTranshipments(@PathVariable Long voyagePoid,
			@Valid @RequestBody TranshipmentUpdateRequest request) {
		log.info("Action={} | Update transhipments | voyagePoid={} items={}", UserContext.getActionRequested(),
				voyagePoid, request.getItems().size());
		return ApiResponse.success("Transhipments updated successfully",
				vesselVoyageService.updateTranshipments(voyagePoid, request));
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PostMapping("/{voyagePoid}/transhipments/transfer")
	public ResponseEntity<?> transferTranshipments(@PathVariable Long voyagePoid,
			@Valid @RequestBody TranshipmentTransferRequest request) {
		log.info("Action={} | Transfer transhipments | voyagePoid={} targetVoyagePoid={} detRowIds={}",
				UserContext.getActionRequested(), voyagePoid, request.getTargetVoyagePoid(), request.getDetRowIds());
		return ApiResponse.success("Transhipments transfer completed",
				vesselVoyageService.transferTranshipments(voyagePoid, request));
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PostMapping("/{voyagePoid}/transhipments/import-hnjn")
	public ResponseEntity<?> importHnjn(@PathVariable Long voyagePoid) {
		log.info("Action={} | Import HNJN transhipments | voyagePoid={}", UserContext.getActionRequested(), voyagePoid);
		return ApiResponse.success("HNJN transhipments import completed",
				vesselVoyageService.importHnjnTranshipments(voyagePoid));
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@GetMapping("/{voyagePoid}/currency")
	public ResponseEntity<?> listCurrency(@PathVariable Long voyagePoid) {
		log.info("Action={} | List currency | voyagePoid={}", UserContext.getActionRequested(), voyagePoid);
		return ApiResponse.success("Voyage currencies fetched successfully",
				vesselVoyageService.listVoyageCurrencies(voyagePoid));
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PostMapping("/{voyagePoid}/currency/update")
	public ResponseEntity<?> updateCurrency(@PathVariable Long voyagePoid,
			@Valid @RequestBody CurrencyUpdateRequest request) {
		log.info("Action={} | Update currency | voyagePoid={} items={}", UserContext.getActionRequested(), voyagePoid,
				request.getItems().size());
		return ApiResponse.success("Currency update completed",
				vesselVoyageService.updateCurrencyRates(voyagePoid, request));
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PostMapping("/{voyagePoid}/can/resend")
	public ResponseEntity<?> resendCan(@PathVariable Long voyagePoid,
			@RequestParam(value = "blTransactionPoid", required = false) Long blTransactionPoid) {
		log.info("Action={} | Resend CAN | voyagePoid={} blTransactionPoid={}", UserContext.getActionRequested(),
				voyagePoid, blTransactionPoid);
		return ApiResponse.success("CAN resend completed",
				vesselVoyageService.resendCan(voyagePoid, blTransactionPoid));
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PostMapping("/{voyagePoid}/manifest/empty")
	public ResponseEntity<?> emptyManifest(@PathVariable Long voyagePoid) {
		log.info("Action={} | Create empty manifest | voyagePoid={}", UserContext.getActionRequested(), voyagePoid);
		return ApiResponse.success("Empty manifest created", vesselVoyageService.createEmptyManifest(voyagePoid));
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PostMapping("/{voyagePoid}/tdr")
	public ResponseEntity<?> createTdr(@PathVariable Long voyagePoid,
			@RequestParam(defaultValue = "true") boolean createEmptyManifestFirst) {
		log.info("Action={} | Create TDR | voyagePoid={} createEmptyManifestFirst={}", UserContext.getActionRequested(),
				voyagePoid, createEmptyManifestFirst);
		return ApiResponse.success("TDR created", vesselVoyageService.createTdr(voyagePoid, createEmptyManifestFirst));
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PostMapping("/{voyagePoid}/edi/export/cosco")
	public ResponseEntity<?> exportCosco(@PathVariable Long voyagePoid,
			@RequestParam(value = "blPoid", required = false) Long blPoid) {
		log.info("Action={} | Export COSCO EDI | voyagePoid={} blPoid={}", UserContext.getActionRequested(), voyagePoid,
				blPoid);
		return ApiResponse.success("COSCO export triggered", vesselVoyageService.exportEdiCosco(voyagePoid, blPoid));
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PostMapping("/{voyagePoid}/imports/general-cargo")
	public ResponseEntity<?> importGeneralCargo(@PathVariable Long voyagePoid) {
		log.info("Action={} | Import general cargo | voyagePoid={}", UserContext.getActionRequested(), voyagePoid);
		return ApiResponse.success("General cargo import completed",
				vesselVoyageService.importGeneralCargo(voyagePoid));
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PostMapping("/{voyagePoid}/edi/moves-load-discharge")
	public ResponseEntity<?> ediMovesLoadDischarge(@PathVariable Long voyagePoid,
			@RequestParam String ediDateValue) {
		log.info("Action={} | EDI moves load/discharge | voyagePoid={} ediDateValue={}",
				UserContext.getActionRequested(), voyagePoid, ediDateValue);
		vesselVoyageService.ediMovesLoadDischarge(voyagePoid, ediDateValue);
		return ApiResponse.success("EDI moves load/discharge triggered");
	}

	@AllowedAction(UserRolesRightsEnum.EDIT)
	@PostMapping("/{voyagePoid}/imports/selected-xl")
	public ResponseEntity<?> importSelectedXl(@PathVariable Long voyagePoid) {
		log.info("Action={} | Import selected XL | voyagePoid={}", UserContext.getActionRequested(), voyagePoid);
		return ApiResponse.success("Selected XL import completed", vesselVoyageService.importSelectedXl(voyagePoid));
	}

	@AllowedAction(UserRolesRightsEnum.DELETE)
	@DeleteMapping("/{voyagePoid}")
	public ResponseEntity<?> deleteVoyage(@PathVariable Long voyagePoid) {
		log.info("Action={} | Delete voyage | voyagePoid={}", UserContext.getActionRequested(), voyagePoid);
		vesselVoyageService.deleteVoyage(voyagePoid);
		return ApiResponse.success("Vessel voyage deleted successfully");
	}

	// --------- File outputs (served from configured exports folder) ---------

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@GetMapping("/{voyagePoid}/exports/excel/{type}")
	public ResponseEntity<?> downloadExcel(@PathVariable Long voyagePoid, @PathVariable String type) {
		log.info("Action={} | Download Excel export | voyagePoid={} type={}", UserContext.getActionRequested(),
				voyagePoid, type);
		Resource resource = vesselVoyageService.downloadExcelExport(voyagePoid, type);
		String filename = resource.getFilename() != null ? resource.getFilename() : "export.bin";
		return ResponseEntity.status(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"").body(resource);
	}

	@AllowedAction(UserRolesRightsEnum.VIEW)
	@GetMapping("/{voyagePoid}/reports/manifest")
	public ResponseEntity<?> downloadManifest(@PathVariable Long voyagePoid,
			@RequestParam(defaultValue = "FALSE") String freightCargo,
			@RequestParam(defaultValue = "BOTH") String importExport) {
		log.info("Action={} | Download manifest report | voyagePoid={} freightCargo={} importExport={}",
				UserContext.getActionRequested(), voyagePoid, freightCargo, importExport);
		Resource resource = vesselVoyageService.downloadManifestReport(voyagePoid, freightCargo, importExport);
		String filename = resource.getFilename() != null ? resource.getFilename() : "manifest.pdf";
		return ResponseEntity.status(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.contentType(MediaType.APPLICATION_PDF).body(resource);
	}

	@AllowedAction(UserRolesRightsEnum.PRINT)
	@Operation(summary = "Generate PDF for Manifest PDF", description = "Generate PDF report for a specific Manifest PDF")
	@GetMapping("/print/{transactionPoid}")
	public ResponseEntity<?> print(@PathVariable Long transactionPoid,
			@RequestParam(defaultValue = "FALSE") FreightCargo freightCargo,

			@RequestParam(required = false) ImportExport importExport) {
		try {
			byte[] pdf = vesselVoyageService.print(transactionPoid, freightCargo.name(),
					importExport != null ? importExport.name() : null);
			String fileName = freightCargo.name().equalsIgnoreCase("FALSE") ? "cargo-manifest-" : "freight-manifest-";
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION,
							"attachment; filename=" + fileName + transactionPoid + ".pdf")
					.contentType(MediaType.APPLICATION_PDF).body(pdf);
		} catch (Exception e) {
			log.error("Failed to generate PDF for Manifest PDF: {}", transactionPoid, e);
			return ApiResponse.error("Failed to generate PDF: " + e.getMessage(), 500);
		}
	}

}
