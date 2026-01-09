package com.asg.shipping.shippingmanifestcorrector.event;

import com.asg.shipping.shippingmanifestcorrector.service.ManifestCorrectorServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ManifestCorrectorEventListener {

    private final ManifestCorrectorServiceImpl manifestCorrectorService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBlManifestSaved(ManifestCorrectorSaveEvent event) {
        log.info("ManifestCorrectorEventListener triggered for transaction: {}", event.getEntity().getTransactionPoid());
        manifestCorrectorService.callProcShipBlReprintAftSave(
                event.getEntity().getTransactionPoid(),
                event.getBlPoid()
        );
    }
}
