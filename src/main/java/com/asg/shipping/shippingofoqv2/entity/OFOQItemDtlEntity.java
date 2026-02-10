package com.asg.shipping.shippingofoqv2.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "OFOQ_API_DATA_DTL")
@IdClass(TransactionDetailId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OFOQItemDtlEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", nullable = false, insertable = false, updatable = false)
    private OfoqApiDataHdrEntity ofoqApiDataHdr;

    @Column(name = "VESSEL_VOYAGE_POID")
    private Long vesselVoyagePoid;

    @Column(name = "LINE_NAME", length = 500)
    private String lineName;

    @Column(name = "VESSEL_NAME", length = 500)
    private String vesselName;

    @Column(name = "VOYAGE_NO", length = 500)
    private String voyageNo;

    @Column(name = "JOB_NO", length = 500)
    private String jobNo;

    @Column(name = "ARRIVAL_DATE")
    private LocalDateTime arrivalDate;

    @Column(name = "SAIL_DATE")
    private LocalDateTime sailDate;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "DRILLDOWN_LINK_INFO", length = 1000)
    private String drilldownLinkInfo;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "CHECKED", length = 1)
    private String checked;

}