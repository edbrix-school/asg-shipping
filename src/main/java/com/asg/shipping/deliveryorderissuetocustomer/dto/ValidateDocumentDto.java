package com.asg.shipping.deliveryorderissuetocustomer.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidateDocumentDto {
    private Boolean canPrint;
    private String message;
}
