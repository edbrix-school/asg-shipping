package com.asg.shipping.importmanifestupdate.event;

import com.asg.shipping.importmanifestupdate.respository.ImportManifestBlProcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
@Slf4j
public class BlManifestEventListener {

    private final ImportManifestBlProcRepository procRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBlManifestSaved(BlManifestSaveEvent event) {
        log.info("BlManifestEventListener triggered for transaction: {}", event.getEntity().getTransactionPoid());
        procRepository.processBlSaveAfter(
                event.getEntity().getTransactionPoid(),
                event.getGroupPoid(),
                event.getCompanyPoid(),
                event.getProcessType()
        );
    }
}
