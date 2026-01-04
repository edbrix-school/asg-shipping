package com.asg.shipping.portMaster.dto;

import java.util.Map;

import lombok.Data;

@Data
public class PortMasterResponse {
	private Long portPoid;
	private String portCode;
	private String portName;
	private String portName2;
	private String globalPortCode;
	private Long countryPoid;
	private Long tradelanePoid;
	private String berths;
	private Long seqno;
	private String active;
	private Map<String,Object> tradelaneDetail;
	private Map<String,Object> countryDetail;
}
