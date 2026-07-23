package com.asg.shipping.common.service;

import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.common.dto.LovResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface LovService {
    LovResponse getLovList(String lovName, Long docKeyPoid, String filterValue, Long groupPoid, Long companyPoid, Long userPoid);

    LovItem getLovItemByPoid(Long poid, String lovName, Long groupPoid, Long companyPoid, Long userPoid);

    LovItem getLovItemByCode(String code, String lovName, Long groupPoid, Long companyPoid, Long userPoid);

    Map<Long, LovItem> getLovItemsByPoids(List<Long> poids, String lovName, Long groupPoid, Long companyPoid, Long userPoid);

    Map<String, LovItem> getLovItemsByCodes(List<String> codes, String lovName, Long groupPoid, Long companyPoid, Long userPoid);
}
