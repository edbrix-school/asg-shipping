package com.asg.shipping.salesinvoice.entity;

import lombok.*;

import java.io.Serializable;

/**
 * Composite primary key for AR_SH_SALES_INVOICE_CONTNR_DTL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArShSalesInvoiceContnrDtlId implements Serializable {

    private Long transactionPoid;
    private Long detRowId;
}

