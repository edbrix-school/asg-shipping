package com.asg.shipping.importManifestUpdate.service;

import com.asg.shipping.importManifestUpdate.dto.ImportManifestBlCreateDto;
import com.asg.shipping.importManifestUpdate.dto.ImportManifestBlRequestDto;
import com.asg.shipping.importManifestUpdate.dto.ImportManifestBlUpdateDTO;

public interface ImportManifestBlService {

    ImportManifestBlRequestDto createImportManifestBl(ImportManifestBlCreateDto dto);

    ImportManifestBlRequestDto updateImportManifestBl(Long id, ImportManifestBlUpdateDTO dto, Long companyPoid, Long groupPoid);
}
