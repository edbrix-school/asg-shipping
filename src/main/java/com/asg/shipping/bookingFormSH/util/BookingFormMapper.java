package com.asg.shipping.bookingFormSH.util;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.asg.common.lib.utility.DateUtil;
import static com.asg.shipping.common.utility.DateTimeHandler.convertDate;
import static com.asg.shipping.common.utility.DateTimeHandler.convertDateTime;
import org.springframework.stereotype.Component;

import com.asg.shipping.bookingFormSH.dto.BookingFormCargoDetailDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormChargesDetailDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormContainerDetailDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormCreateDTO;
import com.asg.shipping.bookingFormSH.dto.BookingFormDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormUpdateDTO;
import com.asg.shipping.bookingFormSH.entity.ShipMateCargoDtl;
import com.asg.shipping.bookingFormSH.entity.ShipMateChargesDtl;
import com.asg.shipping.bookingFormSH.entity.ShipMateContainerDtl;
import com.asg.shipping.bookingFormSH.entity.ShipMateHdr;

/**
 * Mapper utility for converting between Entity and DTO
 */
@Component
public class BookingFormMapper {

	/**
	 * Convert Header Entity to DTO
	 */
	public BookingFormDto mapToDto(ShipMateHdr entity) {
		if (entity == null) {
			return null;
		}

		return BookingFormDto.builder().transactionPoid(entity.getTransactionPoid()).groupPoid(entity.getGroupPoid())
				.companyPoid(entity.getCompanyPoid()).docRef(entity.getDocRef())
				.transactionDate(entity.getTransactionDate()).vessalAgentName(entity.getVessalAgentName())
				.shipperPoid(entity.getShipperPoid()).shipperAddressPoid(entity.getShipperAddressPoid())
				.consigneePoid(entity.getConsigneePoid()).consigneeAddressPoid(entity.getConsigneeAddressPoid())
				.notifyPoid1(entity.getNotifyPoid1()).notifyAddressPoid1(entity.getNotifyAddressPoid1())
				.notifyPoid2(entity.getNotifyPoid2()).notifyAddressPoid2(entity.getNotifyAddressPoid2())
				.quotationTransactionPoid(entity.getQuotationTransactionPoid()).vesselPoid(entity.getVesselPoid())
				.vesselEtaDate(entity.getVesselEtaDate()).linePoid(entity.getLinePoid())
				.salesmanPoid(entity.getSalesmanPoid()).comodityPoid(entity.getComodityPoid())
				.totalVolume(entity.getTotalVolume()).totalWeight(entity.getTotalWeight())
				.unitPack(entity.getUnitPack()).totalNoOfPacks(entity.getTotalNoOfPacks())
				.placeOfRecieptPoid(entity.getPlaceOfRecieptPoid())
				.placeOfDelieveryPoid(entity.getPlaceOfDelieveryPoid()).portOfLoadingPoid(entity.getPortOfLoadingPoid())
				.portOfDischargePoid(entity.getPortOfDischargePoid()).remarks(entity.getRemarks())
				.mateStatus(entity.getMateStatus()).voyageNo(entity.getVoyageNo())
				.bookingIssueNo(entity.getBookingIssueNo()).mateLoadDate(entity.getMateLoadDate())
				.mateLoadNo(entity.getMateLoadNo()).mateLoadVoyagePoid(entity.getMateLoadVoyagePoid())
				.issueType(entity.getIssueType()).deleted(entity.getDeleted()).consigneeName(entity.getConsigneeName())
				.consigneeAddress(entity.getConsigneeAddress()).splitBookingNo(entity.getSplitBookingNo())
				.finalDestination(entity.getFinalDestination())
				.shipperDetailsManually(entity.getShipperDetailsManually()).build();
	}

