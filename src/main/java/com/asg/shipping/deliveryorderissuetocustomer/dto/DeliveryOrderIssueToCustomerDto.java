package com.asg.shipping.deliveryorderissuetocustomer.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOrderIssueToCustomerDto {
    private Long transactionPoid;
    private Long companyPoid;
    private String docRef;
    private LocalDate transactionDate;
    private String jobNo;
    private LocalDate arrivalDate;
    private String blNumber;
    private String line;
    private String consignee;
    private String notify;
    private Integer c20;
    private Integer c40;
    private String holdDo;
    private String doReleasedIdPerson;
    private String doReleasedToPerson;
    private String doReleasedAddrsPerson;
    private String deleted;
    private Long seqno;
    private String blReleaseTypeOffice;
    private String originalBlReleaseCr;
    private String doPriority;
    private LovGetListDto doPriorityDet;
    private String doIssueAuth;
    private Long doIssueAuthPoid;
    private LovGetListDto doIssueAuthPoidDet;
    private String doCntToConsignee;
    private String doCntToNotify;
    private String doCntToOthers;
    private String doCntToOthersMails;
    private String doEmails;
    private String deliverySentTo;
    private LovGetListDto deliverySentToDet;
    private String principalDoNumber;
    private String principalDoRequired;
    private String remarks;
    private String receiptsDocRef;
    private Long receiptsPoid;
}
