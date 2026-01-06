package com.asg.shipping.salesinvoice.entity;

import lombok.*;

import java.io.Serializable;

/**
 * Composite primary key for AR_SH_SALES_INVOICE_CHARG_DTL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArShSalesInvoiceChargDtlId implements Serializable {

    private Long transactionPoid;
    private Long detRowId;
}

