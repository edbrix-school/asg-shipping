package com.asg.shipping.vesselvoyagecreation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoyageUpsertRequest {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;

    @NotNull(message = "voyageNo is required")
    @Size(max = 20, message = "voyageNo max length is 20")
    private String voyageNo;

    @NotNull(message = "linePoid is required")
    private Long linePoid;

    @NotNull(message = "vesselPoid is required")
    private Long vesselPoid;

    private Long startPortPoid;
    private Long nextPortPoid;
    private Long lastPortPoid;
    private Long lastTransshipPortPoid;
    private Long destinationPortPoid;

    @NotNull(message = "expectedDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expectedDate;

    @NotNull(message = "arrivalDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime arrivalDate;

    private LocalDateTime berthDate;

    @NotNull(message = "sailDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sailDate;

    private String customRegno;
    private LocalDateTime customRegdate;
    private LocalDateTime shippedOnboardDate;
    private LocalDateTime operationStartDate;
    private LocalDateTime operationEndDate;

    // SRS marked mandatory
    @NotNull(message = "preArrivalMsgVessel is required")
    private LocalDateTime preArrivalMsgVessel;

    @NotNull(message = "preArrivalMsgPort is required")
    private LocalDateTime preArrivalMsgPort;

    @NotNull(message = "entryInGctos is required")
    private LocalDateTime entryInGctos;

    @NotNull(message = "entryInMarassi is required")
    private LocalDateTime entryInMarassi;

    private String mscVesselVoyageReff;
}










