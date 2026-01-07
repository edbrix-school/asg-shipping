package com.asg.shipping.salesinvoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Response DTO for loading BL data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadBlDataResponseDTO {

    private String companyPoid;
    private String customerPoid;
    private String bookingPartyPoid;
    private String creditDays;
    private String invDate;
    private String ownInvoiceNo;
    private String blTypeInvoice;

}

