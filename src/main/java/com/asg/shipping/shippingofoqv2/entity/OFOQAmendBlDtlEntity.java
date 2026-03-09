package com.asg.shipping.shippingofoqv2.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "OFOQ_API_MANIFEST_BL_DTL")
@IdClass(TransactionDetailId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
//amend bl
public class OFOQAmendBlDtlEntity extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", nullable = false, insertable = false, updatable = false)
    private OfoqApiDataHdrEntity ofoqApiDataHdr;

    @Column(name = "MANIFEST_POID")
    private Long manifestPoid;

    @Column(name = "BL_NUMBER", length = 100)
    private String blNumber;

    @Column(name = "FUNCTIONAL_REF", length = 50)
    private String functionalRef;

    @Column(name = "STATUS_CODE")
    private Long statusCode;

    @Column(name = "RESPONSE_MSG", length = 4000)
    private String responseMsg;

    @Column(name = "PROCESSING_STATUS", length = 50)
    private String processingStatus;

    @Column(name = "AMENDMENT_REQUEST_NO", length = 500)
    private String amendmentRequestNo;

    @Column(name = "MANIFEST_STATUS", length = 50)
    private String manifestStatus;

    @Column(name = "XML_RESPONSE", length = 4000)
    private String xmlResponse;

    @Column(name = "MANIFEST_DOC_REF", length = 100)
    private String manifestDocRef;

    @Column(name = "DRILLDOWN_LINK_INFO", length = 250)
    private String drilldownLinkInfo;

}