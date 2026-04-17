package com.asg.shipping.MafiTrailerDateUpdateForm.dto;

import java.math.BigDecimal;

public interface BlDetailsProjection {
	Long getBlPoid();

	String getBlNumber();

	String getMafiRef();

	BigDecimal getMafiSize();

	BigDecimal getMafiFreeDays();

	Long getVoyagePoid();
}
