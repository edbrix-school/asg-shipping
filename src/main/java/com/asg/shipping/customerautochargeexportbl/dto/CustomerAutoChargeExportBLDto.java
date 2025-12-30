package com.asg.shipping.customerautochargeexportbl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Response DTO for Customer Auto Charge Export BL")
public class CustomerAutoChargeExportBLDto {

    @Schema(description = "Transaction POID", example = "1")
    private Long transactionPoid;

    @Schema(description = "Group POID", example = "100")
    private Long groupPoid;

    @Schema(description = "Customer POID", example = "1001")
    private Long customerPoid;

    @Schema(description = "Document Reference", example = "DOC-001")
    private String docRef;

    @Schema(description = "Transaction Date", example = "2024-01-01")
    private LocalDate transactionDate;

    @Schema(description = "Description", example = "Auto Charge Export BL")
    private String description;

    @Schema(description = "Period From Date", example = "2024-01-01")
    private LocalDate periodFrom;

    @Schema(description = "Period To Date", example = "2024-12-31")
    private LocalDate periodTo;

    @Schema(description = "Sequence Number", example = "1")
    private Integer seqno;

    @Schema(description = "Deleted Flag", example = "N")
    private String deleted;

    @Schema(description = "Created By", example = "admin")
    private String createdBy;

    @Schema(description = "Created Date")
    private Timestamp createdDate;

    @Schema(description = "Last Modified By", example = "admin")
    private String lastModifiedBy;

    @Schema(description = "Last Modified Date")
    private Timestamp lastModifiedDate;

    @Schema(description = "List of charge details")
    private List<CustomerAutoChargeDetailDto> chargeDetails;
}
