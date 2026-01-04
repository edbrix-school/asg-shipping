package com.asg.shipping.MafiTrailerDateUpdateForm.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MafiTrailerDateUpdateFormRequest {
	private MafitrailerHeaderDTO mafiHeader;
	private List<MafiDetailDto> mafiDetails;
}
