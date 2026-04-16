package com.asg.shipping.bookingformsh.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingFormChargesDetailDtoRequest extends BookingFormChargesDetailDto{

    private String actionType; // ISCREATE, ISUPDATE, ISDELETE
}
