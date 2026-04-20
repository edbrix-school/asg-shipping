package com.asg.shipping.shippingofoqv2.dto;
import lombok.*;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OFOQAmendBlRequestDto {

    private Long vesselPoid;
    private String functionalReference;
    private Long transactionPoid;
    private String docReference;
    private String blNumber;

    private List<OFOQItemDtlDto> lineDetails;

    private List<OFOQRequestAmendBlDto> amendBl;
}
