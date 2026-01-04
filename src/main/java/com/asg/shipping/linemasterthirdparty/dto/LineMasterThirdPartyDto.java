package com.asg.shipping.linemasterthirdparty.dto;

import com.asg.common.lib.dto.LovGetListDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for Line Master Third Party details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Line Master Third Party detail response")
public class LineMasterThirdPartyDto {

    @Schema(description = "Line POID (Primary Key)")
    private Long linePoid;

    @Schema(description = "Line Code", required = true)
    private String lineCode;

    @Schema(description = "Line Name", required = true)
    private String lineName;

    @Schema(description = "Line Name (Secondary)")
    private String lineName2;

    @Schema(description = "Line Address")
    private String lineAddress;

    @Schema(description = "Country POID")
    private Long countryPoid;

    @Schema(description = "Country details (from LOV)")
    private LovGetListDto countryDet;

    @Schema(description = "Currency POID")
    private Long currencyPoid;

    @Schema(description = "Currency details (from LOV)")
    private LovGetListDto currencyDet;

    @Schema(description = "Active Status (Y/N)")
    private String active;

    @Schema(description = "Sequence Number")
    private Integer seqno;

    @Schema(description = "Bill To (Customer Code)")
    private String billTo;

    @Schema(description = "Bill To details (from LOV)")
    private LovGetListDto billToDet;

    @Schema(description = "Created By")
    private String createdBy;

    @Schema(description = "Created Date")
    private LocalDateTime createdDate;

    @Schema(description = "Last Modified By")
    private String lastModifiedBy;

    @Schema(description = "Last Modified Date")
    private LocalDateTime lastModifiedDate;

    @Schema(description = "Deleted Flag (Y/N)")
    private String deleted;
}

