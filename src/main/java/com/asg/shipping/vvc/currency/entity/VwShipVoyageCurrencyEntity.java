package com.asg.shipping.vvc.currency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "VW_SHIP_VOYAGE_CURRENCY")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(VwShipVoyageCurrencyId.class)
public class VwShipVoyageCurrencyEntity {

    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Column(name = "CURRENCY_CODE")
    private String currencyCode;

    @Column(name = "CURRENCY_EXCHANGE")
    private Double currencyExchange;

    @Column(name = "NEW_CURRENCY_EXCHANGE")
    private Double newCurrencyExchange;
}


