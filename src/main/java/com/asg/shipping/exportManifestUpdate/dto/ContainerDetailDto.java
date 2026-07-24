package com.asg.shipping.exportManifestUpdate.dto;

import com.asg.shipping.common.dto.LovItem;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Container Details.
 *
 * String @Size limits mirror the column widths on SHIP_BL_MANIFEST_CONTAINER_DTL.
 */
@Data
public class ContainerDetailDto {
    private Long detRowId;
    private Long mateTransactionPoid;

    @Size(max = 25, message = "Container No must not exceed 25 characters")
    private String containerNo;

    @Size(max = 1, message = "SOC (Shipper Owned) must be a single character (Y/N)")
    private String equipmentShipperOwn;

    @Size(max = 200, message = "Container Short Description must not exceed 200 characters")
    private String cargoDescription;

    @Size(max = 25, message = "Seal No must not exceed 25 characters")
    private String equipmentSealNo;

    @Size(max = 20, message = "ISO Type must not exceed 20 characters")
    private String equipmentIsoType;
    private LovItem equipmentIsoTypeDet; // LOV: CONTAINER_TYPE_MASTER (CODE)

    @Size(max = 20, message = "Equipment Type must not exceed 20 characters")
    private String equipmentType;

    @Size(max = 20, message = "Equipment Size must not exceed 20 characters")
    private String equipmentSize;
    private BigDecimal quantity;
    private BigDecimal grsVolume;
    private BigDecimal grsWeight;
    private BigDecimal netVolume;
    private BigDecimal netWeight;
    private BigDecimal tareWeight;
    private BigDecimal noOfPacks;

    @Size(max = 6, message = "Pack Unit must not exceed 6 characters")
    private String packUnit;
    private Long comodityPoid;
    private LovItem comodityDet; // LOV: COMODITY
    private Long destinationPortPoid;
    private LovItem destinationPortDet; // LOV: PORT_MASTER

    @Size(max = 200, message = "IMO must not exceed 200 characters")
    private String imo;

    @Size(max = 20, message = "OOG L must not exceed 20 characters")
    private String oogL;

    @Size(max = 20, message = "OOG B must not exceed 20 characters")
    private String oogB;

    @Size(max = 20, message = "OOG H must not exceed 20 characters")
    private String oogH;

    @Size(max = 20, message = "Reefer Temp must not exceed 20 characters")
    private String refferTemp;

    @Size(max = 20, message = "Reefer Humidity must not exceed 20 characters")
    private String refferHum;

    @Size(max = 20, message = "Reefer Vent must not exceed 20 characters")
    private String refferVent;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private LocalDate issueToConsignee;
    private LocalDate returnFromConsignee;

    @Size(max = 1, message = "Is Imco must be a single character (Y/N)")
    private String isImco;

    @Size(max = 1, message = "Is Oog must be a single character (Y/N)")
    private String isOog;

    @Size(max = 1, message = "Is Refer must be a single character (Y/N)")
    private String isRefer;

    @Size(max = 50, message = "Refer Type must not exceed 50 characters")
    private String referType;

    @Size(max = 25, message = "Imco Class Type must not exceed 25 characters")
    private String imcoClassType;
    private LovItem imcoClassTypeDet; // LOV: IMCO_CLASS (CODE)

    @Size(max = 1, message = "Guarantee Flag must be a single character (Y/N)")
    private String guaranteeFlag;

    @Size(max = 25, message = "Guaranteed By must not exceed 25 characters")
    private String guaranteedBy;
    private Long extraFreeDays;
    private Long extraFreeDaysPrnpls;

    @Size(max = 20, message = "OOG LW must not exceed 20 characters")
    private String oogLW;

    @Size(max = 20, message = "OOG RW must not exceed 20 characters")
    private String oogRW;

    @Size(max = 20, message = "OOG F must not exceed 20 characters")
    private String oogF;

    @Size(max = 20, message = "OOG A must not exceed 20 characters")
    private String oogA;

    @Size(max = 25, message = "OOG Type must not exceed 25 characters")
    private String oogType;
    private LovItem oogTypeDet; // LOV: OOG_TYPE (CODE)

    @Size(max = 100, message = "Imco Class (Actual) must not exceed 100 characters")
    private String imcoClassActual;
    private LocalDate displayCollectedDate;
    private BigDecimal totalDaysCollected;
    private BigDecimal totalAmountCollected;

    @Size(max = 25, message = "Print Return Form Default must not exceed 25 characters")
    private String printReturnFormDefault;

    @Size(max = 25, message = "Print Delivery Form Default must not exceed 25 characters")
    private String printDeliveryFormDefault;

    @Size(max = 10, message = "Dem Dtt Pble Trnsfd must not exceed 10 characters")
    private String demDttPbleTrnsfd;

    @Size(max = 100, message = "HS Code must not exceed 100 characters")
    private String hsCode;

    @Size(max = 200, message = "HS Description must not exceed 200 characters")
    private String hsDescription;
    private BigDecimal amountPerDayAfterFree;
    private LocalDate actualDischargeDate;
    private ActionType actionType;
}
