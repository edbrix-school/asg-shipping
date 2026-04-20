package com.asg.shipping.vesselvoyagecreation.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "SHIP_VOYAGE_HDR")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipVoyageHdrEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "COMPANY_POID", nullable = false)
    private Long companyPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    // Populated by DB trigger SHIP_VOYAGE_HDR_TRG (DOC_REF / JOB_NO generation)
    @Column(name = "JOB_NO", length = 20)
    private String jobNo;

    @Column(name = "VOYAGE_NO", nullable = false, length = 20)
    private String voyageNo;

    @Column(name = "LINE_POID", nullable = false)
    private Long linePoid;

    @Column(name = "VESSEL_POID", nullable = false)
    private Long vesselPoid;

    @Column(name = "AGENT_POID")
    private Long agentPoid;

    @Column(name = "SAIL_DATE")
    private LocalDateTime sailDate;

    @Column(name = "START_PORT_POID")
    private Long startPortPoid;

    @Column(name = "NEXT_PORT_POID")
    private Long nextPortPoid;

    @Column(name = "LAST_PORT_POID")
    private Long lastPortPoid;

    @Column(name = "LAST_TRANSSHIP_PORT_POID")
    private Long lastTransshipPortPoid;

    @Column(name = "DESTINATION_PORT_POID")
    private Long destinationPortPoid;

    @Column(name = "EXPECTED_DATE")
    private LocalDateTime expectedDate;

    @Column(name = "BERTH_DATE")
    private LocalDateTime berthDate;

    @Column(name = "ARRIVAL_DATE")
    private LocalDateTime arrivalDate;

    @Column(name = "SHIPPED_ONBOARD_DATE")
    private LocalDateTime shippedOnboardDate;

    @Column(name = "ENTRY_DATE")
    private LocalDateTime entryDate;

    @Column(name = "CUSTOM_REGNO", length = 50)
    private String customRegno;

    @Column(name = "CUSTOM_REGDATE")
    private LocalDateTime customRegdate;

    @Column(name = "EXPECTED_DEPARTURE_DATE")
    private LocalDateTime expectedDepartureDate;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private Long currencyRate;

    @Column(name = "JOBNO_OLD", length = 20)
    private String jobnoOld;

    // Populated by DB trigger SHIP_VOYAGE_HDR_TRG
    @Column(name = "DOC_REF", length = 25)
    private String docRef;

    @Column(name = "OPERATION_START_DATE")
    private LocalDateTime operationStartDate;

    @Column(name = "OPERATION_END_DATE")
    private LocalDateTime operationEndDate;

    @Column(name = "MSC_VESSEL_VOYAGE_REFF", length = 100)
    private String mscVesselVoyageReff;

    // Set to 'Y' by DB trigger SHIP_VOYAGE_HDR_TRG when arrival date changes on update
    @Column(name = "ARRIVAL_DATE_CHANGED", length = 1)
    private String arrivalDateChanged;

    @Column(name = "PRE_ARRIVAL_MSG_VESSEL")
    private LocalDateTime preArrivalMsgVessel;

    @Column(name = "PRE_ARRIVAL_MSG_PORT")
    private LocalDateTime preArrivalMsgPort;

    @Column(name = "ENTRY_IN_GCTOS")
    private LocalDateTime entryInGctos;

    @Column(name = "ENTRY_IN_MARASSI")
    private LocalDateTime entryInMarassi;

    @Column(name = "DELETED", length = 1)
    private String deleted;

}


