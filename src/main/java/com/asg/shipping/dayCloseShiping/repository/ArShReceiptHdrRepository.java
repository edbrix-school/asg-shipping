package com.asg.shipping.dayCloseShiping.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.asg.shipping.dayCloseShiping.dto.DayCloseSummaryProjection;
import com.asg.shipping.dayCloseShiping.dto.DayCloseSummaryProjectionImpl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ArShReceiptHdrRepository {

	private final EntityManager entityManager;

	@Transactional(readOnly = true)
	public Optional<DayCloseSummaryProjection> fetchNewDayCloseSummary(Long groupPoid, Long companyPoid,
			String txnDate) {

		String sql = """
				SELECT
					SUM(DECODE(PYMT_TYPE,'CASH',AMOUNT,'IMCOCASH',AMOUNT,0)) CASH_AMOUNT,
					SUM(DECODE(PYMT_TYPE,'CASH',0,'IMCOCASH',0,'ROUNDOFF',0,'TT',0,AMOUNT)) CHQ_AMOUNT,
					SUM(DECODE(PYMT_TYPE,'CASH',AMOUNT,'IMCOCASH',AMOUNT,0))
					+ SUM(DECODE(PYMT_TYPE,'CASH',0,'IMCOCASH',0,'ROUNDOFF',0,'TT',0,AMOUNT)) TOTAL_AMOUNT,
					SUM(DECODE(PYMT_TYPE,'CASH',0,'TT',0,1)) CHEQUE_COUNT,
					MAX(TRANSACTION_DATE) TRANSACTION_DATE
					FROM AR_SH_RECEIPT_HDR ARSPHDR, AR_SH_RECEIPT_PYMT_DETAILS ARSPDTL
					WHERE ARSPDTL.TRANSACTION_POID=ARSPHDR.TRANSACTION_POID
					AND PYMT_TYPE NOT IN('SPLIT') AND AMOUNT<>0 AND
					TO_DATE(TRANSACTION_DATE) IN (select distinct min(to_date(transaction_date)) l_pending_dayclose
					from AR_SH_RECEIPT_HDR
					where to_date(transaction_date) not in ( select to_date(transaction_date) from AR_SH_DAY_END_CLOSE_HDR
					where nvl(total_amount,0)>=0 ))
									""";

		String totalAmountSql = """
				SELECT
				    SUM(ARSPHDR.RCPT_AMOUNT)
				    - NVL(
				        (
				            SELECT NVL(SUM(DTL.AMOUNT), 0)
				            FROM AR_SH_RECEIPT_PYMT_DETAILS DTL
				            LEFT JOIN AR_SH_RECEIPT_HDR HDR
				                ON DTL.TRANSACTION_POID = HDR.TRANSACTION_POID
				            WHERE DTL.PYMT_TYPE = 'ROUNDOFF'
				              AND TRUNC(HDR.TRANSACTION_DATE) = :txnDate
				              AND HDR.TRANSACTION_POID NOT IN (
				                    SELECT TRANSACTION_POID
				                    FROM AR_SH_RECEIPT_PYMT_DETAILS
				                    WHERE PYMT_TYPE IN ('SPLIT', 'TT')
				                      AND NVL(AMOUNT, 0) <> 0
				              )
				        ),
				        0
				    ) AS TOTAL_AMOUNT
				FROM AR_SH_RECEIPT_HDR ARSPHDR
				WHERE ARSPHDR.TRANSACTION_POID IN (
				        SELECT TRANSACTION_POID
				        FROM AR_SH_RECEIPT_PYMT_DETAILS
				        WHERE PYMT_TYPE NOT IN ('SPLIT', 'TT')
				          AND AMOUNT <> 0
				)
				AND TRUNC(ARSPHDR.TRANSACTION_DATE) = :txnDate
				AND ARSPHDR.TRANSACTION_POID NOT IN (
				        SELECT TRANSACTION_POID
				        FROM AR_SH_RECEIPT_PYMT_DETAILS
				        WHERE PYMT_TYPE IN ('SPLIT', 'TT')
				          AND NVL(AMOUNT, 0) <> 0
				)
				GROUP BY TRUNC(ARSPHDR.TRANSACTION_DATE)
												""";

		@SuppressWarnings("unchecked")
		List<Object[]> result = entityManager.createNativeQuery(sql).getResultList();

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate castedTxndate = LocalDate.parse(txnDate, formatter);
		@SuppressWarnings("unchecked")
		List<Object> totalAmountResult = entityManager.createNativeQuery(totalAmountSql)
				.setParameter("txnDate", castedTxndate).getResultList();
		BigDecimal totalAmount = BigDecimal.ZERO;
		if (result.isEmpty()) {
			return Optional.empty();
		}
		if (!totalAmountResult.isEmpty()) {
			Object totalAmt = totalAmountResult.get(0);
			totalAmount = (BigDecimal) totalAmt;
		}

		Object[] row = result.get(0);
		LocalDate transactionDate = null;
		if (row[4] != null) {
			if (row[4] instanceof java.time.LocalDateTime ldt) {
				transactionDate = ldt.toLocalDate();
			} else if (row[4] instanceof java.sql.Date d) {
				transactionDate = d.toLocalDate();
			} else if (row[4] instanceof java.sql.Timestamp ts) {
				transactionDate = ts.toLocalDateTime().toLocalDate();
			}
		}

		return Optional.of(DayCloseSummaryProjectionImpl.builder().transactionDate(transactionDate)
				.chequeAmount((BigDecimal) row[1]).cashAmount((BigDecimal) row[0]).totalAmount(totalAmount)
				.chequeCount(row[3] != null ? ((Number) row[3]).longValue() : 0L).build());
	}
}
