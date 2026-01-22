package com.asg.shipping.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LovItem {
    private Long poid;
    private String code;
    private String description;
    private String label;
    private Long value;
    private Integer seqNo;
}
