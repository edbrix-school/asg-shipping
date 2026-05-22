package com.asg.shipping.exportManifestUpdate.dto;

import com.asg.shipping.common.dto.LovItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Container Details
 */
@Data
public class ContainerDetailDto {
    private Long detRowId;
    private Long mateTransactionPoid;
    private String containerNo;
    private String equipmentShipperOwn;
    private String cargoDescription;
    private String equipmentSealNo;
    private String equipmentIsoType;
    private String equipmentType;
    private String equipmentSize;
    private BigDecimal quantity;
    private BigDecimal grsVolume;
    private BigDecimal grsWeight;
    private BigDecimal netVolume;
    private BigDecimal netWeight;
    private BigDecimal tareWeight;
    private BigDecimal noOfPacks;
    private String packUnit;
    private Long comodityPoid;
    private LovItem comodityDet; // LOV: COMODITY
    private Long destinationPortPoid;
    private LovItem destinationPortDet; // LOV: PORT_MASTER
    private String imo;
    private String oogL;
    private String oogB;
    private String oogH;
    private String refferTemp;
    private String refferHum;
    private String refferVent;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private LocalDate issueToConsignee;
    private LocalDate returnFromConsignee;
    private String isImco;
    private String isOog;
    private String isRefer;
    private String referType;
    private String imcoClassType;
    private String guaranteeFlag;
    private String guaranteedBy;
    private Long extraFreeDays;
    private Long extraFreeDaysPrnpls;
    private String oogLW;
    private String oogRW;
    private String oogF;
    private String oogA;
    private String oogType;
    private String imcoClassActual;
    private LocalDate displayCollectedDate;
    private BigDecimal totalDaysCollected;
    private BigDecimal totalAmountCollected;
    private String printReturnFormDefault;
    private String printDeliveryFormDefault;
    private String demDttPbleTrnsfd;
    private String hsCode;
    private String hsDescription;
    private BigDecimal amountPerDayAfterFree;
    private LocalDate actualDischargeDate;
    private String actionType;
}

