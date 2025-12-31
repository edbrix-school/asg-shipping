package com.asg.shipping.importManifestUpdate.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.importManifestUpdate.dto.ImportManifestBlRequestDto;
import com.asg.shipping.importManifestUpdate.dto.ImportManifestBlUpdateDTO;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ImportManifestBlService {

    ImportManifestBlRequestDto updateImportManifestBl(Long id, ImportManifestBlUpdateDTO dto, Long companyPoid, Long groupPoid);

    Map<String, Object> listOfImportManifest(String docId, FilterRequestDto request, Pageable pageable);
}
