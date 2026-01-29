package com.asg.shipping.shippingofoqv2.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.Date;

@Entity
@Table(name = "OFOQ_API_RESPONSE_DTL")
@IdClass(TransactionDetailId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
//Manifest response
public class OFOQManifestResponseDtlEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", nullable = false, insertable = false, updatable = false)
    private OfoqApiDataHdrEntity ofoqApiDataHdr;

    @Column(name = "FUNCTIONAL_REF", length = 50)
    private String functionalRef;

    @Column(name = "STATUS_CODE")
    private Long statusCode;

    @Column(name = "RESPONSE_MSG", length = 4000)
    private String responseMessage;

    @Column(name = "PROCESSING_STATUS", length = 50)
    private String processingStatus;

    @Column(name = "PROV_MANIFEST_NO", length = 500)
    private String provManifestNo;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "XML_RESPONSE", length = 4000)
    private String xmlResponse;

    @Column(name = "MANIFEST_NO", length = 100)
    private String manifestNo;

    @Column(name = "MANIFEST_STATUS", length = 2000)
    private String manifestStatus;

    @Column(name = "RESPONSE_DATE")
    @Temporal(TemporalType.DATE)
    private Date responseDate;

}