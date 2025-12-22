package com.asg.shipping.linemasterthirdparty.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineMasterThirdPartyFilterRequest {
    private String operator;
    private String isDeleted;
    private List<FilterItem> filters;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterItem {
        private String searchField;
        private String searchValue;
    }
}
