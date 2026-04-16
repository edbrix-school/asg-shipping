package com.asg.shipping.bookingformsh.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingFormContainerDetailDtoRequest extends BookingFormContainerDetailDto {

    private String actionType; // ISCREATE, ISUPDATE, ISDELETE
}
