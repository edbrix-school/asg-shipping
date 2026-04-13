package com.asg.shipping.lineprincipalmaster.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipLineMasterPicDtlId implements Serializable {
    private Long linePoid;
    private Long detRowId;
}
