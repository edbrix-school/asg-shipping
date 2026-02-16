package com.asg.shipping.tradelanemaster.service;


import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.tradelanemaster.dto.request.ShipTradelaneRequest;
import com.asg.shipping.tradelanemaster.dto.response.ShipTradelaneResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ShipTradeLaneService {
    
    ShipTradelaneResponse create(ShipTradelaneRequest request);
    
    ShipTradelaneResponse update(Long tradeLanePoid, ShipTradelaneRequest request);
    
    ShipTradelaneResponse getById(Long tradeLanePoid);
    
    void delete(Long tradeLanePoid, DeleteReasonDto deleteReasonDto);
    
    Map<String, Object> list(FilterRequestDto filters, Pageable pageable);
}
