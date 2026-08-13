package com.asg.shipping.salesinvoice.event;

public record SalesInvoiceCreatedEvent(
        Long groupPoid,
        Long companyPoid,
        Long transactionPoid
) {
}
