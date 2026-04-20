package com.asg.shipping.portmaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PortMasterRequest {

	@NotBlank
	private String portCode;

	@NotBlank
	private String portName;

	private String portName2;
	private String globalPortCode;

	@NotNull
	private Long countryPoid;

	@NotNull
	private Long tradelanePoid;

	private String berths;
	private Long seqno;
	private String active;

}
