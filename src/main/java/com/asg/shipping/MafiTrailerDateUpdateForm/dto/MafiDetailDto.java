package com.asg.shipping.MafiTrailerDateUpdateForm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MafiDetailDto {

	private Long transactionPoid;
	private Long detRowId;
	private Long blPoid;
//	private Map<String, Object> blDetail;
	private String mafiRef;
	private BigDecimal mafiSize;
	private BigDecimal mafiFreeDays;
	private String remarks;
	private LocalDate mafiEmptyDate;
	private LocalDate backLoadDate;

}
