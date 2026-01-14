package com.asg.shipping.containerinventorymovementupdate.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryCimuRequest {
    @Size(max = 50, message = "containerNo must be <= 50 chars")
    private String containerNo;

    @Size(max = 50, message = "blNumber must be <= 50 chars")
    private String blNumber;
}


