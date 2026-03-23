package com.asg.shipping.importmanifestbl.util;


import com.asg.shipping.importmanifestupdate.dto.*;
import com.asg.shipping.importmanifestbl.dto.*;

import java.util.List;

public class ImportManifestMapper {


    public static ImportManifestBlDto mapToDto(ImportManifestBlRequestDto dto) {
        if (dto == null) {
            return null;
        }
        return ImportManifestBlDto.builder()
                .transactionPoid(dto.getTransactionPoid())
                .docId(dto.getDocRef())

                .vesselVoyagePoid(dto.getVoyageTransactionPoid())
                .blNumber(dto.getBlNumber())
                .blType(dto.getBlType())
                .cargo(dto.getCargoType())
                .originalBlsCount(dto.getNoOfOrgnlBls())
                .freight(dto.getFreightStatus())
                .quotationPoid(dto.getQuotationTransactionPoid())
                .salesmanPoid(dto.getSalesmanPoid())
                .issueType(dto.getBlIssueType())
                .bookedByPrincipal(dto.getBookedByPp())
                .freightAllInOne(dto.getAllInOneFreight())
                .remarks(dto.getRemarks())

                .shipperName(dto.getShipperEdiName())
                .shipperAddress(dto.getShipperEdiAddress())
                .consigneeName(dto.getConsigneeEdiName())
                .consigneePoid(dto.getConsigneePoid())
                .manifestEmailVerified(dto.getManifestEmailVerified())
                .noRecentShipmentVerified(dto.getEmailVerifiedWithSpecialC())
                .bookingCustomerPoid(dto.getBookingPartyPoid())
                .ediAddress(dto.getConsigneeEdiAddress())

                .preCarried(dto.getPreCarriedBy())
                .loadPortPoid(dto.getPortOfLoadingPoid())
                .receiptPortPoid(dto.getPlaceOfRecieptPoid())
                .deliveryPortPoid(dto.getPlaceOfDelieveryPoid())
                .dischargePortPoid(dto.getPortOfDischargePoid())
                .commodityPoid(dto.getComodityPoid())
                .grossWeight(dto.getTotalWeight())
                .cbm(dto.getTotalNetVolume())
                .weight(dto.getWeightUnit())
                .packTypes(dto.getUnitPack())
                .numberOfPacks(dto.getTotalNoOfPacks())
                .notifyName(dto.getNotify1EdiName())
                .notify1Poid(dto.getNotifyPoid1())
                .notifyEdiAddress(dto.getNotify1EdiAddress())
                .addressDetailsConsignee(mapToAddressDetailsByType(dto.getNotifyParties(), "CN"))
                .addressDetailsNotify1(mapToAddressDetailsByType(dto.getNotifyParties(), "N1"))

                .holdCanAuto(dto.getHoldCanDo())
                .manualCanSend(dto.getManuallyCanSend())
                .holdTypePoid(dto.getHoldReason())
                .holdRemarks(dto.getHoldRemarks())

                .descriptionsAndMarks(mapToDescriptionAndMarks(dto.getCargoDescriptions()))
                .mafiDetails(mapToMafiDetails(dto.getMafiDetails()))
//                .otherNotifies()
//                .generalCargoDetails(mapToGeneralCargoDetails(dto.getGeneralCargoDetails()))
//                .containers(mapToContainers(dto.getContainers()))
//                .charges()
//                .otherCharges()
//                .partBls()

                .build();
    }

    private static List<AddressDetailsDto> mapToAddressDetails(List<NotifyPartyRequestDto> notifyParties) {
        return mapToAddressDetailsByType(notifyParties, "CN");
    }