	/**
	 * Map CreateDTO to Header Entity
	 */
	public void mapCreateDTOToEntity(BookingFormCreateDTO dto, ShipMateHdr entity, Long groupPoid, Long companyPoid) {
		entity.setGroupPoid(groupPoid);
		entity.setCompanyPoid(companyPoid);
		entity.setTransactionDate(dto.getTransactionDate()==null? DateUtil.getCurrentDateInUserTimeZone(): convertDate(dto.getTransactionDate().atStartOfDay()));
		entity.setVessalAgentName(dto.getVessalAgentName());
		entity.setShipperPoid(dto.getShipperPoid());
		entity.setShipperAddressPoid(dto.getShipperAddressPoid());
		entity.setConsigneePoid(dto.getConsigneePoid());
		entity.setConsigneeAddressPoid(dto.getConsigneeAddressPoid());
		entity.setNotifyPoid1(dto.getNotifyPoid1());
		entity.setNotifyAddressPoid1(dto.getNotifyAddressPoid1());
		entity.setNotifyPoid2(dto.getNotifyPoid2());
		entity.setNotifyAddressPoid2(dto.getNotifyAddressPoid2());
		entity.setQuotationTransactionPoid(dto.getQuotationTransactionPoid());
		entity.setVesselPoid(dto.getVesselPoid());
		entity.setVesselEtaDate(convertDate(dto.getVesselEtaDate().atStartOfDay()));
		entity.setLinePoid(dto.getLinePoid());
		entity.setSalesmanPoid(dto.getSalesmanPoid());
		entity.setComodityPoid(dto.getComodityPoid());
		entity.setTotalVolume(dto.getTotalVolume());
		entity.setTotalWeight(dto.getTotalWeight());
		entity.setUnitPack(dto.getUnitPack());
		entity.setTotalNoOfPacks(dto.getTotalNoOfPacks());
		entity.setPlaceOfRecieptPoid(dto.getPlaceOfRecieptPoid());
		entity.setPlaceOfDelieveryPoid(dto.getPlaceOfDelieveryPoid());
		entity.setPortOfLoadingPoid(dto.getPortOfLoadingPoid());
		entity.setPortOfDischargePoid(dto.getPortOfDischargePoid());
		entity.setRemarks(dto.getRemarks());
		entity.setMateStatus(dto.getMateStatus());
		entity.setVoyageNo(dto.getVoyageNo());
		entity.setBookingIssueNo(dto.getBookingIssueNo());
		entity.setMateLoadDate(convertDate(dto.getMateLoadDate().atStartOfDay()));
		entity.setMateLoadNo(dto.getMateLoadNo());
		entity.setMateLoadVoyagePoid(dto.getMateLoadVoyagePoid());
		entity.setIssueType(dto.getIssueType());
		entity.setConsigneeName(dto.getConsigneeName());
		entity.setConsigneeAddress(dto.getConsigneeAddress());
		entity.setSplitBookingNo(dto.getSplitBookingNo());
		entity.setFinalDestination(dto.getFinalDestination());
		entity.setShipperDetailsManually(dto.getShipperDetailsManually());

		// Set deleted flag
		entity.setDeleted("N");
	}

