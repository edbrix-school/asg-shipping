package com.asg.shipping.vesselvoyagecreation.dto;

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

    private List<Long> detRowIds;
}










