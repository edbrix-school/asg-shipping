package com.asg.shipping.MafiTrailerDateUpdateForm.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private Long transactionPoid;
    private LocalDate transactionDate;
    private String docRef;
    private Long voyageTransactionPoid;
	private String voyageNo;
	private String jobNo;
	private Long vesselPoid;
	private Map<String,Object> vesselDetail;
	private Long linePoid;
	private Map<String,Object> lineDetail;
	private String agentReference;
	private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}