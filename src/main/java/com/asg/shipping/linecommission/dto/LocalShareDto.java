package com.asg.shipping.linecommission.dto;

import com.asg.common.lib.dto.LovGetListDto;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocalShareDto {
    private Long detRowId; // for update
    private String actionType;

    @NotNull(message = "chargePoid is required")
    private Long chargePoid;
    private LovGetListDto chargeDet;
    private Long percent;
    private Long amount;
    private String remarks;
}


