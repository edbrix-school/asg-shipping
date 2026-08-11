package com.asg.shipping.lineprofile.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineProfileDrilldownResponse {

    /**
     * Status returned by P_STATUS OUT parameter.
     * Values: "Success", "Warning …", or "ERROR : …"
     */
    private String status;

    /**
     * Rows from the OUTDATA SYS_REFCURSOR.
     * Each row contains TRANSACTION_POID, COMPANY_POID and RECORD_FETCH_TYPE.
     * Empty list when no active record is found (warning/error cases).
     */
    private List<LineProfileDrilldownRow> data;
}
