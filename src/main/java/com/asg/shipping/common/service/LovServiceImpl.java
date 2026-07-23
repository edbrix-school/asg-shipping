package com.asg.shipping.common.service;

import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.service.LovDataService;
import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.common.dto.LovResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LovServiceImpl implements LovService {

    @Autowired
    LovDataService lovDataService;

    @Override
    public LovResponse getLovList(String lovName, Long docKeyPoid, String filterValue, Long groupPoid, Long companyPoid, Long userPoid) {
        log.info("Fetching LOV list for lovName={} docKeyPoid={} filterValue={} groupPoid={} companyPoid={} userId={}",
                lovName, docKeyPoid, filterValue, groupPoid, companyPoid, userPoid);
        Map<String, Object> result = lovDataService.getLovList(
                filterValue != null ? filterValue : "",
                groupPoid, companyPoid, userPoid,
                lovName, 0, 0, "", "");
        List<LovItem> items = toItems((List<LovGetListDto>) result.get("data"));
        log.info("Fetched LOV list for lovName={} itemCount={}", lovName, items.size());
        return new LovResponse(items);
    }

    @Override
    public LovItem getLovItemByPoid(Long poid, String lovName, Long groupPoid, Long companyPoid, Long userPoid) {
        log.info("poid={} lovName={} groupPoid={} companyPoid={} userId={}", poid, lovName, groupPoid, companyPoid, userPoid);
        if (poid == null || StringUtils.isBlank(lovName)) {
            return new LovItem();
        }
        Map<String, Object> result = lovDataService.getLovList(
                "", groupPoid, companyPoid, userPoid,
                lovName, 0, 0, "", "", null, List.of(poid));
        return findByPoid(poid, result);
    }

    @Override
    public LovItem getLovItemByCode(String code, String lovName, Long groupPoid, Long companyPoid, Long userPoid) {
        log.info("code={} lovName={} groupPoid={} companyPoid={} userId={}", code, lovName, groupPoid, companyPoid, userPoid);
        if (StringUtils.isBlank(code) || StringUtils.isBlank(lovName)) {
            return new LovItem();
        }
        Map<String, Object> result = lovDataService.getLovList(
                "", groupPoid, companyPoid, userPoid,
                lovName, 0, 0, "", "", List.of(code), null);
        return findByCode(code, result);
    }

    @Override
    public Map<Long, LovItem> getLovItemsByPoids(List<Long> poids, String lovName, Long groupPoid, Long companyPoid, Long userPoid) {
        if (poids == null || poids.isEmpty() || StringUtils.isBlank(lovName)) {
            return Collections.emptyMap();
        }
        List<Long> distinctPoids = poids.stream().filter(p -> p != null).distinct().collect(Collectors.toList());
        if (distinctPoids.isEmpty()) return Collections.emptyMap();
        Map<String, Object> result = lovDataService.getLovList(
                "", groupPoid, companyPoid, userPoid,
                lovName, 0, 0, "", "", null, distinctPoids);
        List<LovGetListDto> data = (List<LovGetListDto>) result.get("data");
        List<LovGetListDto> defaults = (List<LovGetListDto>) result.get("defaultValues");
        Map<Long, LovItem> map = new java.util.HashMap<>();
        if (data != null) data.forEach(x -> { if (x.getPoid() != null) map.put(x.getPoid(), toItem(x)); });
        if (defaults != null) defaults.forEach(x -> { if (x.getPoid() != null) map.putIfAbsent(x.getPoid(), toItem(x)); });
        return map;
    }

    @Override
    public Map<String, LovItem> getLovItemsByCodes(List<String> codes, String lovName, Long groupPoid, Long companyPoid, Long userPoid) {
        if (codes == null || codes.isEmpty() || StringUtils.isBlank(lovName)) {
            return Collections.emptyMap();
        }
        List<String> distinctCodes = codes.stream().filter(c -> !StringUtils.isBlank(c)).map(String::trim).distinct().collect(Collectors.toList());
        if (distinctCodes.isEmpty()) return Collections.emptyMap();
        Map<String, Object> result = lovDataService.getLovList(
                "", groupPoid, companyPoid, userPoid,
                lovName, 0, 0, "", "", distinctCodes, null);
        List<LovGetListDto> data = (List<LovGetListDto>) result.get("data");
        List<LovGetListDto> defaults = (List<LovGetListDto>) result.get("defaultValues");
        Map<String, LovItem> map = new java.util.HashMap<>();
        if (data != null) data.forEach(x -> { if (x.getCode() != null) map.put(x.getCode().toUpperCase(), toItem(x)); });
        if (defaults != null) defaults.forEach(x -> { if (x.getCode() != null) map.putIfAbsent(x.getCode().toUpperCase(), toItem(x)); });
        return map;
    }

    private LovItem findByPoid(Long poid, Map<String, Object> result) {
        List<LovGetListDto> data = (List<LovGetListDto>) result.get("data");
        if (data != null) {
            LovItem found = data.stream()
                    .filter(x -> poid.equals(x.getPoid()))
                    .findFirst().map(this::toItem).orElse(null);
            if (found != null) return found;
        }
        List<LovGetListDto> defaults = (List<LovGetListDto>) result.get("defaultValues");
        if (defaults != null) {
            return defaults.stream()
                    .filter(x -> poid.equals(x.getPoid()))
                    .findFirst().map(this::toItem)
                    .orElse(new LovItem(poid, null, null, null, null, null));
        }
        return new LovItem(poid, null, null, null, null, null);
    }

    private LovItem findByCode(String code, Map<String, Object> result) {
        List<LovGetListDto> data = (List<LovGetListDto>) result.get("data");
        if (data != null) {
            LovItem found = data.stream()
                    .filter(x -> code.equalsIgnoreCase(x.getCode()))
                    .findFirst().map(this::toItem).orElse(null);
            if (found != null) return found;
        }
        List<LovGetListDto> defaults = (List<LovGetListDto>) result.get("defaultValues");
        if (defaults != null) {
            return defaults.stream()
                    .filter(x -> code.equalsIgnoreCase(x.getCode()))
                    .findFirst().map(this::toItem)
                    .orElse(new LovItem(null, code, null, null, null, null));
        }
        return new LovItem(null, code, null, null, null, null);
    }

    private LovItem toItem(LovGetListDto dto) {
        if (dto == null) return new LovItem();
        return new LovItem(dto.getPoid(), dto.getCode(), dto.getDescription(), dto.getLabel(), dto.getValue(), dto.getSeqNo());
    }

    private List<LovItem> toItems(List<LovGetListDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(this::toItem).collect(Collectors.toList());
    }
}
