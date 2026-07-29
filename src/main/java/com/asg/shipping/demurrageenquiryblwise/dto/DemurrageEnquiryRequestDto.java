package com.asg.shipping.demurrageenquiryblwise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request of the "Apply Date" action of the Demurrage Enquiry - BL wise screen (100-144).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DemurrageEnquiryRequestDto {

	@NotNull(message = "BL POID is required")
	@Schema(description = "Transaction POID of the BL (LOV ALLBLNUMBER)", example = "1001")
	private Long blPoid;

	@Schema(description = "To Date up to which the demurrage is calculated. Defaults to the current date.",
			example = "2025-07-28")
	private LocalDate toDate;

	@DecimalMin(value = "0", message = "Discount(%) cannot be negative")
	@DecimalMax(value = "100", message = "Discount(%) cannot be greater than 100")
	@Schema(description = "Discount percentage applied on the calculated demurrage amount", example = "10")
	private BigDecimal discountPercentage;
}
