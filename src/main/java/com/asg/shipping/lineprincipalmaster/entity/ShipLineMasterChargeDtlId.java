package com.asg.shipping.lineprincipalmaster.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for ShipLineMasterChargeDtl
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipLineMasterChargeDtlId implements Serializable {
    private Long linePoid;
    private Long detRowId;
}

