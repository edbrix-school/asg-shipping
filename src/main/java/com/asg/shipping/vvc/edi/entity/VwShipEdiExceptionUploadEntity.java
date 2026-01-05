package com.asg.shipping.vvc.edi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "VW_SHIP_EDI_EXCEPTION_UPLOAD")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VwShipEdiExceptionUploadEntity {

    @Id
    @Column(name = "PK_ID_ROW")
    private Long pkIdRow;

    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "MAP_CODE")
    private String mapCode;

    @Column(name = "EDI_MISSING_CODE")
    private String ediMissingCode;
}


