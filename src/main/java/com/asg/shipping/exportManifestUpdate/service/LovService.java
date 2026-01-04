package com.asg.shipping.exportManifestUpdate.service;

import com.asg.shipping.exportManifestUpdate.dto.LovItem;
import com.asg.shipping.exportManifestUpdate.dto.LovResponse;

public interface LovService {
    LovResponse getLovList(String lovName, Long docKeyPoid, String filterValue, Long groupPoid, Long companyPoid, Long userPoid);

    LovItem getLovItemByPoid(Long poid, String lovName, Long groupPoid, Long companyPoid, Long userPoid);

    LovItem getLovItemByCode(String code, String lovName, Long groupPoid, Long companyPoid, Long userPoid);
}

