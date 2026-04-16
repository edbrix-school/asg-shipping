package com.asg.shipping.mafitrailerdateupdateform.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MafiDetailDtoRequest extends MafiDetailDto{
    private String actionType;
}