    private static List<AddressDetailsDto> mapToAddressDetailsByType(List<NotifyPartyRequestDto> notifyParties, String addressType) {
        if (notifyParties == null || notifyParties.isEmpty()) {
            return List.of();
        }

        return notifyParties.stream()
                .filter(party -> addressType.equalsIgnoreCase(party.getAddressType()))
                .map(party -> AddressDetailsDto.builder()
                        .detRowId(party.getDetRowId())
                        .preferredCommunicationPoid(party.getAddressPoid())
                        .email1(party.getEmail1())
                        .email2(party.getEmail2())
                        .build())
                .toList();
    }

//    private static List<ContainerDto> mapToContainers(List<ContainerRequestDto> containers) {
//        if (containers == null || containers.isEmpty()) {
//            return List.of();
//        }
//
//        return containers.stream()
//                .map(dto -> ContainerDto.builder()
//                        .detRowId(dto.getDetRowId())
//                        .socType(dto.get)
//                        .containerNumber()
//                        .sealNumber()
//                        .isoTypePoid()
//                        .shortDescription()
//                        .commodityPoid()
//                        .cbm()
//                        .grossWeight()
//                        .netWeight()
//                        .tareWeight()
//                        .packs()
//                        .packsType()
//                        .hsCode()
//                        .hsDescription()
//                        .customerDays()
//                        .principalDays()
//                        .imco()
//                        .reefer()
//                        .oog()
//                        .grantFlag()
//                        .grantBy()
//                        .amountPerDayAfterFree()
//                        .actualDischargeDate()
//                        .emptyDate()
//                        .collectionDate()
//                        .collectionAmount()
//                        .collectionDays()
//                        .imcoTypePoid()
//                        .imcoNumber()
//                        .imcoClassDescription()
//                        .rfType()
//                        .rfHumidity()
//                        .rfVent()
//                        .rfTemperature()
//                        .oogTypePoid()
//                        .oogBack()
//                        .oogLeftWidth()
//                        .oogRightWidth()
//                        .oogHeight()
//                        .oogLength()
//                        .oogAdditional()
//                        .oogFront()
//                        .actionType()
//                        .build()
//                );
//    }

    private static List<GeneralCargoDto> mapToGeneralCargoDetails(
            List<GeneralCargoRequestDto> cargoDescriptions) {

        if (cargoDescriptions == null || cargoDescriptions.isEmpty()) {
            return List.of();
        }

        return cargoDescriptions.stream()
                .map(dto -> GeneralCargoDto.builder()
                        .detRowId(dto.getDetRowId())
                        .description(dto.getCargoDescription())
                        .commodityPoid(dto.getComodityPoid())
                        .volume(dto.getGrsVolume())
                        .grossWeight(dto.getGrsWeight())
                        .netWeight(dto.getNetWeight())
                        .tareWeight(dto.getTareWeight())
                        .packs(dto.getNoOfPacks())
                        .unit(dto.getPackUnit())
                        .quantity(dto.getQuantity())
                        .destinationPortPoid(dto.getDestinationPortPoid())
                        .actionType(dto.getActionType())
                        .build())
                .toList();
    }

    private static List<MafiDetailsDto> mapToMafiDetails(List<MafiRequestDto> mafiDetails) {
        if (mafiDetails == null || mafiDetails.isEmpty()) {
            return List.of();
        }

        return mafiDetails.stream()
                .map(dto -> MafiDetailsDto.builder()
                        .detRowId(dto.getDetRowId())
                        .mafiReferenceNumber(dto.getMafiRef())
                        .mafiSize(dto.getMafiSize())
                        .mafiFreeDays(dto.getMafiFreeDays())
                        .remarks(dto.getRemarks())
                        .build())
                .toList();
    }


    private static DescriptionAndMarksDto mapToDescriptionAndMarks(List<CargoDescriptionRequestDto> cargoDescriptions) {
        CargoDescriptionRequestDto cargoDescriptionRequestDto = cargoDescriptions.get(0);
        return DescriptionAndMarksDto.builder()
                .marksDescription(cargoDescriptionRequestDto.getDescriptionType())
                .cargoDescription(cargoDescriptionRequestDto.getCargoDescription())
                .build();

    }
}