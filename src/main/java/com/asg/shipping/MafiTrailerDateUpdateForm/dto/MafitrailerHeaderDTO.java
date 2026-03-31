package com.asg.shipping.mafitrailerdateupdateform.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MafitrailerHeaderDTO {
	private String voyageNo;
	private String jobNo;
	private Long vesselPoid;
	private Map<String,Object> vesselDetail;
	private Long linePoid;
	private Map<String,Object> lineDetail;
	private String agentReference;
	private String remarks;
}