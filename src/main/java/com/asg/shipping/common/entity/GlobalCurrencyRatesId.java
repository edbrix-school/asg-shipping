package com.asg.shipping.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalCurrencyRatesId implements Serializable {

    private String currencyCode;
    private Date rateDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GlobalCurrencyRatesId that = (GlobalCurrencyRatesId) o;
        return Objects.equals(currencyCode, that.currencyCode) &&
               Objects.equals(rateDate, that.rateDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currencyCode, rateDate);
    }
}

