package com.asg.shipping.exportManifestUpdate.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.exportManifestUpdate.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Export Manifest BL operations
 */
public interface ExportManifestBlService {

    // Header Operations
    ExportManifestBlResponse getExportBlById(Long transactionPoid);
    
    Map<String, Object> searchExportBls(FilterRequestDto filters, Pageable pageable);
    
    ExportManifestBlResponse createExportBl(ExportManifestBlRequest request);
    
    ExportManifestBlResponse updateExportBl(Long transactionPoid, ExportManifestBlRequest request);
    
    ExportManifestUpdateResponse updateExportBlCombined(Long transactionPoid, ExportManifestUpdateRequest request);
    
    void deleteExportBl(Long transactionPoid);

    // General Cargo Details Operations
    List<GeneralCargoDetailDto> getGeneralCargoDetails(Long transactionPoid);
    List<GeneralCargoDetailDto> updateGeneralCargoDetails(Long transactionPoid, List<GeneralCargoDetailDto> request);

    // Container Details Operations
    List<ContainerDetailDto> getContainerDetails(Long transactionPoid);
    List<ContainerDetailDto> updateContainerDetails(Long transactionPoid, List<ContainerDetailDto> request);

    // Cargo Description and Marks Operations
    List<CargoDescriptionDto> getCargoDescription(Long transactionPoid);
    List<CargoDescriptionDto> updateCargoDescription(Long transactionPoid, List<CargoDescriptionDto> request);
    ExportManifestCargoContainerResponse getCargoContainerDetails(Long transactionPoid);
    
    List<CargoMarksDto> getCargoMarks(Long transactionPoid);
    
    List<CargoMarksDto> updateCargoMarks(Long transactionPoid, List<CargoMarksDto> request);

    // Charge Details Operations
    Map<String, Object> getChargeDetails(Long transactionPoid);
    
    Map<String, Object> updateChargeDetails(Long transactionPoid, List<ChargeDetailDto> request);

    // Special Operations
    Map<String, Object> loadBooking(Long transactionPoid, LoadBookingRequest request);
    
    byte[] generateBlPrint(Long transactionPoid, GenerateBlPrintRequest request, String docId) throws Exception;
    
    byte[] generateManifest(Long transactionPoid, GenerateManifestRequest request,String docId)  throws Exception;
    
    byte[] generateDetentionStorage(Long transactionPoid, String docId)  throws Exception;
    
    void exportEdi(Long transactionPoid);
    
    ValidationResponse validate(Long transactionPoid, ExportManifestBlRequest request);
    
    void afterSave(Long transactionPoid);
    
    BlStatusResponse getBlStatus(Long transactionPoid);
    
    Map<String, Object> quotationAfterBrowse(Long transactionPoid, QuotationAfterBrowseRequest request);

    ExportManifestAddressDto getAddressDetails(Long addressMasterPoid, String addressType);
}

