package com.asg.shipping.importManifestUpdate.event;

import com.asg.shipping.importManifestUpdate.entity.ShipBlManifestHdr;
import org.springframework.context.ApplicationEvent;

public class BlManifestSaveEvent extends ApplicationEvent {
    private final ShipBlManifestHdr entity;
    private final Long groupPoid;
    private final Long companyPoid;
    private final String processType;

    public BlManifestSaveEvent(Object source, ShipBlManifestHdr entity, Long groupPoid, Long companyPoid, String processType) {
        super(source);
        this.entity = entity;
        this.groupPoid = groupPoid;
        this.companyPoid = companyPoid;
        this.processType = processType;
    }

    public BlManifestSaveEvent(ShipBlManifestHdr entity, Long groupPoid, Long companyPoid, String processType) {
        super(entity);
        this.entity = entity;
        this.groupPoid = groupPoid;
        this.companyPoid = companyPoid;
        this.processType = processType;
    }

    public ShipBlManifestHdr getEntity() {
        return entity;
    }

    public Long getGroupPoid() {
        return groupPoid;
    }

    public Long getCompanyPoid() {
        return companyPoid;
    }

    public String getProcessType() {
        return processType;
    }
}
