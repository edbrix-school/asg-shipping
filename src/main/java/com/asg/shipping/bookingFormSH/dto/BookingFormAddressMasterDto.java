package com.asg.shipping.bookingFormSH.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingFormAddressMasterDto {

    private String contactPerson;
    private String email1;
    private String mobile;
    private String poBox;
    private String telephone;

}



