package com.asg.shipping.MafiTrailerDateUpdateForm.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class BlDetailsProjectionImpl implements BlDetailsProjection {
	private final Long blPoid;
	private final String blNumber;
	private final String mafiRef;
	private final BigDecimal mafiSize;
	private final BigDecimal mafiFreeDays;
	private final Long voyagePoid;
}
