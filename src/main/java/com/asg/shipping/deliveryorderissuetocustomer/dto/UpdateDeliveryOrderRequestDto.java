package com.asg.shipping.deliveryorderissuetocustomer.dto;

import com.asg.shipping.deliveryorderissuetocustomer.annotation.ValidDoCntToOthers;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidDoCntToOthers
public class UpdateDeliveryOrderRequestDto {
    @NotBlank(message = "DO released ID person is required")
    @Size(max = 100, message = "DO released ID person must not exceed 100 characters")
    private String doReleasedIdPerson;

    @NotBlank(message = "DO released to person is required")
    @Size(max = 100, message = "DO released to person must not exceed 100 characters")
    private String doReleasedToPerson;

    @NotBlank(message = "DO released address person is required")
    @Size(max = 300, message = "DO released address person must not exceed 300 characters")
    private String doReleasedAddressPerson;

    @Size(max = 25, message = "Original BL release CR must not exceed 25 characters")
    private String originalBlReleaseCr;

    @Size(max = 10, message = "DO priority must not exceed 10 characters")
    private String doPriority;

    @Size(max = 1, message = "Delivery sent to must not exceed 1 character")
    private String deliverySentTo;

    @Size(max = 50, message = "Principal DO number must not exceed 50 characters")
    private String principalDoNumber;

    @Size(max = 1, message = "DO count to others must not exceed 1 character")
    private String doCntToOthers;

    @Size(max = 500, message = "DO count to others mails must not exceed 500 characters")
    private String doCntToOthersMails;

    @Size(max = 250, message = "Remarks must not exceed 250 characters")
    private String remarks;
}
