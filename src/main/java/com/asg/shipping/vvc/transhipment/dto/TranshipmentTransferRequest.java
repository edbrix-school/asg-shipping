package com.asg.shipping.vvc.transhipment.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranshipmentTransferRequest {
    @NotNull
    private Long targetVoyagePoid;

    @NotEmpty
    private List<Long> detRowIds;
}


