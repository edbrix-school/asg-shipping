package com.asg.shipping.vvc.currency.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VwShipVoyageCurrencyId implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}


