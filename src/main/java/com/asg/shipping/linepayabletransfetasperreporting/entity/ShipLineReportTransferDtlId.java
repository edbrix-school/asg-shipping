package com.asg.shipping.linepayabletransfetasperreporting.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for SHIP_LINE_REPORT_TRANSFER_DTL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipLineReportTransferDtlId implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}
