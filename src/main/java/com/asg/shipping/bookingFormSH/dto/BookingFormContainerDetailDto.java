package com.asg.shipping.bookingFormSH.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.asg.shipping.common.dto.LovItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for SHIP_MATE_CONTAINER_DTL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingFormContainerDetailDto {

	private Long detRowId;
	private String containerNo;
	private String equipmentSealNo;
	private String equipmentIsoType;
//	private LovItem equipmentIsoTypeDet; // LOV data
	private String equipmentType;
//	private LovItem equipmentTypeDet; // LOV data
	private String equipmentSize;
	private LovItem equipmentSizeDet; // LOV data
	private BigDecimal quantity;
	private BigDecimal grsVolume;
	private BigDecimal grsWeight;
	private BigDecimal netVolume;
	private BigDecimal netWeight;
	private BigDecimal noOfPacks;
	private String packUnit;
	private Long comodityPoid;
	private LovItem comodityPoidDet; // LOV data
	private Long destinationPortPoid;
	private LovItem destinationPortPoidDet; // LOV data
	private String imo;
	private String oogL;
	private String oogB;
	private String oogH;
	private String refferTemp;
	private String refferHum;
	private String refferVent;
	private String cargoDescription;
	private String equipmentShipperOwn;
	private LocalDate issueToShipper;
	private LocalDate returnFromShipper;
	private String releaseAllocation;
	private String isImco;
	private String isOog;
	private String isRefer;
	private String referType;
	private String oogLW;
	private String oogRW;
	private String oogF;
	private String oogA;
	private String isSplit;
	private String imcoClassType;
	private String oogType;
	private BigDecimal vgmWeight;
	private String vgmDocId;
	private LocalDate vgmDate;
	private String vgmEdi;
	private String action; // ISCREATE, ISUPDATE, ISDELETE
}
