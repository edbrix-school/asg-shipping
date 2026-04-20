package com.asg.shipping.lineprofile.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineProfileContactDto {
    private Long detRowId;
    private String actionType;
    private String contactName;
    private String designation;
    private String mobile;
    private String landline;
    private String emailAddress;
}

