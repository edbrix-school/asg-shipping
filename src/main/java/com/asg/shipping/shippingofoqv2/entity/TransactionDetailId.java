package com.asg.shipping.shippingofoqv2.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetailId implements Serializable {
    private Long transactionPoid;
    private Long detRowId;
}