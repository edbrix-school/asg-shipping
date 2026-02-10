package com.asg.shipping.importmanifestbl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultValueDto {
    private String blType;
    private String salesmanPoid;
    private String cargoType;
    private String blIssueType;
    private String freightStatus;
    private String holdReason;
    private String holdCanDo;
    private String canSentQueue;
    private String bookedByPp;
    private String manuallyCanSend;
    private String allInOneFreight;
    private String issueManualInvoice;
    private String deliverySentTo;
    private String manifestEmailVerified;
    private String emailVerifiedWithSpecialC;
    private String stopUcanAlert;
    private String transactionDate;
    private String printFreightDetails;
    private String portOfDischargePoid;
    private String placeOfDeliveryPoid;
    private String blStatus;
    private String blOriginalPrint;
    private String releasedStatus;
}
