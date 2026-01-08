package com.asg.shipping.containerinventorymovementupdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerInfoDto {
    private Long transactionPoid;
    private Long companyPoid;

    private String voyageNo;
    private String jobNo;
    private String line;
    private String vessel;
    private String arrivalDate;

    private String blNumber;
    private String blIssueType;

    private String shipperEdiName;
    private String consigneeEdiName;
    private String consignee;
    private String notify1EdiName;
    private String notify1;

    private String loadPort;
    private String freightStatus;

    private String containerNo;
    private String equipmentIsoType;

    private Long extraFreeDays;
    private Long extraFreeDaysPrnpls;

    private BigDecimal demAmount;

    private String dischargeFull;
    private String coarriDischarge;

    private String withConsigneeFull;
    private String emptyIn;
    private String emptyOut;

    private String exportPortFull;
    private String coarriLoad;
    private String loadFull;
    private String loadEmpty;

    private String mateBookingNo;
    private String exportBlNumber;
    private String doStatus;

    private BigDecimal amountPerDayAfterFree;
    private String actualDischargeDate;
}


