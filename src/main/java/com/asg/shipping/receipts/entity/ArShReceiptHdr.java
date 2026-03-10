package com.asg.shipping.receipts.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "AR_SH_RECEIPT_HDR")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArShReceiptHdr extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "TRANSACTION_POID")
	private Long transactionPoid;

	@Column(name = "TRANSACTION_DATE")
	private LocalDate transactionDate;

	@Column(name = "GROUP_POID")
	private Long groupPoid;

	@Column(name = "COMPANY_POID")
	private Long companyPoid;

	@Column(name = "DOC_REF")
	private String docRef;

	@Column(name = "RCPT_AMOUNT")
	private BigDecimal rcptAmount;

	@Column(name = "REMARKS")
	private String remarks;

	@Column(name = "DELETED")
	private String deleted;

	@Column(name = "DO_RELASED_ID_PERSON")
	private String doReleasedIdPerson;

	@Column(name = "DO_RELASED_TO_PERSON")
	private String doReleasedToPerson;

	@Column(name = "DO_RELASED_ADDRS_PERSON")
	private String doReleasedAddrsPerson;

	@Column(name = "BL_POID")
	private Long blPoid;

	@Column(name = "PRINT_CUSTOMER_POID")
	private Long printCustomerPoid;

	@Column(name = "BL_RELEASE_TYPE_OFFICE")
	private String blReleaseTypeOffice;

	@Column(name = "ORIGNAL_BL_RELEASE_TYPE")
	private String orignalBlReleaseType;

	@Column(name = "RCPT_TYPE")
	private String rcptType;

	@Column(name = "PRINT_STATUS")
	private String printStatus;

	@Column(name = "DOCUMENT_CMP_DIVISION_POID")
	private Long documentCmpDivisionPoid;

	@Column(name = "DOCUMENT_CMP_POID")
	private Long documentCmpPoid;

	@Column(name = "TOKEN_NUMBER")
	private Long tokenNumber;

	@Column(name = "TIN_NUMBER")
	private String tinNumber;

	@Column(name = "CREATED_INVOICE_POID")
	private Long createdInvoicePoid;

	@Column(name = "EMAIL_SENT")
	private String emailSent;

	@Column(name = "PAYMENT_REF")
	private String paymentRef;
}
