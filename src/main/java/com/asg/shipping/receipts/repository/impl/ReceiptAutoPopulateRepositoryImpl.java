package com.asg.shipping.receipts.repository.impl;

import com.asg.shipping.receipts.dto.ReceiptAutoPopulateChargeDto;
import com.asg.shipping.receipts.dto.ReceiptAutoPopulateContainerDto;
import com.asg.shipping.receipts.dto.TaxConfig;
import com.asg.shipping.receipts.repository.ReceiptAutoPopulateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Repository
public class ReceiptAutoPopulateRepositoryImpl implements ReceiptAutoPopulateRepository {

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public List<ReceiptAutoPopulateChargeDto> findAvailableChargesForBl(Long blPoid, Long currentReceiptId) {

        try {
            String sql = """
            SELECT
                CHARGEDTL.TRANSACTION_POID AS BL_POID,
                CHARGEDTL.CHARGE_POID,
                CHARGEDTL.DET_ROW_ID,
                ROUND(
                    NVL(CHARGEDTL.CURRENCY_EXCHANGE,1)
                  * NVL(CHARGEDTL.QUANTITY,1)
                  * NVL(CHARGEDTL.PER_QUANTITY_AMOUNT,0),
                3) AS AMOUNT,
                DECODE(
                    CHARGEDTL.EDI_CHARGE_CODE,
                    'ADDFROMRECEIPT','Y',
                    'ADDFROMINVOICE','Y',
                    'N'
                ) AS ADD_FLAG,
                CHARGEDTL.INVOICE_TYPE,
                CHARGEDTL.TAX_POID,
                CHARGEDTL.TAX_PERCENTAGE,
                CHARGEDTL.TAX_AMOUNT,
                (
                    ROUND(
                        NVL(CHARGEDTL.CURRENCY_EXCHANGE,1)
                      * NVL(CHARGEDTL.QUANTITY,1)
                      * NVL(CHARGEDTL.PER_QUANTITY_AMOUNT,0),
                    3)
                    + NVL(CHARGEDTL.TAX_AMOUNT,0)
                ) AS TOTAL_RCPAMOUNT
            FROM SHIP_VOYAGE_HDR VHDR
            INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR
                ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID
            INNER JOIN SHIP_BL_MANIFEST_CHARGES_DTL CHARGEDTL
                ON CHARGEDTL.TRANSACTION_POID = BLHDR.TRANSACTION_POID
            WHERE FREIGHT_TYPE =
                  DECODE(
                      BLHDR.BL_TYPE,
                      'IMPORT','C',
                      'SWITCH','E',
                      'CROSSTRADE','E',
                      'XX'
                  )
              AND CHARGEDTL.AR_SH_RECEIPT_TRANSACTION_POID IS NULL
              AND BLHDR.TRANSACTION_POID = :blPoid
              AND (
                    CHARGEDTL.TRANSACTION_POID,
                    CHARGEDTL.DET_ROW_ID,
                    CHARGEDTL.CHARGE_POID
                  ) NOT IN (
                    SELECT
                        NVL(ARSHRCPCHD.BL_POID,0),
                        NVL(ARSHRCPCHD.CHARGES_DET_ROW_ID,0),
                        NVL(ARSHRCPCHD.CHARGE_POID,0)
                    FROM AR_SH_RECEIPT_HDR ARSHRCP,
                         AR_SH_RECEIPT_CHARGES_DTL ARSHRCPCHD
                    WHERE ARSHRCP.TRANSACTION_POID = ARSHRCPCHD.TRANSACTION_POID
                      AND NVL(ARSHRCP.DELETED,'N') = 'N'
                      AND ARSHRCP.TRANSACTION_POID <> :currentDocKeyPoid

                    UNION ALL

                    SELECT
                        NVL(ARSHINVCHD.BL_POID,0),
                        NVL(ARSHINVCHD.CHARGES_DET_ROW_ID,0),
                        NVL(ARSHINVCHD.CHARGE_POID,0)
                    FROM AR_SH_SALES_INVOICE_HDR ARSHINV,
                         AR_SH_SALES_INVOICE_CHARG_DTL ARSHINVCHD
                    WHERE ARSHINV.TRANSACTION_POID = ARSHINVCHD.TRANSACTION_POID
                      AND NVL(ARSHINV.DELETED,'N') = 'N'
                      AND NVL(ARSHINV.INVOICE_TYPE,'xx') <> 'AUTOCAN'
              )
            """;

            @SuppressWarnings("unchecked")
            List<Object[]> rows = entityManager
                    .createNativeQuery(sql)
                    .setParameter("blPoid", blPoid)
                    .setParameter("currentDocKeyPoid",
                            currentReceiptId)
                    .getResultList();

            return rows.stream()
                    .map(r -> ReceiptAutoPopulateChargeDto.builder()
                            .blPoid(((Number) r[0]).longValue())
                            .chargePoid(((Number) r[1]).longValue())
                            .detRowId(((Number) r[2]).longValue())
                            .amount((BigDecimal) r[3])
                            .addFlag(r[4] != null ? r[4].toString() : null)
                            .invoiceType((String) r[5])
                            .taxPoid(r[6] != null ? ((Number) r[6]).longValue() : null)
                            .taxPercentage((BigDecimal) r[7])
                            .taxAmount((BigDecimal) r[8])
                            .totalAmount((BigDecimal) r[9])
                            .build())
                    .toList();

        } catch (Exception e) {
            log.error("Failed to auto populate charges for BL POID: {}",blPoid,e);
            throw new DataAccessResourceFailureException(
                    "Failed to auto populate charges  for BL POID: " + blPoid,
                    e
            );
        }
    }

