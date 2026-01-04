package com.asg.shipping.common.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * JPA IdClass for SHIP_LINE_MASTER_TYPE_DTL (composite PK: LINE_POID + DET_ROW_ID).
 */
public class ShipLineMasterTypeId implements Serializable {

    private Long linePoid;
    private Long detRowId;

    public ShipLineMasterTypeId() {}

    public ShipLineMasterTypeId(Long linePoid, Long detRowId) {
        this.linePoid = linePoid;
        this.detRowId = detRowId;
    }

    public Long getLinePoid() {
        return linePoid;
    }

    public void setLinePoid(Long linePoid) {
        this.linePoid = linePoid;
    }

    public Long getDetRowId() {
        return detRowId;
    }

    public void setDetRowId(Long detRowId) {
        this.detRowId = detRowId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShipLineMasterTypeId that = (ShipLineMasterTypeId) o;
        return Objects.equals(linePoid, that.linePoid) && Objects.equals(detRowId, that.detRowId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(linePoid, detRowId);
    }
}


