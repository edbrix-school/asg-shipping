package com.asg.shipping.linemasterthirdparty.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating Line Master Third Party
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Line Master Third Party creation request")
public class LineMasterThirdPartyCreateDTO {

    @NotBlank(message = "Line Code is mandatory")
    @Size(max = 20, message = "Line code cannot exceed 20 characters")
    @Schema(description = "Line Code", example = "TP001")
    private String lineCode;

    @NotBlank(message = "Line Name is mandatory")
    @Size(max = 100, message = "Line name cannot exceed 100 characters")
    @Schema(description = "Line Name", example = "ABC Third Party Line")
    private String lineName;

    @Size(max = 100, message = "Line name2 cannot exceed 100 characters")
    @Schema(description = "Line Name (Secondary)", example = "ABC TP Ltd")
    private String lineName2;

    @Size(max = 500, message = "Line address cannot exceed 500 characters")
    @Schema(description = "Line Address", example = "123 Third Party Street")
    private String lineAddress;

    @Schema(description = "Country POID", example = "100")
    private Long countryPoid;

    @Schema(description = "Currency POID", example = "200")
    private Long currencyPoid;

    @Size(max = 50, message = "Bill to cannot exceed 50 characters")
    @Schema(description = "Bill To (Customer Code)", example = "CUST001")
    private String billTo;

    @Pattern(regexp = "Y|N", message = "Active must be 'Y' or 'N'")
    @Size(max = 1, message = "Active cannot exceed 1 character")
    @Schema(description = "Active Status", example = "Y", allowableValues = {"Y", "N"})
    private String active;

    @Schema(description = "Sequence Number", example = "1")
    private Integer seqno;
}

