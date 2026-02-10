package com.asg.shipping.shippingofoqv2.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "OFOQ_API_XML_DATA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfoqApiXmlDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Column(name = "VESSEL_VOYAGE_POID")
    private Long vesselVoyagePoid;

    @Column(name = "XML_DATA", length = 4000)
    private String xmlData;

    @Column(name = "OFOQ_API_POID")
    private Long ofoqApiPoid;

    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 50)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "XML_BL_NUMBER", length = 100)
    private String xmlBlNumber;

    @Column(name = "MANIFEST_TYPE", length = 10)
    private String manifestType;
}