package com.asg.shipping.lineprofile.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipLineProfileContactDtlId implements Serializable {
    private Long lineProfilePoid;
    private Long detRowId;
}

