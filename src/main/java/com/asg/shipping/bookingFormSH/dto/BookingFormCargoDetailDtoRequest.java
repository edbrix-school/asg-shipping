package com.asg.shipping.bookingformsh.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingFormCargoDetailDtoRequest extends BookingFormCargoDetailDto {
    private String actionType; // ISCREATE, ISUPDATE, ISDELETE
}
