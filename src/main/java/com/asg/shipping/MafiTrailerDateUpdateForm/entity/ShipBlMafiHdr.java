package com.asg.shipping.MafiTrailerDateUpdateForm.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "SHIP_BL_MAFI_HDR")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipBlMafiHdr extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "DOC_REF", unique = true)
    private String docRef;

    @Column(name = "VOYAGE_TRANSACTION_POID")
    private Long voyageTransactionPoid;

    @Column(name = "AGENT_REFERENCE", length = 200)
    private String agentReference;

    @Column(name = "REMARKS", length = 200)
    private String remarks;

    @Column(name = "DELETED", length = 10)
    private String deleted;

}