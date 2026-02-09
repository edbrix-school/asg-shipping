package com.asg.shipping.lineprofile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineProfileRequest {

    @NotNull(message = "linePoid is required")
    private Long linePoid;

    private List<Long> regionPoids = new ArrayList<>();

    @Size(max = 500, message = "remarks cannot exceed 500 characters")
    private String remarks;

    private Long agreementPoid;

    @Size(max = 1, message = "active must be a single character")
    private String active;

    private Long seqNo;

    private String logoImageBase64;

    @Valid
    private List<LineProfileContactDto> contactDetails = new ArrayList<>();
}