	/**
	 * Map UpdateDTO to Header Entity
	 */
	public void mapUpdateDTOToEntity(BookingFormUpdateDTO dto, ShipMateHdr entity) {
		if (dto.getTransactionDate() != null) {
			entity.setTransactionDate(dto.getTransactionDate());
		}
		if (dto.getVessalAgentName() != null) {
			entity.setVessalAgentName(dto.getVessalAgentName());
		}
		if (dto.getShipperPoid() != null) {
			entity.setShipperPoid(dto.getShipperPoid());
		}
		if (dto.getShipperAddressPoid() != null) {
			entity.setShipperAddressPoid(dto.getShipperAddressPoid());
		}
		if (dto.getConsigneePoid() != null) {
			entity.setConsigneePoid(dto.getConsigneePoid());
		}
		if (dto.getConsigneeAddressPoid() != null) {
			entity.setConsigneeAddressPoid(dto.getConsigneeAddressPoid());
		}
		if (dto.getNotifyPoid1() != null) {
			entity.setNotifyPoid1(dto.getNotifyPoid1());
		}
		if (dto.getNotifyAddressPoid1() != null) {
			entity.setNotifyAddressPoid1(dto.getNotifyAddressPoid1());
		}
		if (dto.getNotifyPoid2() != null) {
			entity.setNotifyPoid2(dto.getNotifyPoid2());
		}
		if (dto.getNotifyAddressPoid2() != null) {
			entity.setNotifyAddressPoid2(dto.getNotifyAddressPoid2());
		}
		if (dto.getQuotationTransactionPoid() != null) {
			entity.setQuotationTransactionPoid(dto.getQuotationTransactionPoid());
		}
		if (dto.getVesselPoid() != null) {
			entity.setVesselPoid(dto.getVesselPoid());
		}
		if (dto.getVesselEtaDate() != null) {
			entity.setVesselEtaDate(convertDate(dto.getVesselEtaDate().atStartOfDay()));
		}
		if (dto.getLinePoid() != null) {
			entity.setLinePoid(dto.getLinePoid());
		}
		if (dto.getSalesmanPoid() != null) {
			entity.setSalesmanPoid(dto.getSalesmanPoid());
		}
		if (dto.getComodityPoid() != null) {
			entity.setComodityPoid(dto.getComodityPoid());
		}
		if (dto.getTotalVolume() != null) {
			entity.setTotalVolume(dto.getTotalVolume());
		}
		if (dto.getTotalWeight() != null) {
			entity.setTotalWeight(dto.getTotalWeight());
		}
		if (dto.getUnitPack() != null) {
			entity.setUnitPack(dto.getUnitPack());
		}
		if (dto.getTotalNoOfPacks() != null) {
			entity.setTotalNoOfPacks(dto.getTotalNoOfPacks());
		}
		if (dto.getPlaceOfRecieptPoid() != null) {
			entity.setPlaceOfRecieptPoid(dto.getPlaceOfRecieptPoid());
		}
		if (dto.getPlaceOfDelieveryPoid() != null) {
			entity.setPlaceOfDelieveryPoid(dto.getPlaceOfDelieveryPoid());
		}
		if (dto.getPortOfLoadingPoid() != null) {
			entity.setPortOfLoadingPoid(dto.getPortOfLoadingPoid());
		}
		if (dto.getPortOfDischargePoid() != null) {
			entity.setPortOfDischargePoid(dto.getPortOfDischargePoid());
		}
		if (dto.getRemarks() != null) {
			entity.setRemarks(dto.getRemarks());
		}
		if (dto.getMateStatus() != null) {
			entity.setMateStatus(dto.getMateStatus());
		}
		if (dto.getVoyageNo() != null) {
			entity.setVoyageNo(dto.getVoyageNo());
		}
		if (dto.getBookingIssueNo() != null) {
			entity.setBookingIssueNo(dto.getBookingIssueNo());
		}
		if (dto.getMateLoadDate() != null) {
			entity.setMateLoadDate(convertDate(dto.getMateLoadDate().atStartOfDay()));
		}
		if (dto.getMateLoadNo() != null) {
			entity.setMateLoadNo(dto.getMateLoadNo());
		}
		if (dto.getMateLoadVoyagePoid() != null) {
			entity.setMateLoadVoyagePoid(dto.getMateLoadVoyagePoid());
		}
		if (dto.getIssueType() != null) {
			entity.setIssueType(dto.getIssueType());
		}
		if (dto.getConsigneeName() != null) {
			entity.setConsigneeName(dto.getConsigneeName());
		}
		if (dto.getConsigneeAddress() != null) {
			entity.setConsigneeAddress(dto.getConsigneeAddress());
		}
		if (dto.getSplitBookingNo() != null) {
			entity.setSplitBookingNo(dto.getSplitBookingNo());
		}
		if (dto.getFinalDestination() != null) {
			entity.setFinalDestination(dto.getFinalDestination());
		}
		if (dto.getShipperDetailsManually() != null) {
			entity.setShipperDetailsManually(dto.getShipperDetailsManually());
		}
	}

