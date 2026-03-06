package com.asg.shipping.portMaster.entity;

import java.time.LocalDateTime;

import com.asg.common.lib.entity.BaseEntity;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Table(name = "SHIP_PORT_MASTER", uniqueConstraints = {
		@UniqueConstraint(name = "SHIP_PORT_MASTER_UK_PORTCODE", columnNames = "PORT_CODE"),
		@UniqueConstraint(name = "SHIP_PORT_MASTER_UK_PORTNAME", columnNames = "PORT_NAME") })
@IdClass(PortMasterId.class)
@DynamicUpdate
@Data
public class PortMaster extends BaseEntity {

	@Id
	@Column(name = "PORT_POID", nullable = false)
	private Long portPoid;

	@Id
	@Column(name = "GROUP_POID", nullable = false)
	private Long groupPoid;

	@Column(name = "PORT_CODE", nullable = false, length = 50)
	private String portCode;

	@Column(name = "PORT_NAME", nullable = false, length = 200)
	private String portName;

	@Column(name = "PORT_NAME2", length = 200)
	private String portName2;

	@Column(name = "GLOBAL_PORT_CODE", length = 50)
	private String globalPortCode;

	@Column(name = "COUNTRY_POID", nullable = false)
	private Long countryPoid;

	@Column(name = "TRADELANE_POID", nullable = false)
	private Long tradelanePoid;

	@Column(name = "BERTHS", length = 500)
	private String berths;

	@Column(name = "SEQNO")
	private Long seqno;

	@Column(name = "ACTIVE", length = 1)
	private String active;

	@Column(name = "DELETED", length = 1)
	private String deleted;

}
