package com.asg.shipping.common.service;

import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.common.dto.LovResponse;
import com.asg.shipping.common.repository.LovRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class LovServiceImpl implements LovService {

    @Autowired
    LovRepository lovRepository;

    @Override
    public LovResponse getLovList(String lovName, Long docKeyPoid, String filterValue, Long groupPoid, Long companyPoid, Long userPoid) {
        log.info("Fetching LOV list for lovName={} docKeyPoid={} filterValue={} groupPoid={} companyPoid={} userId={}",
                lovName, docKeyPoid, filterValue, groupPoid, companyPoid, userPoid);
        LovResponse response = lovRepository.getLovList(lovName, docKeyPoid, filterValue, groupPoid, companyPoid, userPoid);
        log.info("Fetched LOV list for lovName={} itemCount={}", lovName,
                response != null && response.getItems() != null ? response.getItems().size() : 0);
        return response;
    }


    @Override
    public LovItem getLovItemByPoid(Long poid, String lovName, Long groupPoid, Long companyPoid, Long userPoid) {
        log.info("poid : {}, lovName : {}, groupPoid : {}, companyPoid : {}, userId : {}",
                poid, lovName, groupPoid, companyPoid, userPoid);

        if (poid == null || StringUtils.isBlank(lovName)) {
            return new LovItem();
        }

        LovItem dto = new LovItem();
        LovResponse listValue = this.getLovList(lovName, poid, "", groupPoid, companyPoid, userPoid);

        if (listValue != null) {

            List<LovItem> lovGetListDtos = listValue.getItems();

            if (lovGetListDtos != null) {

                dto = lovGetListDtos.stream().filter(x -> x.getPoid().equals(poid)).findAny().orElse(new LovItem(poid, null, null, null, null, null));

            }
        }
        return dto;
    }

    @Override
    public LovItem getLovItemByCode(String code, String lovName, Long groupPoid, Long companyPoid, Long userPoid) {
        log.info("code : {}, lovName : {}, groupPoid : {}, companyPoid : {}, userId : {}",
                code, lovName, groupPoid, companyPoid, userPoid);

        if (StringUtils.isBlank(code) || StringUtils.isBlank(lovName)) {
            return new LovItem();
        }

        LovResponse listValue = this.getLovList(lovName, null, "", groupPoid, companyPoid, userPoid);

        if (listValue != null && listValue.getItems() != null) {
            return listValue.getItems().stream()
                    .filter(x -> code.equals(x.getCode()))
                    .findFirst()
                    .orElse(new LovItem(null, code, null, null, null, null));
        }
        return new LovItem(null, code, null, null, null, null);
    }
}