	// Detail mapping methods - Cargo DTL
	public BookingFormCargoDetailDto mapCargoDtlToDto(ShipMateCargoDtl entity) {
		if (entity == null)
			return null;
		return BookingFormCargoDetailDto.builder().detRowId(entity.getDetRowId())
				.cargoDescription(entity.getCargoDescription()).equipmentType(entity.getEquipmentType())
				.equipmentSize(entity.getEquipmentSize()).quantity(entity.getQuantity()).volume(entity.getVolume())
				.weight(entity.getWeight()).equipmentIsoType(entity.getEquipmentIsoType()).isImco(entity.getIsImco())
				.imo(entity.getImo()).isOog(entity.getIsOog()).oogL(entity.getOogL()).oogB(entity.getOogB())
				.oogH(entity.getOogH()).refferTemp(entity.getRefferTemp()).refferHum(entity.getRefferHum())
				.refferVent(entity.getRefferVent()).isRefer(entity.getIsRefer()).referType(entity.getReferType())
				.oogLW(entity.getOogLW()).oogRW(entity.getOogRW()).oogF(entity.getOogF()).oogA(entity.getOogA())
				.build();
	}

	public ShipMateCargoDtl mapCargoDtlFromDto(BookingFormCargoDetailDto dto, Long transactionPoid) {
		if (dto == null)
			return null;
		return ShipMateCargoDtl.builder().transactionPoid(transactionPoid).detRowId(dto.getDetRowId())
				.cargoDescription(dto.getCargoDescription()).equipmentType(dto.getEquipmentType())
				.equipmentSize(dto.getEquipmentSize()).quantity(dto.getQuantity()).volume(dto.getVolume())
				.weight(dto.getWeight()).equipmentIsoType(dto.getEquipmentIsoType()).isImco(dto.getIsImco())
				.imo(dto.getImo()).isOog(dto.getIsOog()).oogL(dto.getOogL()).oogB(dto.getOogB()).oogH(dto.getOogH())
				.refferTemp(dto.getRefferTemp()).refferHum(dto.getRefferHum()).refferVent(dto.getRefferVent())
				.isRefer(dto.getIsRefer()).referType(dto.getReferType()).oogLW(dto.getOogLW()).oogRW(dto.getOogRW())
				.oogF(dto.getOogF()).oogA(dto.getOogA()).build();
	}

	// Detail mapping methods - Charges DTL
	public BookingFormChargesDetailDto mapChargesDtlToDto(ShipMateChargesDtl entity) {
		if (entity == null)
			return null;
		return BookingFormChargesDetailDto.builder().detRowId(entity.getDetRowId()).chargePoid(entity.getChargePoid())
				.currencyExchange(entity.getCurrencyExchange()).quantity(entity.getQuantity())
				.perQuantityAmount(entity.getPerQuantityAmount()).paidAtPortPoid(entity.getPaidAtPortPoid())
				.buyPercharge(entity.getBuyPercharge()).currencyCode(entity.getCurrencyCode()).build();
	}

	public ShipMateChargesDtl mapChargesDtlFromDto(BookingFormChargesDetailDto dto, Long transactionPoid) {
		if (dto == null)
			return null;
		return ShipMateChargesDtl.builder().transactionPoid(transactionPoid).detRowId(dto.getDetRowId())
				.chargePoid(dto.getChargePoid()).currencyExchange(dto.getCurrencyExchange()).quantity(dto.getQuantity())
				.perQuantityAmount(dto.getPerQuantityAmount()).paidAtPortPoid(dto.getPaidAtPortPoid())
				.buyPercharge(dto.getBuyPercharge()).currencyCode(dto.getCurrencyCode()).build();
	}

