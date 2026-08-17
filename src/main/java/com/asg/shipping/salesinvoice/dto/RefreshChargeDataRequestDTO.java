package com.asg.shipping.salesinvoice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for refreshing charge data.
 *
 * <p>Unlike {@link LoadChargeDataRequestDTO}, this carries the grids as they currently stand in the
 * UI so the recalculation is based on what the user is actually looking at:</p>
 * <ul>
 *   <li>{@code containerDetails} - demurrage/detention is calculated from these rows, including
 *       unsaved edits to DM dates and amounts. When omitted, containers are re-read from the database.</li>
 *   <li>{@code chargesDetails} - manually added charges and user edits (selection flag, print rate)
 *       are preserved across the refresh. System-derived rows (demurrage, late collection) are
 *       always recalculated and replaced.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshChargeDataRequestDTO {

    @NotNull(message = "BL POID is required")
    private Long blPoid;

    /**
     * Optional. The BL type is resolved from SHIP_BL_MANIFEST_HDR; this is only used as a fallback
     * when the BL cannot be read. Supported values: IMPORT, EXPORT, SWITCH, CROSSTRADE.
     */
    private String blTypeInvoice;

    /** Container grid as it currently stands in the UI. Null/empty falls back to a database read. */
    private List<SalesInvoiceContainerDtlDto> containerDetails;

    /** Charge grid as it currently stands in the UI. Null/empty means nothing to preserve. */
    private List<SalesInvoiceChargesDtlDto> chargesDetails;
}
