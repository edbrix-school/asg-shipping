package com.asg.shipping.bookingFormSH.entity;

import java.io.Serializable;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipMateCargoDtlId implements Serializable {

	private Long transactionPoid;
	private Long detRowId;

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ShipMateCargoDtlId that = (ShipMateCargoDtlId) o;
		return Objects.equals(transactionPoid, that.transactionPoid) && Objects.equals(detRowId, that.detRowId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(transactionPoid, detRowId);
	}
}