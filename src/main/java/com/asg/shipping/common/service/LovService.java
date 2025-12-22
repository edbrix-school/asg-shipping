package com.asg.shipping.common.service;

import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.common.dto.LovResponse;
import org.springframework.stereotype.Service;

@Service
public interface LovService {
    LovResponse getLovList(String lovName, Long docKeyPoid, String filterValue, Long groupPoid, Long companyPoid, Long userPoid);

    LovItem getLovItemByPoid(Long poid, String lovName, Long groupPoid, Long companyPoid, Long userPoid);

    LovItem getLovItemByCode(String code, String lovName, Long groupPoid, Long companyPoid, Long userPoid);
}
