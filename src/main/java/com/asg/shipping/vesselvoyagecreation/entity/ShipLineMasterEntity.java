package com.asg.shipping.vesselvoyagecreation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SHIP_LINE_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipLineMasterEntity {

    @Id
    @Column(name = "LINE_POID", nullable = false)
    private Long linePoid;

    @Column(name = "COMPANY_POID", nullable = false)
    private Long companyPoid;

    @Column(name = "LINE_CODE", length = 20)
    private String lineCode;
}










