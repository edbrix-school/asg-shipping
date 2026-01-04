package com.asg.shipping.exportManifestUpdate.service;

import com.asg.shipping.exportManifestUpdate.dto.LovItem;
import com.asg.shipping.exportManifestUpdate.dto.LovResponse;
import com.asg.shipping.exportManifestUpdate.repository.LovRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service implementation for LOV operations
 * Uses PROC_LOV_GETLIST stored procedure to fetch LOV data
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LovServiceImpl implements LovService {

    private final LovRepository lovRepository;

    @Override
    public LovResponse getLovList(String lovName, Long docKeyPoid, String filterValue, Long groupPoid, Long companyPoid, Long userPoid) {
        log.info("Fetching LOV list for lovName={} docKeyPoid={} filterValue={} groupPoid={} companyPoid={} userId={}",
                lovName, docKeyPoid, filterValue, groupPoid, companyPoid, userPoid);
        
        // filterField is left as empty string as per requirement
        String filterField = "";
        String filterValueStr = filterValue != null ? filterValue : "";
        
        LovResponse response = lovRepository.getLovList(lovName, docKeyPoid, filterField, filterValueStr, groupPoid, companyPoid, userPoid);
        
        log.info("Fetched LOV list for lovName={} itemCount={}", lovName,
                response != null && response.getItems() != null ? response.getItems().size() : 0);
        return response;
    }

    @Override
    public LovItem getLovItemByPoid(Long poid, String lovName, Long groupPoid, Long companyPoid, Long userPoid) {
        log.info("Fetching LOV item by POID: poid={}, lovName={}, groupPoid={}, companyPoid={}, userId={}",
                poid, lovName, groupPoid, companyPoid, userPoid);

        if (poid == null || StringUtils.isBlank(lovName)) {
            return new LovItem();
        }

        // Call stored procedure with POID as filterValue (as string) for POID-based filtering
        // filter_field is left as empty string as per requirement
        // Some LOV queries in LOV_Script.sql support filtering by POID via P_LOV_FILTER_VALUE
        // For example: VESSAL_VOYAGE checks TRANSACTION_POID LIKE '%' || P_LOV_FILTER_VALUE || '%'
        // CUSTOMER_MASTER checks CUSTOMER_POID LIKE '%' || P_LOV_FILTER_VALUE || '%'
        String filterField = ""; // Left as empty string as per requirement
        String filterValue = poid.toString(); // Pass POID as string for filtering
        
        LovResponse listValue = lovRepository.getLovList(lovName, null, filterField, filterValue, groupPoid, companyPoid, userPoid);

        if (listValue != null && listValue.getItems() != null) {
            List<LovItem> lovItems = listValue.getItems();
            
            // Find the item matching the POID exactly
            // Some queries use LIKE which might return multiple items, so we filter to find exact match
            return lovItems.stream()
                    .filter(x -> x.getPoid() != null && x.getPoid().equals(poid))
                    .findAny()
                    .orElse(new LovItem(poid, null, null, null, null, null));
        }
        
        return new LovItem(poid, null, null, null, null, null);
    }

    @Override
    public LovItem getLovItemByCode(String code, String lovName, Long groupPoid, Long companyPoid, Long userPoid) {
        log.info("Fetching LOV item by Code: code={}, lovName={}, groupPoid={}, companyPoid={}, userId={}",
                code, lovName, groupPoid, companyPoid, userPoid);

        if (StringUtils.isBlank(code) || StringUtils.isBlank(lovName)) {
            return new LovItem();
        }

        // Use code as filterValue
        LovResponse listValue = this.getLovList(lovName, null, code, groupPoid, companyPoid, userPoid);

        if (listValue != null && listValue.getItems() != null) {
            return listValue.getItems().stream()
                    .filter(x -> code.equals(x.getCode()))
                    .findFirst()
                    .orElse(new LovItem(null, code, null, null, null, null));
        }
        
        return new LovItem(null, code, null, null, null, null);
    }
}

