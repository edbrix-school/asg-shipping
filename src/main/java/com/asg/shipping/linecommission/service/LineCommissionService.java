package com.asg.shipping.linecommission.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.containertypes.dto.ContainerTypeDto;
import com.asg.shipping.linecommission.dto.LineCommissionResponse;
import com.asg.shipping.linecommission.dto.LineCommissionRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface LineCommissionService {
    Map<String, Object> listLineCommissions(String docId, FilterRequestDto filters, Pageable pageable);

    LineCommissionResponse getById(Long transactionPoid, Long groupPoid);

    LineCommissionResponse create(LineCommissionRequest request, Long groupPoid, String userId, String docId);

    LineCommissionResponse update(Long transactionPoid, LineCommissionRequest request, Long groupPoid, String userId, String docId);

    void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto);

    List<ContainerTypeDto> loadContainerTypes(Long transactionPoid, Long groupPoid, String userId);
}


