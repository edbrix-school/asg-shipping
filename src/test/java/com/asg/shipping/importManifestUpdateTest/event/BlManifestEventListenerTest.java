package com.asg.shipping.importManifestUpdateTest.event;

import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestHdr;
import com.asg.shipping.importManifestUpdate.event.BlManifestEventListener;
import com.asg.shipping.importManifestUpdate.event.BlManifestSaveEvent;
import com.asg.shipping.importManifestUpdate.respository.ImportManifestBlProcRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlManifestEventListenerTest {

    @Mock
    private ImportManifestBlProcRepository procRepository;

    @InjectMocks
    private BlManifestEventListener listener;

    @Test
    void onBlManifestSaved_Success() {
        ShipBlManifestHdr entity = new ShipBlManifestHdr();
        entity.setTransactionPoid(1L);
        
        BlManifestSaveEvent event = new BlManifestSaveEvent(entity, 100L, 200L, "AUTOSUMWEIGHTPACKATE");

        listener.onBlManifestSaved(event);

        verify(procRepository).processBlSaveAfter(1L, 100L, 200L, "AUTOSUMWEIGHTPACKATE");
    }
}
