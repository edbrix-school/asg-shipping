package com.asg.shipping.daycloseshiping.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DayCloseDto {

	private DayCloseHdrDto header;
	private List<DayCloseDenominationDto> denominations;
}
