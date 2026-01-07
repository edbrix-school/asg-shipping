package com.asg.shipping.containerinventorymovementupdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelInspectionRow {

    private String containerNo;

    private Map<String, String> otherColumns;
}

