package com.asg.shipping.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "GLOBAL_CURRENCY_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalCurrencyMaster {

    @Id
    @Column(name = "CURRENCY_POID", nullable = false)
    private BigDecimal currencyPoid;

    @Column(name = "GROUP_POID", nullable = false)
    private BigDecimal groupPoid;

    @Column(name = "CURRENCY_CODE", length = 20, nullable = false)
    private String currencyCode;

    @Column(name = "CURRENCY_NAME", length = 100, nullable = false)
    private String currencyName;

    @Column(name = "CURRENCY_NAME2", length = 100)
    private String currencyName2;

    @Column(name = "CURRENCY_DECIMALS")
    private BigDecimal currencyDecimals;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Integer seqno;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastmodifiedBy;

    @UpdateTimestamp
    @Column(name = "LASTMODIFIED_DATE")
    private Timestamp lastmodifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "CURRENCY_SHORT_NAME", length = 20)
    private String currencyShortName;

    @Column(name = "COIN_SHORT_NAME", length = 20)
    private String coinShortName;

    @Column(name = "NUMBER_FORMAT_CURRENCY", length = 100)
    private String numberFormatCurrency;
}

