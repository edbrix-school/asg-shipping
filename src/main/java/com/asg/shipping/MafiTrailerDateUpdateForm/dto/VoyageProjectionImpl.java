package com.asg.shipping.MafiTrailerDateUpdateForm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class VoyageProjectionImpl implements VoyageProjection {
	private String voyageNo;
	private String jobNo;
	private Long linePoid;
	private String lineCode;
	private String lineName;
	private Long vesselPoid;
	private String vesselCode;
	private String vesselName;
}