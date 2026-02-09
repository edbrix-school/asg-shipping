package com.asg.shipping.deliveryorderissuetocustomer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeliveryOrderIssueToCustomerPrintRequest {

    @NotBlank(message = "DO Released Id Person is required")
    @Size(max = 50, message = "DO released ID person must not exceed 50 characters")
    private String doReleasedIdPerson;

    @Size(max = 50, message = "DO released to person must not exceed 50 characters")
    private String doReleasedToPerson;

    @Size(max = 100, message = "DO released address person must not exceed 100 characters")
    private String doReleasedAddressPerson;

    private String originalBlReleaseCr;

    @NotBlank(message = "DO Priority is required")
    @Size(max = 10, message = "DO priority must not exceed 10 characters")
    private String doPriority;

    @Size(max = 500, message = "Consignee emails must not exceed 500 characters")
    private String emailsConsg;

    @Size(max = 500, message = "Notify emails must not exceed 500 characters")
    private String emailNotify;

    @Size(max = 500, message = "Other emails must not exceed 500 characters")
    private String emailsOthers;

    @Size(max = 500, message = "Additional emails must not exceed 500 characters")
    private String emailsAdditional;

    @Size(max = 500, message = "DO emails must not exceed 500 characters")
    private String emailsDo;

    @NotBlank(message = "Delivery sent to is required")
    @Size(max = 1, message = "Delivery sent to must not exceed 1 character")
    private String deliverySentTo;

    @Size(max = 50, message = "Principal DO number must not exceed 50 characters")
    private String principalDoNumber;

    @Size(max = 1, message = "DO count to consignee must not exceed 1 character")
    private String doCntToConsignee;

    @Size(max = 1, message = "DO count to notify must not exceed 1 character")
    private String doCntToNotify;

    @Size(max = 1, message = "DO count to others must not exceed 1 character")
    private String doCntToOthers;

    @Size(max = 500, message = "DO count to others mails must not exceed 500 characters")
    private String doCntToOthersMails;

    @Size(max = 500, message = "DO count to regs mails must not exceed 500 characters")
    private String doCntToRegsMails;

}
