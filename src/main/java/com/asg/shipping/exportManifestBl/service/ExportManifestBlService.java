package com.asg.shipping.exportManifestBl.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.exportManifestBl.dto.ExportManifestBlCreateDto;
import com.asg.shipping.exportManifestBl.dto.ExportManifestBlRequestDto;
import com.asg.shipping.exportManifestBl.dto.ExportManifestBlUpdateDto;
import com.asg.shipping.exportManifestBl.dto.ShipBlToFfDto;
import org.springframework.data.domain.Pageable;
import com.asg.shipping.exportManifestUpdate.dto.GenerateBlPrintRequest;
import com.asg.shipping.exportManifestUpdate.dto.GenerateManifestRequest;

import java.util.Map;

public interface ExportManifestBlService {

  
    ExportManifestBlRequestDto createExportManifestBl(ExportManifestBlCreateDto dto);

   
    ExportManifestBlRequestDto updateExportManifestBl(Long id, ExportManifestBlUpdateDto dto, Long companyPoid, Long groupPoid);

   
    Map<String, Object> searchExportManifestBl(String docId, FilterRequestDto request, Pageable pageable);

   
    ExportManifestBlRequestDto getExportManifestBl(Long id);

    
    void deleteExportManifestBl(Long id);

   
    ShipBlToFfDto getShipBlToFfByBlNumber(String blNumber);
    
    
    byte[] generateBlPrint(Long transactionPoid, GenerateBlPrintRequest request, String docId) throws Exception;
    
    
    byte[] generateManifest(Long transactionPoid, GenerateManifestRequest request,String docId)  throws Exception;
    
    
    byte[] generateDetentionStorage(Long transactionPoid, String docId)  throws Exception;
    
    byte[] exportDraftPrint(Long transactionPoid)  throws Exception;
}


