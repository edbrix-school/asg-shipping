package com.asg.shipping.lineprofile.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineProfileResponse {
    private Long lineProfilePoid;
    private Long linePoid;
    private LineProfileLineDetailsResponse lineDetails;
    private List<Long> regionPoids = new ArrayList<>();
    private List<LovGetListDto> regionDet = new ArrayList<>();
    private String remarks;
    private Long agreementPoid;
    private LineProfileAgreementDetailsResponse agreementDetails;
    private String active;
    private Long seqNo;
    private String deleted;
    private String logoImageBase64;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private List<LineProfileContactDto> contactDetails = new ArrayList<>();
}

