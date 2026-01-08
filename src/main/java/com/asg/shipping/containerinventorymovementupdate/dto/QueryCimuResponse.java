package com.asg.shipping.containerinventorymovementupdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryCimuResponse {
    private QueryEcho queryEcho;
    private Permissions permissions;
    private List<ContainerInfoDto> containerInfoList;
    private List<ContainerHistoryRowDto> containerHistoryList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QueryEcho {
        private String containerNo;
        private String blNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Permissions {
        /**
         * Controls visibility of "Dem Tariff override Amount" field (AmountPerDayAfterFree).
         * Legacy uses right "000-279" "Edit" to set actualTariffDischargeColumnEnableDisable property (line 207-211).
         * Legacy UI binds this property to AmountPerDayAfterFree field visibility (JSF line 386).
         * Naming: Matches legacy property name concept (actualTariffDischargeColumnEnableDisable).
         * Frontend should use this boolean to show/hide the "Dem Tariff override Amount" field.
         */
        private boolean canEditActualDischargeDate;
    }

}


