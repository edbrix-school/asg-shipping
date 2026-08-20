package com.asg.shipping.demurrageenquiryblwise.repository.impl;

import com.asg.shipping.demurrageenquiryblwise.dto.ContainerDemurrageCalcDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageChargeConfigDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageEnquiryContainerDto;
import com.asg.shipping.demurrageenquiryblwise.dto.ManifestChargeRowDto;
import com.asg.shipping.demurrageenquiryblwise.dto.PortChargeRowDto;
import com.asg.shipping.demurrageenquiryblwise.repository.DemurrageEnquiryBlWiseRepository;
import com.asg.common.lib.security.util.UserContext;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.OracleTypes;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DemurrageEnquiryBlWiseRepositoryImpl implements DemurrageEnquiryBlWiseRepository {

	/**
	 * The enquiry never creates a transaction, so no receipt / invoice has to be excluded from the
	 * already billed demurrage periods.
	 */
	private static final long NO_TRANSACTION = 0L;

	/**
	 * {@code PROC_SH_DEM_DTTN_CONTAINER} covers both halves of the calculation: import demurrage
	 * ({@code DEMM}, from the arrival date) and export detention ({@code DETN}, from the stuffing
	 * move). Calling it keeps this screen and the sales invoice on one copy of the rule.
	 */
	private static final String CALCULATE_DEMURRAGE_CALL =
			"{call PROC_SH_DEM_DTTN_CONTAINER(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";

	private final EntityManager entityManager;
	private final JdbcTemplate jdbcTemplate;

	@Override
	public boolean blExists(Long blPoid) {
		Number count = (Number) entityManager.createNativeQuery("""
				SELECT COUNT(1)
				FROM SHIP_BL_MANIFEST_HDR
				WHERE TRANSACTION_POID = :blPoid
				""")
				.setParameter("blPoid", blPoid)
				.getSingleResult();
		return count != null && count.longValue() > 0;
	}

	@Override
	public List<DemurrageEnquiryContainerDto> loadContainers(Long blPoid) {
		try {
			String sql = """
					SELECT BL_POID,
					       EQUIPMENT_SHIPPER_OWN,
					       CONTAINER_NO,
					       NVL(DM_TILL_DATE,FROMDATE) FMDATE,
					       NVL(DM_TILL_DATE,TODATE) TODATE,
					       (TO_DATE(NVL(DM_TILL_DATE,TODATE)) - NVL(DM_TILL_DATE,FROMDATE)) + 1 DAYS,
					       FUNC_RTN_DEM_DETTN_FULL(
					           GROUP_POID,
					           COMPANY_POID,
					           :currentTransactionPoid,
					           BL_POID,
					           CONTAINER_NO,
					           GET_CONTAINER_CODE_POID(EQUIPMENT_ISO_TYPE),
					           LINE_POID,
					           TO_DATE(ARRIVAL_DATE),
					           TO_DATE(NVL(DM_TILL_DATE,TODATE)),
					           'DEMM',
					           NVL(EXTRA_FREE_DAYS,0)
					       ) DM_AMT,
					       EQUIPMENT_ISO_TYPE,
					       FREE_DAYS,
					       EMPTY_IN
					FROM (
					    SELECT TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE)) ARRIVAL_DATE,
					           BLHDR.GROUP_POID,
					           BLHDR.COMPANY_POID,
					           EQUIPMENT_ISO_TYPE,
					           VHDR.LINE_POID,
					           EXTRA_FREE_DAYS,
					           CONTAINERDTL.TRANSACTION_POID BL_POID,
					           EQUIPMENT_SHIPPER_OWN,
					           CONTAINER_NO,
					           TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE))
					             + DECODE(NVL(EXTRA_FREE_DAYS,0),0,FREE_DAYS,NVL(EXTRA_FREE_DAYS,0)) FROMDATE,
					           CASE
					               WHEN (TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE))
					                     + DECODE(NVL(EXTRA_FREE_DAYS,0),0,FREE_DAYS,NVL(EXTRA_FREE_DAYS,0)))
					                    <= TO_DATE(SYSDATE)
					               THEN TO_DATE(SYSDATE)
					               ELSE (TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE))
					                     + DECODE(NVL(EXTRA_FREE_DAYS,0),0,FREE_DAYS,NVL(EXTRA_FREE_DAYS,0)))
					           END TODATE,
					           (
					               SELECT MAX(DM_TO_DATE) + 1
					               FROM VW_AR_SH_CONTAINER_DEMG_DTTN ARCONTAINERDTL
					               WHERE ARCONTAINERDTL.BL_POID = CONTAINERDTL.TRANSACTION_POID
					                 AND ARCONTAINERDTL.CONTAINER_NO = CONTAINERDTL.CONTAINER_NO
					                 AND ARCONTAINERDTL.TRANSACTION_POID <> :currentTransactionPoid
					           ) DM_TILL_DATE,
					           DECODE(NVL(EXTRA_FREE_DAYS,0),0,FREE_DAYS,NVL(EXTRA_FREE_DAYS,0)) FREE_DAYS,
					           (
					               SELECT TO_DATE(TRUNC(MOVES_DATE_TIME))
					               FROM SHIP_CONTAINER_INVENTORY
					               WHERE MOVES_TYPE = 'MTIN'
					                 AND LINK_TRANSACTION_POID = BLHDR.TRANSACTION_POID
					                 AND CONTAINER_NO = CONTAINERDTL.CONTAINER_NO
					           ) EMPTY_IN
					    FROM SHIP_VOYAGE_HDR VHDR
					    INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR
					        ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID
					    INNER JOIN SHIP_BL_MANIFEST_CONTAINER_DTL CONTAINERDTL
					        ON CONTAINERDTL.TRANSACTION_POID = BLHDR.TRANSACTION_POID
					    INNER JOIN SHIP_LINE_TARIFF_HDR SHLNTFHDR
					        ON SHLNTFHDR.LINE_POID = VHDR.LINE_POID
					       AND TO_DATE(TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE)))
					           BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO)
					    INNER JOIN SHIP_LINE_TARIFF_IMP_DTL CONTAINERTRIFIMP
					        ON SHLNTFHDR.TRANSACTION_POID = CONTAINERTRIFIMP.TRANSACTION_POID
					       AND CONTAINERTRIFIMP.CONTAINER_TYPE_POID =
					           GET_CONTAINER_CODE_POID(CONTAINERDTL.EQUIPMENT_ISO_TYPE)
					    WHERE BLHDR.TRANSACTION_POID = :blPoid
					)
					ORDER BY CONTAINER_NO
					""";

			@SuppressWarnings("unchecked")
			List<Object[]> rows = entityManager.createNativeQuery(sql)
					.setParameter("blPoid", blPoid)
					.setParameter("currentTransactionPoid", NO_TRANSACTION)
					.getResultList();

			long serialNumber = 0;
			List<DemurrageEnquiryContainerDto> containers = new java.util.ArrayList<>();
			for (Object[] row : rows) {
				containers.add(DemurrageEnquiryContainerDto.builder()
						.detRowId(++serialNumber)
						.blPoid(toLong(row[0]))
						.containerSocYn(toString(row[1]))
						.containerNo(toString(row[2]))
						.dmFrmDate(toLocalDate(row[3]))
						.dmToDate(toLocalDate(row[4]))
						.dmDays(toLong(row[5]))
						.dmChargeAmt(toBigDecimal(row[6]))
						.dmChargeAmtBeforeDiscount(toBigDecimal(row[6]))
						.equipmentIsoType(toString(row[7]))
						.freeDays(toLong(row[8]))
						.emptyIn(toLocalDate(row[9]))
						.build());
			}
			return containers;
		} catch (Exception e) {
			log.error("Failed to load demurrage containers for BL POID: {}", blPoid, e);
			throw new DataAccessResourceFailureException(
					"Failed to load container demurrage details for BL POID: " + blPoid, e);
		}
	}

	@Override
	public String findDoStatus(Long blPoid) {
		try {
			return (String) entityManager.createNativeQuery("""
					SELECT NVL((SELECT MAX('DOISSUED')
					            FROM DO_SH_PRINTING_DTL
					            WHERE NVL(DO_PRINTED,'N') = 'Y'
					              AND TRANSACTION_POID = :blPoid), 'DONOTISSUED')
					FROM DUAL
					""")
					.setParameter("blPoid", blPoid)
					.getSingleResult();
		} catch (Exception e) {
			log.error("Failed to read the DO status for BL POID: {}", blPoid, e);
			return null;
		}
	}

	@Override
	public ContainerDemurrageCalcDto calculateContainerDemurrage(Long blPoid, String containerNo, LocalDate toDate,
																 Integer freeDays) {
		try {
			return jdbcTemplate.execute(CALCULATE_DEMURRAGE_CALL, (CallableStatement cs) -> {
				cs.setLong(1, orZero(UserContext.getGroupPoid()));
				cs.setLong(2, orZero(UserContext.getCompanyPoid()));
				cs.setLong(3, orZero(UserContext.getUserPoid()));
				// The enquiry has no document of its own, the BL is the key of the screen.
				cs.setNull(4, Types.VARCHAR);
				// 0, never null: the procedure excludes the already billed periods with
				// TRANSACTION_POID <> ?, and <> NULL matches nothing - the enquiry would then bill
				// every container from its arrival date again.
				cs.setLong(5, NO_TRANSACTION);
				cs.setString(6, String.valueOf(blPoid));
				cs.setString(7, containerNo);
				if (toDate != null) {
					cs.setDate(8, Date.valueOf(toDate));
				} else {
					cs.setNull(8, Types.DATE);
				}
				cs.registerOutParameter(9, OracleTypes.CURSOR);
				cs.setString(10, "N");
				cs.setInt(11, overrideFreeDays(freeDays));

				cs.execute();

				try (ResultSet rs = (ResultSet) cs.getObject(9)) {
					if (rs == null || !rs.next()) {
						return null;
					}

					ContainerDemurrageCalcDto calc = ContainerDemurrageCalcDto.builder()
							.fromDate(toLocalDate(rs.getDate("FMDATE")))
							.toDate(toLocalDate(rs.getDate("TODATE")))
							.days(toLong(rs.getBigDecimal("DAYS")))
							.amount(rs.getBigDecimal("DM_AMT"))
							.build();

					// A line tariff that holds one row per slab makes the procedure return the
					// container once per slab; every row carries the same period and amount.
					if (rs.next()) {
						log.warn("PROC_SH_DEM_DTTN_CONTAINER returned more than one row for BL {}, "
								+ "container {} - the first row is used", blPoid, containerNo);
					}

					return calc;
				}
			});
		} catch (Exception e) {
			log.error("Failed to calculate demurrage for BL POID: {}, container: {}", blPoid, containerNo, e);
			throw new DataAccessResourceFailureException(
					"Failed to calculate demurrage for container " + containerNo, e);
		}
	}

	/**
	 * The procedure reads 0 as "use the free days of the container / line tariff"; only a positive
	 * value replaces them.
	 */
	private static int overrideFreeDays(Integer freeDays) {
		return freeDays != null && freeDays > 0 ? freeDays : 0;
	}

	/**
	 * The procedure accepts the login POIDs but never reads them - it takes the group and the company
	 * from the BL itself - so a missing user context must not fail the calculation.
	 */
	private static long orZero(Long poid) {
		return poid != null ? poid : 0L;
	}

	@Override
	public Map<String, String> findContainerDemurrageRemarks(Long blPoid, LocalDate toDate, Integer freeDays) {
		try {
			String sql = """
					SELECT CONTAINER_NO,
					       FUNC_RTN_DEM_DETTN_FULL_TEXT(
					           GROUP_POID,
					           COMPANY_POID,
					           :currentTransactionPoid,
					           BL_POID,
					           CONTAINER_NO,
					           GET_CONTAINER_CODE_POID(EQUIPMENT_ISO_TYPE),
					           LINE_POID,
					           TO_DATE(ARRIVAL_DATE),
					           TO_DATE(NVL(EMPTY_IN,TODATE)),
					           'DEMM',
					           NVL(EXTRA_FREE_DAYS,0)
					       ) REMARKS
					FROM (
					    SELECT TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE)) ARRIVAL_DATE,
					           BLHDR.GROUP_POID,
					           BLHDR.COMPANY_POID,
					           EQUIPMENT_ISO_TYPE,
					           VHDR.LINE_POID,
					           DECODE(NVL(:freeDays,0),0,NVL(EXTRA_FREE_DAYS,0),:freeDays) EXTRA_FREE_DAYS,
					           CONTAINERDTL.TRANSACTION_POID BL_POID,
					           CONTAINER_NO,
					           :toDate TODATE,
					           (
					               SELECT TO_DATE(TRUNC(MOVES_DATE_TIME))
					               FROM SHIP_CONTAINER_INVENTORY
					               WHERE MOVES_TYPE = 'MTIN'
					                 AND LINK_TRANSACTION_POID = BLHDR.TRANSACTION_POID
					                 AND CONTAINER_NO = CONTAINERDTL.CONTAINER_NO
					           ) EMPTY_IN
					    FROM SHIP_VOYAGE_HDR VHDR
					    INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR
					        ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID
					    INNER JOIN SHIP_BL_MANIFEST_CONTAINER_DTL CONTAINERDTL
					        ON CONTAINERDTL.TRANSACTION_POID = BLHDR.TRANSACTION_POID
					    INNER JOIN SHIP_LINE_TARIFF_HDR SHLNTFHDR
					        ON SHLNTFHDR.LINE_POID = VHDR.LINE_POID
					       AND TO_DATE(TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE)))
					           BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO)
					    INNER JOIN SHIP_LINE_TARIFF_IMP_DTL CONTAINERTRIFIMP
					        ON SHLNTFHDR.TRANSACTION_POID = CONTAINERTRIFIMP.TRANSACTION_POID
					       AND CONTAINERTRIFIMP.CONTAINER_TYPE_POID =
					           GET_CONTAINER_CODE_POID(CONTAINERDTL.EQUIPMENT_ISO_TYPE)
					    WHERE BLHDR.TRANSACTION_POID = :blPoid
					)
					""";

			@SuppressWarnings("unchecked")
			List<Object[]> rows = entityManager.createNativeQuery(sql)
					.setParameter("blPoid", blPoid)
					.setParameter("toDate", toDate != null ? Date.valueOf(toDate) : null)
					.setParameter("freeDays", freeDays != null && freeDays > 0 ? freeDays : 0)
					.setParameter("currentTransactionPoid", NO_TRANSACTION)
					.getResultList();

			Map<String, String> remarks = new java.util.HashMap<>();
			for (Object[] row : rows) {
				String containerNo = toString(row[0]);
				String text = toString(row[1]);
				if (containerNo != null && text != null && !text.isBlank()) {
					// The tariff can hold one row per slab, so the same container arrives repeatedly
					// with the same text.
					remarks.putIfAbsent(containerNo, text.trim());
				}
			}
			return remarks;
		} catch (Exception e) {
			// The breakdown is an explanation of the amount, not the amount - the enquiry is still
			// usable without it.
			log.error("Failed to read the demurrage breakdown for BL POID: {}", blPoid, e);
			return Map.of();
		}
	}

	@Override
	public List<ManifestChargeRowDto> findManifestCharges(Long blPoid) {
		try {
			String sql = """
					SELECT CHARGEDTL.TRANSACTION_POID BL_POID,
					       CHARGEDTL.CHARGE_POID,
					       CHARGEDTL.DET_ROW_ID,
					       ROUND(NVL(CHARGEDTL.CURRENCY_EXCHANGE,1)
					           * NVL(CHARGEDTL.QUANTITY,1)
					           * NVL(CHARGEDTL.PER_QUANTITY_AMOUNT,0), 3) AMOUNT,
					       DECODE(CHARGEDTL.EDI_CHARGE_CODE,
					              'ADDFROMRECEIPT','Y',
					              'ADDFROMINVOICE','Y',
					              'N') CHARGE_NEW_RECORD,
					       CHARGEDTL.INVOICE_TYPE,
					       CHARGEDTL.TAX_POID,
					       CHARGEDTL.TAX_PERCENTAGE,
					       CHARGEDTL.TAX_AMOUNT,
					       (ROUND(NVL(CHARGEDTL.CURRENCY_EXCHANGE,1)
					            * NVL(CHARGEDTL.QUANTITY,1)
					            * NVL(CHARGEDTL.PER_QUANTITY_AMOUNT,0), 3)
					        + NVL(CHARGEDTL.TAX_AMOUNT,0)) TOTAL_RCPAMOUNT
					FROM SHIP_VOYAGE_HDR VHDR
					INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR
					    ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID
					INNER JOIN SHIP_BL_MANIFEST_CHARGES_DTL CHARGEDTL
					    ON CHARGEDTL.TRANSACTION_POID = BLHDR.TRANSACTION_POID
					WHERE FREIGHT_TYPE = DECODE(BLHDR.BL_TYPE,
					                            'IMPORT','C',
					                            'SWITCH','E',
					                            'CROSSTRADE','E',
					                            'XX')
					  AND CHARGEDTL.AR_SH_RECEIPT_TRANSACTION_POID IS NULL
					  AND CHARGEDTL.RECEIPT_INVOICE_POID IS NULL
					  AND BLHDR.TRANSACTION_POID = :blPoid
					  AND (CHARGEDTL.TRANSACTION_POID,
					       CHARGEDTL.DET_ROW_ID,
					       CHARGEDTL.CHARGE_POID) NOT IN (
					        SELECT NVL(ARSHRCPCHD.BL_POID,0),
					               NVL(ARSHRCPCHD.CHARGES_DET_ROW_ID,0),
					               NVL(ARSHRCPCHD.CHARGE_POID,0)
					        FROM AR_SH_RECEIPT_HDR ARSHRCP,
					             AR_SH_RECEIPT_CHARGES_DTL ARSHRCPCHD
					        WHERE ARSHRCP.TRANSACTION_POID = ARSHRCPCHD.TRANSACTION_POID
					          AND NVL(ARSHRCP.DELETED,'N') = 'N'

					        UNION ALL

					        SELECT NVL(ARSHINVCHD.BL_POID,0),
					               NVL(ARSHINVCHD.CHARGES_DET_ROW_ID,0),
					               NVL(ARSHINVCHD.CHARGE_POID,0)
					        FROM AR_SH_SALES_INVOICE_HDR ARSHINV,
					             AR_SH_SALES_INVOICE_CHARG_DTL ARSHINVCHD
					        WHERE ARSHINV.TRANSACTION_POID = ARSHINVCHD.TRANSACTION_POID
					          AND NVL(ARSHINV.DELETED,'N') = 'N'
					          AND NVL(ARSHINV.INVOICE_TYPE,'xx') <> 'AUTOCAN'
					  )
					ORDER BY CHARGEDTL.DET_ROW_ID
					""";

			@SuppressWarnings("unchecked")
			List<Object[]> rows = entityManager.createNativeQuery(sql)
					.setParameter("blPoid", blPoid)
					.getResultList();

			return rows.stream()
					.map(row -> ManifestChargeRowDto.builder()
							.blPoid(toLong(row[0]))
							.chargePoid(toLong(row[1]))
							.detRowId(toLong(row[2]))
							.amount(toBigDecimal(row[3]))
							.chargeNewRecord(toString(row[4]))
							.invoiceType(toString(row[5]))
							.taxPoid(toLong(row[6]))
							.taxPercentage(toBigDecimal(row[7]))
							.taxAmount(toBigDecimal(row[8]))
							.totalAmount(toBigDecimal(row[9]))
							.build())
					.toList();
		} catch (Exception e) {
			log.error("Failed to load BL manifest charges for BL POID: {}", blPoid, e);
			throw new DataAccessResourceFailureException(
					"Failed to load BL manifest charges for BL POID: " + blPoid, e);
		}
	}

	@Override
	public DemurrageChargeConfigDto findDemurrageChargeConfig(Long companyPoid) {
		try {
			String sql = """
					SELECT PARAMETER_VALUE,
					       (SELECT TAX_POID FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN (
					            SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH
					            INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD
					                ON GTH.TRANSACTION_POID = GTD.TRANSACTION_POID
					            WHERE TO_DATE(SYSDATE) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO)
					              AND CHARGE_POID = PARAMETER_VALUE)) TAX_POID,
					       (SELECT PERCENTAGE FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN (
					            SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH
					            INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD
					                ON GTH.TRANSACTION_POID = GTD.TRANSACTION_POID
					            WHERE TO_DATE(SYSDATE) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO)
					              AND CHARGE_POID = PARAMETER_VALUE)) TAX_PERCENTAGE,
					       RTN_GLOBAL_PARAMETER('1','GLOBAL_TAX_APPLICABLE','TAX',:companyPoid,'N') TAX_APPLICABLE
					FROM GLOBAL_PARAMETERS
					WHERE PARAMETER_KEYID_TYPE IN ('SHDEMURRAGE')
					""";

			@SuppressWarnings("unchecked")
			List<Object[]> rows = entityManager.createNativeQuery(sql)
					.setParameter("companyPoid", companyPoid)
					.getResultList();

			if (rows.isEmpty()) {
				log.warn("SHDEMURRAGE global parameter is not configured for company {}", companyPoid);
				return null;
			}

			Object[] row = rows.get(0);
			Long chargePoid = toLong(row[0]);
			if (chargePoid == null) {
				log.error("SHDEMURRAGE parameter holds '{}', which is not a charge POID - the demurrage "
						+ "charge line cannot be built", row[0]);
			}
			return DemurrageChargeConfigDto.builder()
					.chargePoid(chargePoid)
					.taxPoid(toLong(row[1]))
					.taxPercentage(toBigDecimal(row[2]))
					.taxApplicable(toString(row[3]))
					.build();
		} catch (Exception e) {
			// Never swallow this: a failure here drops the demurrage charge row and the enquiry would
			// quietly report a smaller amount than the legacy screen.
			log.error("Failed to read the demurrage charge configuration for company: {}", companyPoid, e);
			throw new DataAccessResourceFailureException(
					"Failed to read the demurrage charge configuration for company " + companyPoid, e);
		}
	}

	@Override
	public List<PortChargeRowDto> findPortCharges(Long blPoid, Long companyPoid) {
		try {
			String sql = """
					SELECT CHARGE_TYPE_APPLICABLE, CHARGE_APPLICABLE, CHARGE_CODE_POID,
					       AMOUNT_20, AMOUNT_40, AMOUNT_OTHER,
					       (SELECT TAX_POID FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN (
					            SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH
					            INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD
					                ON GTH.TRANSACTION_POID = GTD.TRANSACTION_POID
					            WHERE TO_DATE(SYSDATE) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO)
					              AND CHARGE_POID = CHARGE_CODE_POID)) TAX_POID,
					       (SELECT PERCENTAGE FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN (
					            SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH
					            INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD
					                ON GTH.TRANSACTION_POID = GTD.TRANSACTION_POID
					            WHERE TO_DATE(SYSDATE) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO)
					              AND CHARGE_POID = CHARGE_CODE_POID)) TAX_PERCENTAGE,
					       RTN_GLOBAL_PARAMETER('1','GLOBAL_TAX_APPLICABLE','TAX',:companyPoid,'N') TAX_APPLICABLE,
					       (SELECT TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE))
					               + (SELECT MAX(TO_NUMBER(PARAMETER_VALUE)) FROM GLOBAL_PARAMETERS
					                  WHERE PARAMETER_NAME LIKE '%SHIPLATEDOCOLLECTION%') - 1
					        FROM SHIP_VOYAGE_HDR VHDR
					        INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR
					            ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID
					        WHERE BLHDR.TRANSACTION_POID = :blPoid) APPLICABLE_FROM
					FROM SHIP_PORT_CHARGES_HDR MCHDR
					INNER JOIN SHIP_PORT_CHARGES_DTL MCDTL
					    ON MCHDR.TRANSACTION_POID = MCDTL.TRANSACTION_POID
					WHERE (SELECT TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE))
					       FROM SHIP_VOYAGE_HDR VHDR
					       INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR
					           ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID
					       WHERE BLHDR.TRANSACTION_POID = :blPoid) BETWEEN PERIOD_FROM AND PERIOD_TO
					  AND NVL(CHARGE_LINE_POID,'0') = '0'
					  AND CHARGE_TYPE_APPLICABLE IN ('LATECOLLECTIONIMP','LATECOLLECTIONBOTH')
					  AND NVL(MCHDR.DELETED,'N') = 'N'
					  AND (SELECT (TO_DATE(SYSDATE) - TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE))) + 1 DAYS
					       FROM SHIP_VOYAGE_HDR VHDR
					       INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR
					           ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID
					       INNER JOIN SHIP_BL_MANIFEST_CHARGES_DTL CNTDTL
					           ON CNTDTL.TRANSACTION_POID = BLHDR.TRANSACTION_POID
					       WHERE NVL(BLHDR.TRANSACTION_POID,0) NOT IN (
					                 SELECT NVL(BL_POID,0) FROM AR_SH_RECEIPT_HDR
					                 UNION ALL
					                 SELECT NVL(BL_POID,0) FROM AR_SH_SALES_INVOICE_HDR
					                 WHERE NVL(INVOICE_TYPE,'xx') <> 'AUTOCAN')
					         AND BLHDR.TRANSACTION_POID = :blPoid
					       GROUP BY TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE)), BLHDR.TRANSACTION_POID)
					      >= (SELECT TO_NUMBER(PARAMETER_VALUE) FROM GLOBAL_PARAMETERS
					          WHERE PARAMETER_NAME LIKE '%SHIPLATEDOCOLLECTION%')

					UNION ALL

					SELECT CHARGE_TYPE_APPLICABLE, CHARGE_APPLICABLE, CHARGE_CODE_POID,
					       AMOUNT_20, AMOUNT_40, AMOUNT_OTHER,
					       (SELECT TAX_POID FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN (
					            SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH
					            INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD
					                ON GTH.TRANSACTION_POID = GTD.TRANSACTION_POID
					            WHERE TO_DATE(SYSDATE) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO)
					              AND CHARGE_POID = CHARGE_CODE_POID)) TAX_POID,
					       (SELECT PERCENTAGE FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN (
					            SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH
					            INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD
					                ON GTH.TRANSACTION_POID = GTD.TRANSACTION_POID
					            WHERE TO_DATE(SYSDATE) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO)
					              AND CHARGE_POID = CHARGE_CODE_POID)) TAX_PERCENTAGE,
					       RTN_GLOBAL_PARAMETER('1','GLOBAL_TAX_APPLICABLE','TAX',:companyPoid,'N') TAX_APPLICABLE,
					       CAST(NULL AS DATE) APPLICABLE_FROM
					FROM SHIP_PORT_CHARGES_HDR MCHDR
					INNER JOIN SHIP_PORT_CHARGES_DTL MCDTL
					    ON MCHDR.TRANSACTION_POID = MCDTL.TRANSACTION_POID
					WHERE (SELECT TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE))
					       FROM SHIP_VOYAGE_HDR VHDR
					       INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR
					           ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID
					       WHERE BLHDR.TRANSACTION_POID IN (
					                 SELECT BL_POID FROM AR_SH_RECEIPT_HDR
					                 UNION ALL
					                 SELECT BL_POID FROM AR_SH_SALES_INVOICE_HDR
					                 WHERE NVL(INVOICE_TYPE,'xx') <> 'AUTOCAN')
					         AND BLHDR.TRANSACTION_POID = :blPoid) BETWEEN PERIOD_FROM AND PERIOD_TO
					  AND NVL(CHARGE_LINE_POID,'0') = '0'
					  AND CHARGE_TYPE_APPLICABLE IN ('REVALIDATEIMP','REVALIDATEBOTH')
					  AND NVL(MCHDR.DELETED,'N') = 'N'
					ORDER BY 1, 2
					""";

			@SuppressWarnings("unchecked")
			List<Object[]> rows = entityManager.createNativeQuery(sql)
					.setParameter("blPoid", blPoid)
					.setParameter("companyPoid", companyPoid)
					.getResultList();

			return rows.stream()
					.map(row -> PortChargeRowDto.builder()
							.chargeTypeApplicable(toString(row[0]))
							.chargeApplicable(toString(row[1]))
							.chargeCodePoid(toLong(row[2]))
							.amount20(toBigDecimal(row[3]))
							.amount40(toBigDecimal(row[4]))
							.amountOther(toBigDecimal(row[5]))
							.taxPoid(toLong(row[6]))
							.taxPercentage(toBigDecimal(row[7]))
							.taxApplicable(toString(row[8]))
							.applicableFrom(toLocalDate(row[9]))
							.build())
					.toList();
		} catch (Exception e) {
			// Same reasoning as the demurrage configuration - a swallowed failure here silently hides
			// the late collection / revalidation charges.
			log.error("Failed to load port charges for BL POID: {}", blPoid, e);
			throw new DataAccessResourceFailureException(
					"Failed to load the late collection / revalidation charges for BL POID: " + blPoid, e);
		}
	}

	@Override
	public String getContainerSize(String equipmentIsoType) {
		if (equipmentIsoType == null) {
			return null;
		}
		try {
			return (String) entityManager.createNativeQuery(
					"SELECT GET_CONTAINER_TYPE(:equipmentIsoType,'SIZE') FROM DUAL")
					.setParameter("equipmentIsoType", equipmentIsoType)
					.getSingleResult();
		} catch (Exception e) {
			log.error("Failed to read the container size of ISO type: {}", equipmentIsoType, e);
			return null;
		}
	}

	private static String toString(Object value) {
		return value != null ? value.toString() : null;
	}

	/**
	 * Not every POID arrives as a number: {@code GLOBAL_PARAMETERS.PARAMETER_VALUE} is a VARCHAR2, so
	 * the demurrage charge POID comes back as the string "94" and dropping it would silently cost the
	 * enquiry its whole demurrage charge line.
	 */
	static Long toLong(Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}
		if (value instanceof String text && !text.isBlank()) {
			try {
				return new BigDecimal(text.trim()).longValueExact();
			} catch (NumberFormatException | ArithmeticException e) {
				log.warn("Value '{}' is not a POID", text);
				return null;
			}
		}
		return null;
	}

	private static BigDecimal toBigDecimal(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof BigDecimal bigDecimal) {
			return bigDecimal;
		}
		return value instanceof Number number ? BigDecimal.valueOf(number.doubleValue()) : null;
	}

	private static LocalDate toLocalDate(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof LocalDate localDate) {
			return localDate;
		}
		if (value instanceof LocalDateTime localDateTime) {
			return localDateTime.toLocalDate();
		}
		if (value instanceof java.sql.Date sqlDate) {
			return sqlDate.toLocalDate();
		}
		if (value instanceof java.util.Date date) {
			return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
		}
		return null;
	}
}
