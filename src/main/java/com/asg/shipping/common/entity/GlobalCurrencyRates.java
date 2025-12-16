package com.asg.shipping.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "GLOBAL_CURRENCY_RATES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(GlobalCurrencyRatesId.class)
public class GlobalCurrencyRates {

    @Id
    @Column(name = "CURRENCY_CODE", length = 20, nullable = false)
    private String currencyCode;

    @Id
    @Column(name = "RATE_DATE", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date rateDate;

    @Column(name = "GROUP_POID")
    private BigDecimal groupPoid;

    @Column(name = "BUY_RATE")
    private BigDecimal buyRate;

    @Column(name = "SELL_RATE")
    private BigDecimal sellRate;

    @Column(name = "DOC_REF", length = 25)
    private String docRef;

    @Column(name = "DELETED", length = 1)
    private String deleted;
}

