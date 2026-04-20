package com.asg.shipping.shippingmanifestcorrector.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.asg.common.lib.security.util.UserContext.*;

/**
 * Entity class for SHIP_BL_REPRINT_HDR table
 */
@Entity
@Table(name = "SHIP_BL_REPRINT_HDR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class ShipBlReprintHdr extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "DOC_REF", length = 25)
    private String docRef;

    @Column(name = "BL_NUMBER", length = 25)
    private String blNumber;

    @Column(name = "DO_REPRINT", length = 1)
    @Builder.Default
    private String doReprint = "N";

    @Column(name = "CONTAINER_REPRINT", length = 1)
    @Builder.Default
    private String containerReprint = "N";

    @Column(name = "RETURN_REPRINT", length = 1)
    @Builder.Default
    private String returnReprint = "N";

    @Column(name = "BL_REPRINT", length = 1)
    @Builder.Default
    private String blReprint = "N";

    @Column(name = "ISSUE_TYPE", length = 20)
    private String issueType;

    @Column(name = "CONSIGNEE_POID")
    private Long consigneePoid;

    @Column(name = "NOTIFY_POID")
    private Long notifyPoid;

    @Column(name = "SHIPPER_EDI_NAME", length = 200)
    private String shipperEdiName;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "DELETED", length = 1)
    @Builder.Default
    private String deleted = "N";

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "BL_TYPE", length = 25)
    private String blType;

    @Column(name = "DEM_REFUND", length = 1)
    @Builder.Default
    private String demRefund = "N";

    @Column(name = "HOLD_CAN_DO", length = 25)
    @Builder.Default
    private String holdCanDo = "N";

    @Column(name = "HOLD_REASON", length = 100)
    private String holdReason;

    @Column(name = "PAYABLE_GL_POID")
    private Long payableGlPoid;

    @Column(name = "INCOME_GL_POID")
    private Long incomeGlPoid;

    @Column(name = "PAYING_TO", length = 100)
    private String payingTo;

    @Column(name = "DEM_PAY_TYPE", length = 25)
    private String demPayType;

    @Column(name = "DEM_CUSTOMER_POID")
    private Long demCustomerPoid;

    @Column(name = "PLACE_OF_DELIEVERY_POID")
    private Long placeOfDeliveryPoid;

    @Column(name = "PLACE_OF_RECIEPT_POID")
    private Long placeOfReceiptPoid;

    @Column(name = "BL_PLACE_RECEIPT", length = 50)
    private String blPlaceReceipt;

    @Column(name = "BL_PLACE_LOAD", length = 50)
    private String blPlaceLoad;

    @Column(name = "BL_FINAL_DESTINATION", length = 50)
    private String blFinalDestination;

    @Column(name = "BL_PLACE_DISCHARE_DESC", length = 50)
    private String blPlaceDischargeDesc;

    @Column(name = "PORT_OF_LOADING_POID")
    private Long portOfLoadingPoid;

    @Column(name = "PORT_OF_DISCHARGE_POID")
    private Long portOfDischargePoid;

    @Column(name = "VOYAGE_TRANSACTION_POID")
    private Long voyageTransactionPoid;

    @PrePersist
    protected void onCreate() {
        if (deleted == null) {
            deleted = "N";
        }
        if (doReprint == null) {
            doReprint = "N";
        }
        if (containerReprint == null) {
            containerReprint = "N";
        }
        if (returnReprint == null) {
            returnReprint = "N";
        }
        if (blReprint == null) {
            blReprint = "N";
        }
        if (demRefund == null) {
            demRefund = "N";
        }
        if (holdCanDo == null) {
            holdCanDo = "N";
        }
    }


}

