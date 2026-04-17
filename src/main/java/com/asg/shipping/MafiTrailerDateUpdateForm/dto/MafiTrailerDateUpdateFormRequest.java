package com.asg.shipping.MafiTrailerDateUpdateForm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MafiTrailerDateUpdateFormRequest {
    private MafitrailerHeaderDTO mafiHeader;
    private List<MafiDetailDtoRequest> mafiDetails;
}
