package com.asg.shipping.MafiTrailerDateUpdateForm.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SHIP_BL_MAFI_DTL")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipBlMafiDtl {

	@EmbeddedId
	private ShipBlMafiDtlId id;

	@Column(name = "BL_POID")
	private Long blPoid;

	@Column(name = "MAFI_REF", length = 100)
	private String mafiRef;

	@Column(name = "MAFI_SIZE")
	private BigDecimal mafiSize;

	@Column(name = "MAFI_FREE_DAYS")
	private BigDecimal mafiFreeDays;

	@Column(name = "BACK_LOAD_DATE")
	private LocalDate backLoadDate;

	@Column(name = "MAFI_EMPTY_DATE")
	private LocalDate mafiEmptyDate;

	@Column(name = "REMARKS", length = 200)
	private String remarks;

	@Column(name = "CREATED_BY", length = 20)
	private String createdBy;

	@Column(name = "CREATED_DATE")
	private LocalDateTime createdDate;

	@Column(name = "LASTMODIFIED_BY", length = 20)
	private String lastModifiedBy;

	@Column(name = "LASTMODIFIED_DATE")
	private LocalDateTime lastModifiedDate;
}
