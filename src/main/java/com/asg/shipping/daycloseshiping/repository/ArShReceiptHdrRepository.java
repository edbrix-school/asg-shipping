package com.asg.shipping.daycloseshiping.repository;

import com.asg.shipping.daycloseshiping.dto.DayCloseSummaryProjection;
import com.asg.shipping.daycloseshiping.dto.DayCloseSummaryProjectionImpl;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ArShReceiptHdrRepository {

    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public Optional<DayCloseSummaryProjection> fetchNewDayCloseSummary(Long groupPoid, Long companyPoid,
                                                                       String txnDate) {

        // Pending day-close date must be resolved independently of the cash/cheque join below —
        // if that day's receipts are all SPLIT or zero-amount, the join returns no rows and the
        // date must not be lost along with the (legitimately empty) amounts.
        String pendingDateSql = """
                select distinct min(to_date(transaction_date)) l_pending_dayclose
                from AR_SH_RECEIPT_HDR
                where to_date(transaction_date) not in ( select to_date(transaction_date) from AR_SH_DAY_END_CLOSE_HDR
                where nvl(total_amount,0)>=0 )
                """;

        String sql = """
                SELECT
                	SUM(DECODE(PYMT_TYPE,'CASH',AMOUNT,'IMCOCASH',AMOUNT,0)) CASH_AMOUNT,
                	SUM(DECODE(PYMT_TYPE,'CASH',0,'IMCOCASH',0,'ROUNDOFF',0,'TT',0,AMOUNT)) CHQ_AMOUNT,
                	SUM(DECODE(PYMT_TYPE,'CASH',0,'TT',0,1)) CHEQUE_COUNT
                	FROM AR_SH_RECEIPT_HDR ARSPHDR, AR_SH_RECEIPT_PYMT_DETAILS ARSPDTL
                	WHERE ARSPDTL.TRANSACTION_POID=ARSPHDR.TRANSACTION_POID
                	AND PYMT_TYPE NOT IN('SPLIT') AND AMOUNT<>0
                	AND TRUNC(TRANSACTION_DATE) = :txnDate
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
        List<Object> pendingDateResult = entityManager.createNativeQuery(pendingDateSql).getResultList();

        LocalDate transactionDate = null;
        if (!pendingDateResult.isEmpty() && pendingDateResult.get(0) != null) {
            Object dateVal = pendingDateResult.get(0);
            if (dateVal instanceof java.time.LocalDateTime ldt) {
                transactionDate = ldt.toLocalDate();
            } else if (dateVal instanceof java.sql.Date d) {
                transactionDate = d.toLocalDate();
            } else if (dateVal instanceof java.sql.Timestamp ts) {
                transactionDate = ts.toLocalDateTime().toLocalDate();
            }
        }

        if (transactionDate == null) {
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        List<Object[]> result = entityManager.createNativeQuery(sql).setParameter("txnDate", transactionDate)
                .getResultList();

        BigDecimal cashAmount = BigDecimal.ZERO;
        BigDecimal chequeAmount = BigDecimal.ZERO;
        Long chequeCount = 0L;
        if (!result.isEmpty()) {
            Object[] row = result.get(0);
            if (row[0] != null) cashAmount = (BigDecimal) row[0];
            if (row[1] != null) chequeAmount = (BigDecimal) row[1];
            if (row[2] != null) chequeCount = ((Number) row[2]).longValue();
        }

        @SuppressWarnings("unchecked")
        List<Object> totalAmountResult = entityManager.createNativeQuery(totalAmountSql)
                .setParameter("txnDate", transactionDate).getResultList();
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (!totalAmountResult.isEmpty() && totalAmountResult.get(0) != null) {
            totalAmount = (BigDecimal) totalAmountResult.get(0);
        }

        return Optional.of(DayCloseSummaryProjectionImpl.builder().transactionDate(transactionDate)
                .chequeAmount(chequeAmount).cashAmount(cashAmount).totalAmount(totalAmount)
                .noOfCheques(chequeCount).verifiedRcvd("N").mainOfcRemarks(".").build());
    }
}
