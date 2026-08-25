package com.asg.shipping.demurrageenquiryblwise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * One container row sent to the row wise calculation. Only the container number, the To Date and the
 * free days of the row are read - the Empty In date, the ISO type and the tariff free days are always
 * taken from the BL, so a row cannot be calculated against figures the BL does not hold.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DemurrageContainerCalcRowDto {

	@NotBlank(message = "Container number is required")
	@Schema(description = "Container number of the row, as returned by /bl-details", example = "MSCU1234567")
	private String containerNo;

	@Schema(description = """
			To Date up to which this row is calculated. It takes precedence over the To Date of the \
			request. Leave it empty to fall back on the To Date of the request, and on the current \
			date when that one is empty too. A container that was returned empty is charged only up \
			to its Empty In date, whatever To Date the row carries.""",
			example = "2025-07-28")
	private LocalDate toDate;

	@Min(value = 0, message = "Free days cannot be negative")
	@Schema(description = """
			Free days of this row, replacing the free days of the container / line tariff for this \
			container only. It takes precedence over the free days of the request. Leave it empty \
			or 0 to fall back on the free days of the request, and on the tariff when that one is \
			empty too.""",
			example = "7")
	private Integer freeDays;
}
