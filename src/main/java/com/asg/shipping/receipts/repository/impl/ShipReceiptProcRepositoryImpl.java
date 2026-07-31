package com.asg.shipping.receipts.repository.impl;

import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.receipts.dto.ChargeDto;
import com.asg.shipping.receipts.dto.ReceiptBlAutoPopulateDto;
import com.asg.shipping.receipts.dto.TaxConfig;
import com.asg.shipping.receipts.repository.ShipReceiptProcRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ShipReceiptProcRepositoryImpl implements ShipReceiptProcRepository {

	private final EntityManager entityManager;

	@Override
	public void afterSave(Long groupPoid, Long companyPoid, Long docKeyPoid, Long detRowId, String updateType, String loginUser) {
		try {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_SHIP_BL_PAGE_SAVE_AFTER");

			query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_DET_ROW_ID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_UPDATE_TYPE", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_LOGIN_USER", String.class, ParameterMode.IN);

			query.setParameter("P_GROUP_POID", groupPoid);
			query.setParameter("P_COMPANY_POID", companyPoid);
			query.setParameter("P_DOC_KEY_POID", docKeyPoid);
			query.setParameter("P_DET_ROW_ID", detRowId);
			query.setParameter("P_UPDATE_TYPE", updateType);
			query.setParameter("P_LOGIN_USER", loginUser != null ? loginUser : "82");

			query.execute();
		} catch (Exception e) {
			log.info("Error in after save proc --------------------> {}", e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public String validateFinancialYear(Long companyPoid, LocalDate transactionDate) {
		try {
			String result = (String) entityManager.createNativeQuery(
					"SELECT FUNC_GLOB_FINANCIAL_YEAR_VALID(:companyPoid, :docDate) FROM DUAL")
					.setParameter("companyPoid", companyPoid)
					.setParameter("docDate", transactionDate)
					.getSingleResult();

			return result;
		} catch (Exception e) {
			log.error("error in validate financial year", e);
			throw new RuntimeException("Financial year validation failed", e);
		}
	}


	@Override
	public BigDecimal validateDemurrageAmount(Long blPoid,
											  String containerNo,
											  BigDecimal demAmount) {
		try {
			BigDecimal totalDemAmount = (BigDecimal) entityManager
					.createNativeQuery(
							"SELECT FUNC_RTN_DEM_BL_WISE(:blPoid, :rcptMaking) FROM DUAL")
					.setParameter("blPoid", blPoid)
					.setParameter("rcptMaking", "Y")
					.getSingleResult();

			if (totalDemAmount == null) {
				return BigDecimal.ZERO.negate();
			}
			return totalDemAmount.compareTo(demAmount) >= 0
					? demAmount
					: BigDecimal.ZERO.negate();

		} catch (Exception e) {
			log.error("error in validate demurrage amount", e);
			throw new DataAccessResourceFailureException("Unable to fetch the demurrage amount for Bl Poid " + blPoid);
		}
	}


	@Override
	public String validateTransactionPeriod(Long companyPoid, LocalDate transactionDate) {
		try {
			String result = (String) entityManager.createNativeQuery(
					"SELECT FUNC_GLOB_TRANSACTN_YEAR_VALID(:companyPoid, :transactionDate) FROM DUAL")
					.setParameter("companyPoid", companyPoid)
					.setParameter("transactionDate", transactionDate)
					.getSingleResult();
			return result;
		} catch (Exception e) {
			log.error("error in validate transaction period", e);
			throw new RuntimeException("Transaction period validation failed", e);
		}
	}



	@Override
	public BigDecimal calculateDemurrageAmount(
			Long currentTransactionPoid,
			Long blPoid,
			String containerNo,
			String containerIsoType,
			Long linePoid,
			LocalDate fromDate,
			LocalDate toDate,
			Long extraFreeDays) {

		log.info(
				"Demurrage inputs → groupPoid={}, companyPoid={}, blPoid={}, containerNo={}, isoType={}, linePoid={}, fromDate={}, toDate={}, extraFreeDays={}",
				UserContext.getGroupPoid(),
				UserContext.getCompanyPoid(),
				blPoid,
				containerNo,
				containerIsoType,
				linePoid,
				fromDate,
				toDate,
				extraFreeDays
		);


		try {
			BigDecimal result = (BigDecimal) entityManager
					.createNativeQuery("""
                SELECT FUNC_RTN_DEM_DETTN_FULL(
                    :groupPoid,
                    :companyPoid,
                    :currentTransactionPoid,
                    :blPoid,
                    :containerNo,
                    GET_CONTAINER_CODE_POID(:containerIsoType),
                    :linePoid,
                    :fromDate,
                    :toDate,
                    'DEMM',
                    :extraFreeDays
                )
                FROM DUAL
            """)
					.setParameter("groupPoid", UserContext.getGroupPoid())
					.setParameter("companyPoid", UserContext.getCompanyPoid())
					.setParameter("currentTransactionPoid",
							currentTransactionPoid != null ? currentTransactionPoid : 0)
					.setParameter("blPoid", blPoid)
					.setParameter("containerNo", containerNo)
					.setParameter("containerIsoType", containerIsoType)
					.setParameter("linePoid", linePoid)
					.setParameter("fromDate", fromDate != null ? java.sql.Date.valueOf(fromDate) : null)
					.setParameter("toDate", toDate != null ? java.sql.Date.valueOf(toDate) : null)
					.setParameter("extraFreeDays",
							extraFreeDays != null ? extraFreeDays : 0)
					.getSingleResult();

			log.info(
					"Demurrage amount calculated: {} for BL: {}, Container: {}",
					result, blPoid, containerNo
			);

			return result ;

		} catch (Exception e) {
			log.error(
					"Error calculating demurrage amount for BL: {}, Container: {}",
					blPoid, containerNo, e
			);
			throw new DataAccessResourceFailureException(
					"Unable to calculate demurrage amount", e
			);
		}
	}

	public String validateBlacklistedCustomer(String accountNo, Long bankPoid) {
		try {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_BLACK_LISTED_CUST_VAL");

			query.registerStoredProcedureParameter("P_ACCOUNT_NO", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_BANK_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

			query.setParameter("P_ACCOUNT_NO", accountNo);
			query.setParameter("P_BANK_POID", bankPoid);

			query.execute();

			return (String) query.getOutputParameterValue("P_RESULT");
		} catch (Exception e) {
			log.info("error in validate blacklisted customer {}", e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getReceiptType(Long blPoid) {
		try {
			return (String) entityManager.createNativeQuery(
					"SELECT RCPT_TYPE FROM AR_SH_BL_HDR WHERE BL_POID = :blPoid")
					.setParameter("blPoid", blPoid)
					.getSingleResult();
		} catch (Exception e) {
			log.info("error in getReceiptType {}", e.getMessage(), e);
			return null;
		}
	}

	@Override
	public ReceiptBlAutoPopulateDto autoPopulateFields(Long blPoid) {
		try {
			StoredProcedureQuery query =
					entityManager.createStoredProcedureQuery("PROC_LOV_AFTER_BRWS_300_103");

			query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_LOV_NAME", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_LOV_VALUE", String.class, ParameterMode.IN);

			query.registerStoredProcedureParameter("OUTDATA", ResultSet.class, ParameterMode.REF_CURSOR);

			query.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
			query.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
			query.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
			query.setParameter("P_DOC_ID", "300-103");
			query.setParameter("P_DOC_KEY_POID", null);
			query.setParameter("P_LOV_NAME", "IMPORTBLNUMBER");
			query.setParameter("P_LOV_VALUE", String.valueOf(blPoid));

			query.execute();

			@SuppressWarnings("unchecked")
			List<Object[]> results = query.getResultList();

			if (results == null || results.isEmpty()) {
				return null;
			}

			Object[] row = results.get(0);
			log.info("Row data - Total columns: {}", row.length);


			return ReceiptBlAutoPopulateDto.builder()
					.blPoid(blPoid)
					.companyPoid(toLong(row[0]))
					.blReleaseType(toString(row[1]))
					.originalBlReleaseType(toString(row[2]))
					.printCustomerPoid(toBigDecimal(row[3]))
					.chequeCompanyPoid(toBigDecimal(row[4]))
					.remarks(toString(row[6]))
					.build();

		} catch (Exception e) {
			log.error("Failed to auto populate BL details for BL POID: {}", blPoid, e);
			throw new DataAccessResourceFailureException(
					"Failed to auto populate BL details for BL POID: " + blPoid, e
			);
		}
	}



	@Override
	public void doCntPrintAfter(Long groupPoid, Long companyPoid, Long docKeyPoid, Long detRowId, String updateType, String reprintUser) {
		try {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_SHIP_DO_CNT_PRINT_AFTER");

			query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_DET_ROW_ID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_UPDATE_TYPE", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_REPRINT_USER", String.class, ParameterMode.IN);

			query.setParameter("P_GROUP_POID", groupPoid);
			query.setParameter("P_COMPANY_POID", companyPoid);
			query.setParameter("P_DOC_KEY_POID", docKeyPoid);
			query.setParameter("P_DET_ROW_ID", detRowId);
			query.setParameter("P_UPDATE_TYPE", updateType);
			query.setParameter("P_REPRINT_USER", reprintUser != null ? reprintUser : "82");

			query.execute();
		} catch (Exception e) {
			log.error("Error in PROC_SHIP_DO_CNT_PRINT_AFTER procedure");
		}
	}

	@Override
	public String receiptValidate(Long transactionPoid, Long companyPoid, Long userId) {
		try {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_SH_RCPT_VALIDATE");

			query.registerStoredProcedureParameter("P_TRN_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_USER", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_RETURNS", String.class, ParameterMode.OUT);

			query.setParameter("P_TRN_POID", transactionPoid);
			query.setParameter("P_COMPANY_POID", companyPoid);
			query.setParameter("P_USER", userId != null ? userId : 1L);

			query.execute();

			return (String) query.getOutputParameterValue("P_RETURNS");
		} catch (Exception e) {
			return "True";
		}
	}

	@Override
	public String glLedgerPostShRcpInv(Long groupPoid, Long companyPoid, Long userPoid, String docId, Long transactionPoid, String docRef) {
		try {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_LEDGER_POST_SH_RCPINV");

			query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_DOC_REF", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);
			query.registerStoredProcedureParameter("P_INVOICE_POID", String.class, ParameterMode.OUT);

			query.setParameter("P_LOGIN_GROUP_POID", groupPoid);
			query.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
			query.setParameter("P_LOGIN_USER_POID", userPoid);
			query.setParameter("P_DOC_ID", docId);
			query.setParameter("P_TRANSACTION_POID", transactionPoid);
			query.setParameter("P_DOC_REF", docRef);

			query.execute();

			return (String) query.getOutputParameterValue("P_STATUS");
		} catch (Exception e) {
			return "ERROR: " + e.getMessage();
		}
	}

	@Override
	public TaxConfig getDemurrageTaxInfo(Long companyPoid) {
		try {
			@SuppressWarnings("unchecked")
			List<Object[]> results = entityManager.createNativeQuery("""
				SELECT PARAMETER_VALUE,
				    (SELECT TAX_POID FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN 
				        (SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH 
				         INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD ON GTH.TRANSACTION_POID=GTD.TRANSACTION_POID 
				         WHERE SYSDATE BETWEEN PERIOD_FROM AND PERIOD_TO AND CHARGE_POID=PARAMETER_VALUE)) TAX_POID,
				    (SELECT PERCENTAGE FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN 
				        (SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH 
				         INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD ON GTH.TRANSACTION_POID=GTD.TRANSACTION_POID 
				         WHERE SYSDATE BETWEEN PERIOD_FROM AND PERIOD_TO AND CHARGE_POID=PARAMETER_VALUE)) TAX_PERCENTAGE,
				    RTN_GLOBAL_PARAMETER('1','GLOBAL_TAX_APPLICABLE','TAX',:companyPoid,'N') TAX_APPLICABLE
				FROM GLOBAL_PARAMETERS WHERE PARAMETER_KEYID_TYPE='SHDEMURRAGE'
			""")
					.setParameter("companyPoid", companyPoid)
					.getResultList();

			if (results == null || results.isEmpty()) {
				return null;
			}

			Object[] row = results.get(0);
			return TaxConfig.builder()
					.parameterValue((String)(row[0]))
					.taxPoid(toLong(row[1]))
					.percentage(toBigDecimal(row[2]))
					.taxApplicable(toString(row[3]))
					.build();
		} catch (Exception e) {
			log.error("Error fetching demurrage tax info", e);
			return null;
		}
	}

	@Override
	public String getContainerSize(String containerIsoType) {
		try {
			return (String) entityManager.createNativeQuery(
					"SELECT GET_CONTAINER_TYPE(:containerIsoType,'SIZE') FROM DUAL")
					.setParameter("containerIsoType", containerIsoType)
					.getSingleResult();
		} catch (Exception e) {
			log.error("Error fetching container size", e);
			return null;
		}
	}

	@Override
	public List<ChargeDto> getCombinedCharges(Long blPoid, Long companyPoid) {
		try {
			@SuppressWarnings("unchecked")
			List<Object[]> results = entityManager.createNativeQuery("""
					SELECT CHARGE_TYPE_APPLICABLE,CHARGE_APPLICABLE,CHARGE_CODE_POID,AMOUNT_20,AMOUNT_40,AMOUNT_OTHER,
                                                     				    (SELECT TAX_POID FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN\s
                                                     				        (SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD\s
                                                     				         ON GTH.TRANSACTION_POID=GTD.TRANSACTION_POID WHERE TO_DATE(SYSDATE) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO)\s
                                                     				         AND CHARGE_POID=CHARGE_CODE_POID)) TAX_POID,
                                                     				    (SELECT PERCENTAGE FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN\s
                                                     				        (SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD\s
                                                     				         ON GTH.TRANSACTION_POID=GTD.TRANSACTION_POID WHERE TO_DATE(SYSDATE) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO)\s
                                                     				         AND CHARGE_POID=CHARGE_CODE_POID)) TAX_PERCENTAGE,
                                                     				    RTN_GLOBAL_PARAMETER('1','GLOBAL_TAX_APPLICABLE','TAX',:companyPoid,'N') TAX_APPLICABLE
                                                     				FROM SHIP_PORT_CHARGES_HDR MCHDR\s
                                                     				INNER JOIN SHIP_PORT_CHARGES_DTL MCDTL ON MCHDR.TRANSACTION_POID=MCDTL.TRANSACTION_POID
                                                     				WHERE (SELECT TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE))\s
                                                     				       FROM SHIP_VOYAGE_HDR VHDR\s
                                                     				       INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID=BLHDR.VOYAGE_TRANSACTION_POID\s
                                                     				       WHERE BLHDR.TRANSACTION_POID=:blPoid) BETWEEN PERIOD_FROM AND PERIOD_TO
                                                     				AND NVL(CHARGE_LINE_POID,'0')='0'
                                                     				AND CHARGE_TYPE_APPLICABLE IN ('LATECOLLECTIONIMP','LATECOLLECTIONBOTH')
                                                     				AND NVL(DELETED,'N')='N'
                                                     				AND (SELECT (TO_DATE(SYSDATE)-TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE)))+1\s
                                                     				     FROM SHIP_VOYAGE_HDR VHDR\s
                                                     				     INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID=BLHDR.VOYAGE_TRANSACTION_POID\s
                                                     				     WHERE NVL(BLHDR.TRANSACTION_POID,0) NOT IN\s
                                                     				         (SELECT NVL(BL_POID,0) FROM AR_SH_RECEIPT_HDR\s
                                                     				          UNION ALL\s
                                                     				          SELECT NVL(BL_POID,0) FROM AR_SH_SALES_INVOICE_HDR WHERE NVL(INVOICE_TYPE,'xx')<>'AUTOCAN')
                                                     				     AND BLHDR.TRANSACTION_POID=:blPoid) >=\s
                                                     				    (SELECT TO_NUMBER(PARAMETER_VALUE) FROM GLOBAL_PARAMETERS WHERE PARAMETER_NAME LIKE '%SHIPLATEDOCOLLECTION%')
                                                     
                                                     				UNION ALL
                                                     
                                                     				SELECT CHARGE_TYPE_APPLICABLE,CHARGE_APPLICABLE,CHARGE_CODE_POID,AMOUNT_20,AMOUNT_40,AMOUNT_OTHER,
                                                     				    (SELECT TAX_POID FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN\s
                                                     				        (SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD\s
                                                     				         ON GTH.TRANSACTION_POID=GTD.TRANSACTION_POID WHERE TO_DATE(SYSDATE) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO)\s
                                                     				         AND CHARGE_POID=CHARGE_CODE_POID)) TAX_POID,
                                                     				    (SELECT PERCENTAGE FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN\s
                                                     				        (SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD\s
                                                     				         ON GTH.TRANSACTION_POID=GTD.TRANSACTION_POID WHERE TO_DATE(SYSDATE) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO)\s
                                                     				         AND CHARGE_POID=CHARGE_CODE_POID)) TAX_PERCENTAGE,
                                                     				    RTN_GLOBAL_PARAMETER('1','GLOBAL_TAX_APPLICABLE','TAX',:companyPoid,'N') TAX_APPLICABLE
                                                     				FROM SHIP_PORT_CHARGES_HDR MCHDR\s
                                                     				INNER JOIN SHIP_PORT_CHARGES_DTL MCDTL ON MCHDR.TRANSACTION_POID=MCDTL.TRANSACTION_POID
                                                     				WHERE (SELECT TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE))\s
                                                     				       FROM SHIP_VOYAGE_HDR VHDR\s
                                                     				       INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID=BLHDR.VOYAGE_TRANSACTION_POID\s
                                                     				       WHERE BLHDR.TRANSACTION_POID IN\s
                                                     				           (SELECT BL_POID FROM AR_SH_RECEIPT_HDR\s
                                                     				            UNION ALL\s
                                                     				            SELECT BL_POID FROM AR_SH_SALES_INVOICE_HDR WHERE NVL(INVOICE_TYPE,'xx')<>'AUTOCAN')
                                                     				       AND BLHDR.TRANSACTION_POID=:blPoid) BETWEEN PERIOD_FROM AND PERIOD_TO
                                                     				AND NVL(CHARGE_LINE_POID,'0')='0'
                                                     				AND CHARGE_TYPE_APPLICABLE IN ('REVALIDATEIMP','REVALIDATIONBOTH')
                                                     				AND NVL(DELETED,'N')='N'
                                                     				ORDER BY CHARGE_TYPE_APPLICABLE,CHARGE_APPLICABLE
			""")
					.setParameter("blPoid", blPoid)
					.setParameter("companyPoid", companyPoid)
					.getResultList();

			return mapResultsToList(results);
		} catch (Exception e) {
			log.error("Error fetching combined charges for BL: {}", blPoid, e);
			return Collections.emptyList();
		}
	}

	private List<ChargeDto> mapResultsToList(List<Object[]> results) {
		if (results == null || results.isEmpty()) {
			return Collections.emptyList();
		}

		return results.stream().map(row -> {
			ChargeDto dto = ChargeDto.builder()
				.chargeTypeApplicable(toString(row[0]))
				.chargeApplicable(toString(row[1]))
				.chargeCodePoid(toLong(row[2]))
				.amount20(toBigDecimal(row[3]))
				.amount40(toBigDecimal(row[4]))
				.amountOther(toBigDecimal(row[5]))
				.taxPoid(toLong(row[6]))
				.taxPercentage(toBigDecimal(row[7]))
				.taxApplicable(toString(row[8]))
				.build();
			return dto;
		}).collect(Collectors.toList());
	}

	@Override
	public String validateDuplicateBlReceipt(Long blPoid, String remarks) {
		try {
			return (String) entityManager.createNativeQuery(
					"SELECT FUNC_RTN_TT_REF(:blPoid, :remarks) FROM DUAL")
					.setParameter("blPoid", blPoid)
					.setParameter("remarks", remarks)
					.getSingleResult();
		} catch (Exception e) {
			log.error("Error in validateDuplicateBlReceipt", e);
			return "FALSE";
		}
	}

	@Override
	public String validateChequeDate(LocalDate chequeDate, String type) {
		try {
			return (String) entityManager.createNativeQuery("""
                SELECT DECODE(:type, 'SHPOSTCHQ', SIGN(VALID_CHQDT - :chqDate), 'SHPRECHQ', SIGN(VALID_CHQDT - :chqDate)) 
                FROM (
                    SELECT DECODE(:type, 'SHPOSTCHQ', TRUNC(SYSDATE) + TO_NUMBER(PARAMETER_VALUE), TRUNC(SYSDATE) - TO_NUMBER(PARAMETER_VALUE)) VALID_CHQDT 
                    FROM GLOBAL_PARAMETERS 
                    WHERE PARAMETER_KEYID_TYPE = :type
                )
            """)
					.setParameter("type", type)
					.setParameter("chqDate", java.sql.Date.valueOf(chequeDate))
					.getSingleResult();
		} catch (Exception e) {
			log.error("Error in validateChequeDate for type: {}", type, e);
			return "0";
		}
	}

	@Override
	public String validatePdcDateAgainstInvoice(Long blPoid, LocalDate chequeDate) {
		try {
			return (String) entityManager.createNativeQuery("""
                SELECT SIGN(:chqDate - (TRUNC(INV_DATE) + CREDIT_DAYS)) 
                FROM AR_SH_SALES_INVOICE_HDR 
                WHERE INVOICE_TYPE = 'AUTOCAN' 
                AND CUSTOMER_POID NOT IN (SELECT PARAMETER_VALUE FROM GLOBAL_PARAMETERS WHERE PARAMETER_NAME LIKE 'CAN_GL_CASHAC_SHIPPING%' AND PARAMETER_KEYID = 'SHCANCASHACT') 
                AND NVL(DELETED, 'N') = 'N' 
                AND CUSTOMER_POID IN (SELECT CUSTOMER_POID FROM SALES_CUSTOMER_MASTER WHERE CUSTOMER_TYPE IN ('PDC', 'CASH')) 
                AND BL_POID = :blPoid
            """)
					.setParameter("chqDate", java.sql.Date.valueOf(chequeDate))
					.setParameter("blPoid", blPoid)
					.getSingleResult();
		} catch (Exception e) {
			log.error("Error in validatePdcDateAgainstInvoice", e);
			return "0";
		}
	}

	@Override
	public Long getBankCompany(Long bankPoid) {
		try {
			BigDecimal companyPoid = (BigDecimal) entityManager.createNativeQuery(
					"SELECT GET_BANK_COMPANY(:bankPoid) FROM DUAL")
					.setParameter("bankPoid", bankPoid)
					.getSingleResult();
			return companyPoid != null ? companyPoid.longValue() : null;
		} catch (Exception e) {
			log.error("Error in getBankCompany", e);
			return null;
		}
	}

	@Override
	public String getGlobalParameter(String paramName, String paramKeyIdType, Long companyPoid, String defaultValue) {
		try {
			return (String) entityManager.createNativeQuery(
					"SELECT RTN_GLOBAL_PARAMETER(:groupPoid, :paramName, :paramKeyIdType, :companyPoid, :defaultValue) FROM DUAL")
					.setParameter("groupPoid", "1")
					.setParameter("paramName", paramName)
					.setParameter("paramKeyIdType", paramKeyIdType)
					.setParameter("companyPoid", companyPoid)
					.setParameter("defaultValue", defaultValue)
					.getSingleResult();
		} catch (Exception e) {
			log.error("Error in getGlobalParameter", e);
			return defaultValue;
		}
	}

	private Long toLong(Object value) {
		if (value == null) return null;
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		if (value instanceof String str && !str.isBlank()) {
			return Long.valueOf(str);
		}
		return null;
	}


	private BigDecimal toBigDecimal(Object value) {
		return value == null ? null : (BigDecimal) value;
	}

	private String toString(Object value) {
		return value == null ? null : value.toString();
	}

	@Override
	public String validateDuplicatePaymentRef(Long blPoid, String paymentReference) {
		try {
			Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM AR_SH_RECEIPT_HDR
                WHERE BL_POID <> :blPoid
                  AND UPPER(PAYMENT_REF) = UPPER(:paymentReference)
                """)
					.setParameter("blPoid", blPoid)
					.setParameter("paymentReference", paymentReference)
					.getSingleResult();

			return count.intValue() > 0 ? "TRUE" : "FALSE";

		} catch (Exception e) {
			log.error("Error in validateDuplicatePaymentRef", e);
			return "FALSE";
		}
	}
}
