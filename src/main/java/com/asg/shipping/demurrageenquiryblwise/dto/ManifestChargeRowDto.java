package com.asg.shipping.demurrageenquiryblwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Not yet received / invoiced charge of the BL manifest (SHIP_BL_MANIFEST_CHARGES_DTL).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ManifestChargeRowDto {

	private Long blPoid;
	private Long chargePoid;
	private Long detRowId;
	private BigDecimal amount;
	/** Y when the charge originates from a receipt / invoice (EDI_CHARGE_CODE). */
	private String chargeNewRecord;
	private String invoiceType;
	private Long taxPoid;
	private BigDecimal taxPercentage;
	private BigDecimal taxAmount;
	private BigDecimal totalAmount;
}
