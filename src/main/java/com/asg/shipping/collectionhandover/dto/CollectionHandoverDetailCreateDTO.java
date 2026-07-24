package com.asg.shipping.collectionhandover.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating collection handover detail records
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionHandoverDetailCreateDTO {

    // Optional on create: when omitted the server assigns the next DET_ROW_ID
    // (legacy auto-numbered rows via LastRowNumber++).
    private Long detRowId;

    @NotNull(message = "Currency amount is required")
    @PositiveOrZero(message = "Currency amount must be positive or zero")
    private BigDecimal currencyAmount;

    @NotNull(message = "Currency type is required")
    @Size(max = 20, message = "Currency type must not exceed 20 characters")
    private String currencyType;

    @PositiveOrZero(message = "Number of transactions must be positive or zero")
    private Integer noOfTran;

    @PositiveOrZero(message = "Cash amount must be positive or zero")
    private BigDecimal cashAmount;

    /** ISCREATED | ISUPDATED | ISDELETED */
    private String action;
}

