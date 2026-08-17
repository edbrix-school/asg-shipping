package com.asg.shipping.salesinvoice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SalesInvoiceCreatedListener {

    private final SalesInvoiceProcedureService procedureService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSalesInvoiceCreated(
            SalesInvoiceCreatedEvent event
    ) {
        procedureService.callProcShipBlPageSaveAfter(
                event.groupPoid(),
                event.companyPoid(),
                event.transactionPoid(),
                "ARSHRCPTPRINTUPDATE"
        );
    }

}
