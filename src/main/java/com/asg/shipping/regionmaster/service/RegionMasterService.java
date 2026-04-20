package com.asg.shipping.regionmaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.regionmaster.dto.RegionMasterRequest;
import com.asg.shipping.regionmaster.dto.RegionMasterResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface RegionMasterService {
    
    Map<String, Object> listRegionMasters(
            String docId,
            FilterRequestDto request,
            Pageable pageable);

    RegionMasterResponse getById(Long regionPoid, Long groupPoid);

    RegionMasterResponse create(
            RegionMasterRequest request,
            Long groupPoid,
            String userId,
            String docId);

    RegionMasterResponse update(
            Long regionPoid,
            RegionMasterRequest request,
            Long groupPoid,
            String userId,
            String docId);

    void toggleActiveStatus(
            Long regionPoid,
            Long groupPoid,
            String userId);

    void delete(
            Long regionPoid, DeleteReasonDto deleteReasonDto);
}