    @Override
    public List<ReceiptAutoPopulateContainerDto> findAvailableContainersForBl(
            Long blPoid,
            Long currentReceiptId) {



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
                       :currentReceiptId,
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
                             AND TRANSACTION_POID <> :currentReceiptId
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
                   AND DECODE(
                           VHDR.LINE_POID,
                           '1123', TO_DATE(SYSDATE),
                           TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE))
                       ) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO)
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
                    .setParameter("currentReceiptId",
                            currentReceiptId != null ? currentReceiptId : 0)
                    .getResultList();

            return rows.stream()
                    .map(r -> ReceiptAutoPopulateContainerDto.builder()
                            .blPoid(((Number) r[0]).longValue())

                            .equipmentShipperOwn(r[1] != null ? r[1].toString() : null)

                            .containerNo((String) r[2])

                            .fromDate(convertToLocalDate(r[3]))
                            .toDate(convertToLocalDate(r[4]))

                            .days(((Number) r[5]).longValue())

                            .demAmount((BigDecimal) r[6])

                            .equipmentIsoType((String) r[7])

                            .freeDays(((Number) r[8]).longValue())

                            .emptyIn(convertToLocalDate(r[9]))

                            .build())
                    .toList();

        } catch (Exception e) {
            log.error("Failed to auto populate  containers for BL POID: {}",blPoid,e);
            throw new DataAccessResourceFailureException(
                    "Failed to auto populate  containers for BL POID: " + blPoid,
                    e
            );
        }
    }

    @Override
    public BigDecimal findLinePoidByBlPoid(Long blPoid) {
        try {
            String sql = "SELECT v.LINE_POID FROM SHIP_BL_MANIFEST_HDR b JOIN SHIP_VOYAGE_HDR v ON v.TRANSACTION_POID = b.VOYAGE_TRANSACTION_POID WHERE b.TRANSACTION_POID = :blPoid";
            return (BigDecimal) entityManager.createNativeQuery(sql)
                    .setParameter("blPoid", blPoid)
                    .getSingleResult();
        } catch (Exception e) {
            log.error("Failed to find LINE_POID for BL POID: {}", blPoid, e);
            throw new DataAccessResourceFailureException("Unable to fetch LINE_POID for BL POID: " + blPoid, e);
        }
    }

    private LocalDate convertToLocalDate(Object date) {
        if (date == null) return null;
        if (date instanceof LocalDate) return (LocalDate) date;
        if (date instanceof LocalDateTime) return ((LocalDateTime) date).toLocalDate();
        if (date instanceof java.util.Date) return ((java.util.Date) date).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        return null;
    }

}