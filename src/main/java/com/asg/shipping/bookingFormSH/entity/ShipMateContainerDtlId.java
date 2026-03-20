package com.asg.shipping.bookingformsh.entity;

import java.io.Serializable;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipMateContainerDtlId implements Serializable {

	private Long transactionPoid;
	private Long detRowId;

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ShipMateContainerDtlId that = (ShipMateContainerDtlId) o;
		return Objects.equals(transactionPoid, that.transactionPoid) && Objects.equals(detRowId, that.detRowId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(transactionPoid, detRowId);
	}
}
