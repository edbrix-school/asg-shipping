package com.asg.shipping.shippingofoqv2.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "OFOQ_API_DATA_HDR")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfoqApiDataHdrEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "DOC_REF", length = 50)
    private String docRef;

    @Column(name = "TRANSACTION_DATE")
    private LocalDateTime transactionDate;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "ARRIVAL_DATE_FROM")
    private LocalDateTime arrivalDateFrom;

    @Column(name = "ARRIVAL_DATE_TO")
    private LocalDateTime arrivalDateTo;

    @Column(name = "VESSEL_NAME", length = 500)
    private String vesselName;

    @Column(name = "VOYAGE_NO", length = 500)
    private String voyageNo;

    @Column(name = "ARRIVAL_DATE")
    private LocalDateTime arrivalDate;

    @Column(name = "VESSEL_POID")
    private Long vesselPoid;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "API_RESPONSE_CODE", length = 500)
    private String apiResponseCode;

    @Column(name = "API_RESPONSE_MSG", length = 500)
    private String apiResponseMsg;

    @Column(name = "API_RESPONSE", length = 500)
    private String apiResponse;

    @Column(name = "API_PROVISIONAL_STATUS", length = 100)
    private String apiProvisionalStatus;

    @Column(name = "API_PROVISIONAL_MF_NO", length = 100)
    private String apiProvisionalMfNo;

    @Column(name = "ROTATION_NUMBER")
    private Long rotationNumber;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "FUNCTIONAL_REF", length = 100)
    private String functionalRef;

    @Column(name = "MANIFEST_STATUS", length = 2000)
    private String manifestStatus;

    @Column(name = "MANIFEST_NO", length = 2000)
    private String manifestNo;

    @Column(name = "MANIFEST_TYPE", length = 100)
    private String manifestType;
}