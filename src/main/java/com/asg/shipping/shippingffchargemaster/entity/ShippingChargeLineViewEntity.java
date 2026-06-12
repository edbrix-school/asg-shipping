package com.asg.shipping.shippingffchargemaster.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "CHARGEWISE_LINES_VIEW") // replace with actual table name
@Immutable
@Getter
@NoArgsConstructor
public class ShippingChargeLineViewEntity {
    
    @Column(name = "CHARGE_POID")
    private Long chargePoid;

    @Id
    @Column(name = "LINE_POID")
    private Long linePoid;

    @Column(name = "LINE_NAME")
    private String lineName;

    @Column(name = "LINE_CODE")
    private String lineCode;
}
