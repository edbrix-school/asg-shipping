package com.asg.shipping.bookingformsh.dto;

import java.math.BigDecimal;

import com.asg.shipping.common.dto.LovItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for SHIP_MATE_CHARGES_DTL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingFormChargesDetailDto {

	private Long detRowId;
	private Long chargePoid;
	private LovItem chargePoidDet; // LOV data
	private BigDecimal currencyExchange;
	private BigDecimal quantity;
	private BigDecimal perQuantityAmount;
	private Long paidAtPortPoid;
	private LovItem paidAtPortPoidDet; // LOV data
	private BigDecimal buyPercharge;
	private String currencyCode;
	private String action; // ISCREATE, ISUPDATE, ISDELETE
}
