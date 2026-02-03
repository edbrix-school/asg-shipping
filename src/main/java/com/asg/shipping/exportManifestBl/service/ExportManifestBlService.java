package com.asg.shipping.exportManifestBl.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.exportManifestBl.dto.ExportManifestBlCreateDto;
import com.asg.shipping.exportManifestBl.dto.ExportManifestBlRequestDto;
import com.asg.shipping.exportManifestBl.dto.ExportManifestBlUpdateDto;
import com.asg.shipping.exportManifestBl.dto.ShipBlToFfDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ExportManifestBlService {

  
    ExportManifestBlRequestDto createExportManifestBl(ExportManifestBlCreateDto dto);

   
    ExportManifestBlRequestDto updateExportManifestBl(Long id, ExportManifestBlUpdateDto dto, Long companyPoid, Long groupPoid);

   
    Map<String, Object> searchExportManifestBl(String docId, FilterRequestDto request, Pageable pageable);

   
    ExportManifestBlRequestDto getExportManifestBl(Long id);

    
    void deleteExportManifestBl(Long id);

   
    ShipBlToFfDto getShipBlToFfByBlNumber(String blNumber);
}


