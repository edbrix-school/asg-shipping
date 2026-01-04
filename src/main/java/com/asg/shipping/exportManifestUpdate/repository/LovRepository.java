package com.asg.shipping.exportManifestUpdate.repository;

import com.asg.shipping.exportManifestUpdate.dto.LovResponse;

public interface LovRepository {
    LovResponse getLovList(String lovName, Long docKeyPoid, String filterField, String filterValue, 
                          Long groupPoid, Long companyPoid, Long userPoid);
}

