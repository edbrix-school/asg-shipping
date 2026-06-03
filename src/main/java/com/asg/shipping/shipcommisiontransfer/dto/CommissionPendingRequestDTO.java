package com.asg.shipping.shipcommisiontransfer.dto;

import lombok.Data;

@Data
public class CommissionPendingRequestDTO {
    private Double exchangeRate;
    private Long blPoid;
    private Double frtBuyActual;
    private String shortLegSelected;
    private String recordType;
    private Long voyageTransactionPoid;
}
