package com.asg.shipping.importmanifestupdate.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.shipping.importmanifestupdate.dto.*;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ImportManifestBlService {

    ImportManifestBlRequestDto updateImportManifestBl(Long id, ImportManifestBlUpdateDTO dto, Long companyPoid, Long groupPoid);

    ImportManifestUpdateOpsDto updateImportManifestUpdateOps(Long id, ImportManifestUpdateOpsDto dto, Long companyPoid, Long groupPoid);

    Map<String, Object> listOfImportManifest(String docId, FilterRequestDto request, Pageable pageable);

    ImportManifestBlRequestDto getImportManifestBl(Long id);

    ImportManifestUpdateOpsDto getImportManifestUpdateOps(Long id);

    void deleteImportManifestBl(Long id, DeleteReasonDto deleteReasonDto);

    ResendCanResponseDto resendCan(Long transactionPoId, String updateDemurrage);

    LoadEmailFaxResponseDto loadEmailFax(Long addressMasterPoid, String addressType);

    BlStatusResponseDto getBlStatus(Long transactionPoId);

}
