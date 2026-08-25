package com.asg.shipping.demurrageenquiryblwise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request of the row wise calculation of the Demurrage Enquiry - BL wise screen (100-144): the
 * demurrage of the given container rows only, as against "Apply Date" which recalculates the whole
 * BL and its charges.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DemurrageContainerCalcRequestDto {

	@NotNull(message = "BL POID is required")
	@Schema(description = "Transaction POID of the BL the containers belong to (LOV ALLBLNUMBER)", example = "1001")
	private Long blPoid;

	@Schema(description = "To Date up to which the demurrage is calculated. Defaults to the current date.",
			example = "2025-07-28")
	private LocalDate toDate;

	@DecimalMin(value = "0", message = "Discount(%) cannot be negative")
	@DecimalMax(value = "100", message = "Discount(%) cannot be greater than 100")
	@Schema(description = "Discount percentage applied on the calculated demurrage amount of every row",
			example = "10")
	private BigDecimal discountPercentage;

	@Min(value = 0, message = "Free days cannot be negative")
	@Schema(description = """
			Free days applied to every row that does not carry free days of its own. It replaces the \
			free days of the container / line tariff, it is not added on top of them.""",
			example = "7")
	private Integer freeDays;

	@NotEmpty(message = "At least one container row is required")
	@Valid
	@Schema(description = "Container rows to calculate - one row, or as many rows of the BL as needed")
	private List<DemurrageContainerCalcRowDto> containers;
}
