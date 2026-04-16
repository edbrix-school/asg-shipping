package com.asg.shipping.mafitrailerdateupdateform.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MafiDetailDto {

	private Long detRowId;
	private Long blPoid;
	private String mafiRef;
	private BigDecimal mafiSize;
	private BigDecimal mafiFreeDays;
	private String remarks;
	private LocalDate mafiEmptyDate;
	private LocalDate backLoadDate;

}
