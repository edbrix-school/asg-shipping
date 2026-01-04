package com.asg.shipping.terminal.containertype.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "SHIP_TERMINAL_CONTAINER_TYPE")
public class ContainerTerminalTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CONTAINER_TMNL_TYPE_POID", nullable = false)
    private Long containerTerminalTypePoid;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "CONTAINER_TMNL_TYPE_CODE", nullable = false, length = 10)
    private String containerTerminalTypeCode;

    @Column(name = "CONTAINER_TMNL_TYPE_NAME", nullable = false, length = 100)
    private String containerTerminalTypeName;

    @Column(name = "CONTAINER_TMNL_TYPE_SIZE", nullable = false)
    private String containerTerminalTypeSize;

    @Column(name = "SEQNO")
    private BigInteger seqno;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}
