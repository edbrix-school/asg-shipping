package com.asg.shipping.vesselvoyagecreation.util;

import com.asg.common.lib.utility.DateUtil;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageResponse;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageUpsertRequest;
import com.asg.shipping.vesselvoyagecreation.entity.ShipVoyageHdrEntity;
public class VoyageMapper {

    private VoyageMapper() {
        // Utility class
    }

    public static VoyageResponse toResponse(ShipVoyageHdrEntity e, String lineCode) {
        if (e == null) return null;
        return VoyageResponse.builder()
                .transactionPoid(e.getTransactionPoid())
                .docRef(e.getDocRef())
                .jobNo(e.getJobNo())
                .voyageNo(e.getVoyageNo())
                .linePoid(e.getLinePoid())
                .lineCode(lineCode)
                .vesselPoid(e.getVesselPoid())
                .startPortPoid(e.getStartPortPoid())
                .nextPortPoid(e.getNextPortPoid())
                .lastPortPoid(e.getLastPortPoid())
                .lastTransshipPortPoid(e.getLastTransshipPortPoid())
                .destinationPortPoid(e.getDestinationPortPoid())
                .expectedDate(e.getExpectedDate())
                .arrivalDate(e.getArrivalDate())
                .berthDate(e.getBerthDate())
                .sailDate(e.getSailDate())
                .customRegno(e.getCustomRegno())
                .customRegdate(e.getCustomRegdate())
                .shippedOnboardDate(e.getShippedOnboardDate())
                .operationStartDate(e.getOperationStartDate())
                .operationEndDate(e.getOperationEndDate())
                .preArrivalMsgVessel(e.getPreArrivalMsgVessel())
                .preArrivalMsgPort(e.getPreArrivalMsgPort())
                .entryInGctos(e.getEntryInGctos())
                .entryInMarassi(e.getEntryInMarassi())
                .mscVesselVoyageReff(e.getMscVesselVoyageReff())
                .deleted(e.getDeleted())
                .build();
    }

    public static ShipVoyageHdrEntity toEntityForCreate(
            VoyageUpsertRequest req,
            Long groupPoid,
            Long companyPoid) {
        return ShipVoyageHdrEntity.builder()
                .groupPoid(groupPoid)
                .companyPoid(companyPoid)
                .transactionDate(DateUtil.getCurrentDateInUserTimeZone())
                .voyageNo(req.getVoyageNo())
                .linePoid(req.getLinePoid())
                .vesselPoid(req.getVesselPoid())
                .startPortPoid(req.getStartPortPoid())
                .nextPortPoid(req.getNextPortPoid())
                .lastPortPoid(req.getLastPortPoid())
                .lastTransshipPortPoid(req.getLastTransshipPortPoid())
                .destinationPortPoid(req.getDestinationPortPoid())
                .expectedDate(req.getExpectedDate())
                .arrivalDate(req.getArrivalDate())
                .berthDate(req.getBerthDate())
                .sailDate(req.getSailDate())
                .customRegno(req.getCustomRegno())
                .customRegdate(req.getCustomRegdate())
                .shippedOnboardDate(req.getShippedOnboardDate())
                .operationStartDate(req.getOperationStartDate())
                .operationEndDate(req.getOperationEndDate())
                .preArrivalMsgVessel(req.getPreArrivalMsgVessel())
                .preArrivalMsgPort(req.getPreArrivalMsgPort())
                .entryInGctos(req.getEntryInGctos())
                .entryInMarassi(req.getEntryInMarassi())
                .mscVesselVoyageReff(req.getMscVesselVoyageReff())
                .deleted("N")
                .build();
    }

    public static void updateEntity(ShipVoyageHdrEntity e, VoyageUpsertRequest req) {
        e.setVoyageNo(req.getVoyageNo());
        e.setLinePoid(req.getLinePoid());
        e.setVesselPoid(req.getVesselPoid());
        e.setStartPortPoid(req.getStartPortPoid());
        e.setNextPortPoid(req.getNextPortPoid());
        e.setLastPortPoid(req.getLastPortPoid());
        e.setLastTransshipPortPoid(req.getLastTransshipPortPoid());
        e.setDestinationPortPoid(req.getDestinationPortPoid());
        e.setExpectedDate(req.getExpectedDate());
        e.setArrivalDate(req.getArrivalDate());
        e.setBerthDate(req.getBerthDate());
        e.setSailDate(req.getSailDate());
        e.setCustomRegno(req.getCustomRegno());
        e.setCustomRegdate(req.getCustomRegdate());
        e.setShippedOnboardDate(req.getShippedOnboardDate());
        e.setOperationStartDate(req.getOperationStartDate());
        e.setOperationEndDate(req.getOperationEndDate());
        e.setPreArrivalMsgVessel(req.getPreArrivalMsgVessel());
        e.setPreArrivalMsgPort(req.getPreArrivalMsgPort());
        e.setEntryInGctos(req.getEntryInGctos());
        e.setEntryInMarassi(req.getEntryInMarassi());
        e.setMscVesselVoyageReff(req.getMscVesselVoyageReff());
    }
}










