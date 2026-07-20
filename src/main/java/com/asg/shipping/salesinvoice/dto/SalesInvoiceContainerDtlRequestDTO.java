package com.asg.shipping.salesinvoice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SalesInvoiceContainerDtlRequestDTO {

    private Long transactionPoid;

    @NotNull(message = "BL POID is required")
    private Long blPoid;

    @NotNull(message = "Container No is required")
    private String containerNo;

    @NotNull(message = "To Date is required")
    private LocalDate dmToDate;
}
