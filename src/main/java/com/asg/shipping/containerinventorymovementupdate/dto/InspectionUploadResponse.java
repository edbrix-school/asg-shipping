package com.asg.shipping.containerinventorymovementupdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionUploadResponse {
    private String uploadId;
    private String fileName;
    private Long rowCount;
    private List<String> parsedPreview;
}


