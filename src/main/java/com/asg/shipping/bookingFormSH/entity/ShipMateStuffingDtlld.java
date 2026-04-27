package com.asg.shipping.bookingFormSH.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipMateStuffingDtlld {

    private Long transactionPoid;
    private Long detRowId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShipMateStuffingDtlld)) return false;
        ShipMateStuffingDtlld that = (ShipMateStuffingDtlld) o;
        return Objects.equals(transactionPoid, that.transactionPoid) &&
                Objects.equals(detRowId, that.detRowId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionPoid, detRowId);
    }

}
