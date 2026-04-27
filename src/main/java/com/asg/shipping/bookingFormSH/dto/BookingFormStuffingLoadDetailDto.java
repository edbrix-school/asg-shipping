package com.asg.shipping.bookingFormSH.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingFormStuffingLoadDetailDto {

    private Long detRowId;
    private String containerNo;
    private String equipmentSealNo;
    private String equipmentIsoType;
    private String marks;
    private String colourCode;
    private BigDecimal weightTonnes;
    private BigDecimal qtyOfBundles;

}
