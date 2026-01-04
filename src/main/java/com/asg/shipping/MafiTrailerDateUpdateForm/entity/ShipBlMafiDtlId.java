package com.asg.shipping.MafiTrailerDateUpdateForm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipBlMafiDtlId implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Column(name = "TRANSACTION_POID")
	private Long transactionPoid;

	@Column(name = "DET_ROW_ID")
	private Long detRowId;
}
