package com.asg.shipping.common.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LovResponse {
    private List<LovItem> items;
}

