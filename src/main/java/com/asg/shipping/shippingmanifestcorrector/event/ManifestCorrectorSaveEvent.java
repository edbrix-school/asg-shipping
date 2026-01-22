package com.asg.shipping.shippingmanifestcorrector.event;

import com.asg.shipping.shippingmanifestcorrector.entity.ShipBlReprintHdr;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ManifestCorrectorSaveEvent extends ApplicationEvent {
    private final ShipBlReprintHdr entity;
    private final Long blPoid;

    public ManifestCorrectorSaveEvent(ShipBlReprintHdr entity, Long blPoid) {
        super(entity);
        this.entity = entity;
        this.blPoid = blPoid;
    }
}

