package com.asg.shipping.bookingformsh.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for SHIP_MATE_CARGO_DTL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingFormCargoDetailDto {

	private Long detRowId;
	private String cargoDescription;
	private String equipmentType;
	private String equipmentSize;
	private BigDecimal quantity;
	private BigDecimal volume;
	private BigDecimal weight;
	private String equipmentIsoType;
//	private LovItem equipmentIsoTypeDet; // LOV data
	private String isImco;
	private String imo;
	private String isOog;
	private String oogL;
	private String oogB;
	private String oogH;
	private String refferTemp;
	private String refferHum;
	private String refferVent;
	private String isRefer;
	private String referType;
	private String oogLW;
	private String oogRW;
	private String oogF;
	private String oogA;
	private String action; // ISCREATE, ISUPDATE, ISDELETE
}