	// Detail mapping methods - Container DTL
	public BookingFormContainerDetailDto mapContainerDtlToDto(ShipMateContainerDtl entity) {
		if (entity == null)
			return null;
		return BookingFormContainerDetailDto.builder().detRowId(entity.getDetRowId())
				.containerNo(entity.getContainerNo()).equipmentSealNo(entity.getEquipmentSealNo())
				.equipmentIsoType(entity.getEquipmentIsoType()).equipmentType(entity.getEquipmentType())
				.equipmentSize(entity.getEquipmentSize()).quantity(entity.getQuantity())
				.grsVolume(entity.getGrsVolume()).grsWeight(entity.getGrsWeight()).netVolume(entity.getNetVolume())
				.netWeight(entity.getNetWeight()).noOfPacks(entity.getNoOfPacks()).packUnit(entity.getPackUnit())
				.comodityPoid(entity.getComodityPoid()).destinationPortPoid(entity.getDestinationPortPoid())
				.imo(entity.getImo()).oogL(entity.getOogL()).oogB(entity.getOogB()).oogH(entity.getOogH())
				.refferTemp(entity.getRefferTemp()).refferHum(entity.getRefferHum()).refferVent(entity.getRefferVent())
				.cargoDescription(entity.getCargoDescription()).equipmentShipperOwn(entity.getEquipmentShipperOwn())
				.issueToShipper(entity.getIssueToShipper()).returnFromShipper(entity.getReturnFromShipper())
				.releaseAllocation(entity.getReleaseAllocation()).isImco(entity.getIsImco()).isOog(entity.getIsOog())
				.isRefer(entity.getIsRefer()).referType(entity.getReferType()).oogLW(entity.getOogLW())
				.oogRW(entity.getOogRW()).oogF(entity.getOogF()).oogA(entity.getOogA()).isSplit(entity.getIsSplit())
				.imcoClassType(entity.getImcoClassType()).oogType(entity.getOogType()).vgmWeight(entity.getVgmWeight())
				.vgmDocId(entity.getVgmDocId()).vgmDate(entity.getVgmDate()).vgmEdi(entity.getVgmEdi()).build();
	}

	public ShipMateContainerDtl mapContainerDtlFromDto(BookingFormContainerDetailDto dto, Long transactionPoid) {
		if (dto == null)
			return null;
		return ShipMateContainerDtl.builder().transactionPoid(transactionPoid).detRowId(dto.getDetRowId())
				.containerNo(dto.getContainerNo() != null ? dto.getContainerNo().trim().replace(" ", "") : null)
				.equipmentSealNo(dto.getEquipmentSealNo()).equipmentIsoType(dto.getEquipmentIsoType())
				.equipmentType(dto.getEquipmentType()).equipmentSize(dto.getEquipmentSize()).quantity(dto.getQuantity())
				.grsVolume(dto.getGrsVolume()).grsWeight(dto.getGrsWeight()).netVolume(dto.getNetVolume())
				.netWeight(dto.getNetWeight()).noOfPacks(dto.getNoOfPacks()).packUnit(dto.getPackUnit())
				.comodityPoid(dto.getComodityPoid()).destinationPortPoid(dto.getDestinationPortPoid()).imo(dto.getImo())
				.oogL(dto.getOogL()).oogB(dto.getOogB()).oogH(dto.getOogH()).refferTemp(dto.getRefferTemp())
				.refferHum(dto.getRefferHum()).refferVent(dto.getRefferVent())
				.cargoDescription(dto.getCargoDescription()).equipmentShipperOwn(dto.getEquipmentShipperOwn())
				.issueToShipper(dto.getIssueToShipper()).returnFromShipper(dto.getReturnFromShipper())
				.releaseAllocation(dto.getReleaseAllocation()).isImco(dto.getIsImco()).isOog(dto.getIsOog())
				.isRefer(dto.getIsRefer()).referType(dto.getReferType()).oogLW(dto.getOogLW()).oogRW(dto.getOogRW())
				.oogF(dto.getOogF()).oogA(dto.getOogA()).isSplit(dto.getIsSplit()).imcoClassType(dto.getImcoClassType())
				.oogType(dto.getOogType()).vgmWeight(dto.getVgmWeight()).vgmDocId(dto.getVgmDocId())
				.vgmDate(dto.getVgmDate()).vgmEdi(dto.getVgmEdi()).build();
	}

	// Helper methods to map lists
	public List<BookingFormCargoDetailDto> mapCargoDtlListToDto(List<ShipMateCargoDtl> entities) {
		if (entities == null)
			return null;
		return entities.stream().map(this::mapCargoDtlToDto).collect(Collectors.toList());
	}

	public List<BookingFormChargesDetailDto> mapChargesDtlListToDto(List<ShipMateChargesDtl> entities) {
		if (entities == null)
			return null;
		return entities.stream().map(this::mapChargesDtlToDto).collect(Collectors.toList());
	}

	public List<BookingFormContainerDetailDto> mapContainerDtlListToDto(List<ShipMateContainerDtl> entities) {
		if (entities == null)
			return null;
		return entities.stream().map(this::mapContainerDtlToDto).collect(Collectors.toList());
	}
}
