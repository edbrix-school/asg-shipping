# Demurrage Enquiry - BL wise (100-144) — SQL reference

Reference copy of every query implemented for the Demurrage Enquiry - BL wise document, kept for
safe keeping alongside the legacy source they were ported from.

| Item | Value |
|---|---|
| Document | Shipping → Transactions → Demurrage Enquiry BL wise (**100-144**) |
| SRS | `Shipping -Demurrage Enquiry - BL wise V 1.0 .docx` |
| Legacy bean | `ASGShippingViewController/src/asg/shipping/view/DemEnquiryBlWiseBean.java` |
| Legacy page | `ASGShippingViewController/public_html/DemEnquiryBlWise.jsff` |
| Implementation | `com.asg.shipping.demurrageenquiryblwise.repository.impl.DemurrageEnquiryBlWiseRepositoryImpl` |
| Persistence | None — the document is display only, every figure is derived at read time |

All queries are Hibernate native queries with **named bind parameters**; the legacy built the same
statements by string concatenation.

## Query index

| # | Method | Legacy origin | Feeds |
|---|---|---|---|
| 1 | `blExists` | new (guard) | 404 when the BL does not exist |
| 2 | `loadContainers` | `loadContainerDataDemurrage()` / SRS query 2 | Containers tab |
| 3 | `findDoStatus` | `DO_STATUS` column of `loadContainerDataDemurrage()` | DO status field |
| 4 | `calculateContainerDemurrage` | `ArrayTableDemSubmit()` / SRS query 3 | Apply Date recalculation |
| 5 | `findManifestCharges` | `ChargeLoadManifestData()` / SRS query 1.1 | Charges tab |
| 6 | `findDemurrageChargeConfig` | `ChargeLoadManifestData()` / SRS query 1.2 | Demurrage charge row |
| 7 | `findPortCharges` | `ChargeLoadManifestData()` / SRS query 1.3 | Late collection / revalidation rows |
| 8 | `getContainerSize` | `createDemurrageDettention()` | PERQUENTITY charge split |

### Shared constant

`:currentTransactionPoid` is always bound to **0** (`NO_TRANSACTION`). The enquiry never creates a
transaction, so no receipt / invoice has to be excluded from the already billed demurrage periods.
The legacy passed the scratch receipt POID of the screen here.

---

## 1. `blExists(blPoid)`

Guard added on top of the legacy: the enquiry returns `404` instead of an empty screen for an
unknown or deleted BL.

```sql
SELECT COUNT(1)
FROM SHIP_BL_MANIFEST_HDR
WHERE TRANSACTION_POID = :blPoid
  AND NVL(DELETED,'N') = 'N'
```

Binds: `:blPoid`

---

## 2. `loadContainers(blPoid)` — Containers tab

Legacy: `loadContainerDataDemurrage()`. Loads the containers of the BL with the demurrage period
defaults, the already billed cut-off, the empty return move and the tariff validated period.

```sql
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
```

Binds: `:blPoid`, `:currentTransactionPoid`

| Col | Alias | Mapped to `DemurrageEnquiryContainerDto` |
|---|---|---|
| 0 | `BL_POID` | `blPoid` |
| 1 | `EQUIPMENT_SHIPPER_OWN` | `containerSocYn` (SOC) |
| 2 | `CONTAINER_NO` | `containerNo` |
| 3 | `FMDATE` | `dmFrmDate` |
| 4 | `TODATE` | `dmToDate` |
| 5 | `DAYS` | `dmDays` |
| 6 | `DM_AMT` | `dmChargeAmt` / `dmChargeAmtBeforeDiscount` |
| 7 | `EQUIPMENT_ISO_TYPE` | `equipmentIsoType` |
| 8 | `FREE_DAYS` | `freeDays` |
| 9 | `EMPTY_IN` | `emptyIn` |

Notes vs legacy:

- `DO_STATUS` was split out into query 3 (it is a BL level value, not a container column).
- `ORDER BY CONTAINER_NO` added so the SN column is stable between calls.
- The `VW_AR_SH_CONTAINER_DEMG_DTTN` sub-select is qualified with `ARCONTAINERDTL.TRANSACTION_POID`
  (the legacy left `TRANSACTION_POID` unqualified).
- On `GET /bl-details` the service blanks `dmToDate`, `dmDays` and `dmChargeAmt` afterwards, matching
  the legacy which commented out those `setAttribute` calls on BL selection.

---

## 3. `findDoStatus(blPoid)`

Legacy: the `DO_STATUS` column of `loadContainerDataDemurrage()`, bound to the read-only text field
next to the BL number.

```sql
SELECT NVL((SELECT MAX('DOISSUED')
            FROM DO_SH_PRINTING_DTL
            WHERE NVL(DO_PRINTED,'N') = 'Y'
              AND TRANSACTION_POID = :blPoid), 'DONOTISSUED')
FROM DUAL
```

Binds: `:blPoid`

Note vs legacy: wrapped in `MAX(...)`. The legacy scalar sub-select raises ORA-01427 when a BL has
more than one printed DO row.

---

## 4. `calculateContainerDemurrage(blPoid, containerNo, toDate)` — Apply Date

Legacy: `ArrayTableDemSubmit(current_row, discPer)`. One call per container row.

```sql
SELECT NVL(DM_TILL_DATE,FROMDATE) FMDATE,
       TODATE,
       (TO_DATE(TODATE) - NVL(DM_TILL_DATE,FROMDATE)) + 1 DAYS,
       FUNC_RTN_DEM_DETTN_FULL(
           GROUP_POID,
           COMPANY_POID,
           :currentTransactionPoid,
           BL_POID,
           CONTAINER_NO,
           GET_CONTAINER_CODE_POID(EQUIPMENT_ISO_TYPE),
           LINE_POID,
           TO_DATE(ARRIVAL_DATE),
           TO_DATE(TODATE),
           'DEMM',
           NVL(EXTRA_FREE_DAYS,0)
       ) DM_AMT
FROM (
    SELECT TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE)) ARRIVAL_DATE,
           BLHDR.GROUP_POID,
           BLHDR.COMPANY_POID,
           EQUIPMENT_ISO_TYPE,
           VHDR.LINE_POID,
           EXTRA_FREE_DAYS,
           CONTAINERDTL.TRANSACTION_POID BL_POID,
           CONTAINER_NO,
           TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE))
             + DECODE(NVL(EXTRA_FREE_DAYS,0),0,FREE_DAYS,NVL(EXTRA_FREE_DAYS,0)) FROMDATE,
           :toDate TODATE,
           (
               SELECT MAX(DM_TO_DATE) + 1
               FROM VW_AR_SH_CONTAINER_DEMG_DTTN ARCONTAINERDTL
               WHERE ARCONTAINERDTL.BL_POID = CONTAINERDTL.TRANSACTION_POID
                 AND ARCONTAINERDTL.CONTAINER_NO = CONTAINERDTL.CONTAINER_NO
                 AND ARCONTAINERDTL.TRANSACTION_POID <> :currentTransactionPoid
           ) DM_TILL_DATE
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
      AND CONTAINERDTL.CONTAINER_NO = :containerNo
)
```

Binds: `:blPoid`, `:containerNo`, `:toDate` (`java.sql.Date`), `:currentTransactionPoid`

Post processing in `DemurrageEnquiryBlWiseServiceImpl.calculateContainers`, ported from the legacy:

- `:toDate` is the **Empty In** date when the container was already returned, otherwise the To Date
  entered on the screen (legacy `ApplyDateAction`).
- `DAYS <= 0` (or no row) → `dmDays = 0`, `dmChargeAmt = 0`. The legacy tested
  `days = 0 || days contains '-'`.
- Discount: `dmChargeAmt = DM_AMT - (DM_AMT * discount / 100)`, `HALF_UP` at 3 decimals.
- `:toDate` is bound as a real `DATE`; the legacy inlined the row value through
  `to_date(SUBSTR('<value>',1,10),'RRRR-MM-DD')`.

---

## 5. `findManifestCharges(blPoid)` — Charges tab, BL manifest charges

Legacy: first query of `ChargeLoadManifestData()` / SRS query 1.1. Charges of the BL that are neither
received nor invoiced yet.

```sql
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
```

Binds: `:blPoid`

Notes vs legacy:

- The legacy also carried `AND ARSHRCP.transaction_poid != nvl(<current doc>,0)`. With no document of
  its own that predicate is always true, so it was dropped.
- `DELETED` and `INVOICE_TYPE` are qualified with their owning alias (the legacy left them bare).
- Charge rows are rendered with `chargeType = BLCHARGE`; the `CHARGE_NEW_RECORD` /
  `INVOICE_TYPE` columns are carried on the DTO for parity but drive no logic in an enquiry.

---

## 6. `findDemurrageChargeConfig(companyPoid)` — demurrage charge row

Legacy: second query of `ChargeLoadManifestData()` / SRS query 1.2. Resolves the charge POID mapped
on the `SHDEMURRAGE` global parameter plus the tax applicable on it.

```sql
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
```

Binds: `:companyPoid` (from `UserContext.getCompanyPoid()`, i.e. the `X-Company-Poid` header)

> **`PARAMETER_VALUE` is a VARCHAR2**, so the charge POID arrives as the string `"94"`. Mapping it as
> a number only yields a config with no charge, and the enquiry then reports the demurrage amount but
> drops the demurrage charge line - the Charges tab shows one row where the legacy screen shows two.
> The row mapper parses both numbers and strings, and logs when the parameter is not a POID.

> **`RTN_GLOBAL_PARAMETER` raises ORA-01400 when the company is null** - the function writes into
> `GLOBAL_PARAMETERS` and the key column is mandatory. Both this query and the port charges query call
> it, so a request without a company context used to lose *both* derived charge rows. The service now
> rejects such a request instead, and neither query swallows a failure any more.

Post processing (legacy identical): the row is only added when the total demurrage of the containers
is non zero; `taxAmount = totalDemurrage * TAX_PERCENTAGE / 100` and only when `TAX_POID` is present
and `TAX_APPLICABLE = 'Y'`.

> The SRS calls this parameter `SHIPPERDEMURRAGE` in the "Global Parameters" section, but both the
> legacy bean and the SRS query itself read `PARAMETER_KEYID_TYPE = 'SHDEMURRAGE'` — the query wins.

---

## 7. `findPortCharges(blPoid, companyPoid)` — late collection / revalidation

Legacy: third query of `ChargeLoadManifestData()` / SRS query 1.3. First branch: late DO collection
charges for a BL that was never receipted or invoiced. Second branch: revalidation charges for a BL
that already was.

```sql
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
       RTN_GLOBAL_PARAMETER('1','GLOBAL_TAX_APPLICABLE','TAX',:companyPoid,'N') TAX_APPLICABLE
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
       RTN_GLOBAL_PARAMETER('1','GLOBAL_TAX_APPLICABLE','TAX',:companyPoid,'N') TAX_APPLICABLE
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
```

Binds: `:blPoid`, `:companyPoid`

Notes vs legacy:

- `DELETED` is qualified as `MCHDR.DELETED` — `SHIP_PORT_CHARGES_DTL` has no `DELETED` column, so the
  bare legacy reference resolved to the header anyway.
- `ORDER BY CHARGE_TYPE_APPLICABLE, CHARGE_APPLICABLE` is expressed positionally because it applies
  to the `UNION ALL`.
- `REVALIDATEBOTH` is the value used by the legacy and the SRS (the receipts module spells it
  `REVALIDATIONBOTH`).

Post processing in `buildPortCharge`:

| `CHARGE_APPLICABLE` | Amount |
|---|---|
| `PERBL` | `AMOUNT_OTHER` |
| `PERQUENTITY` | `AMOUNT_20 × count(20' containers)` + `AMOUNT_40 × count(other containers)` |

Only containers that actually carry demurrage are counted - the legacy fills `FtotalQtyValidate20/40`
inside `createDemurrageDettention()`, which increments them per container **whose `DmChargeAmt` is
non zero**, and the port charge loop then multiplies those counts by the slab rates. A container
still inside its free days is therefore not billed for the per quantity charge.

The container size comes from `GET_CONTAINER_TYPE(iso,'SIZE')`; anything that is not `20` counts
against the 40' rate, as in the legacy `else` branch.

Tax: `amount * TAX_PERCENTAGE / 100`, only when `TAX_POID` is present and `TAX_APPLICABLE = 'Y'`.

---

## 8. `getContainerSize(equipmentIsoType)`

Legacy: `createDemurrageDettention()`. Used to split the containers into 20' and other for the
`PERQUENTITY` port charges.

```sql
SELECT GET_CONTAINER_TYPE(:equipmentIsoType,'SIZE') FROM DUAL
```

Binds: `:equipmentIsoType`

---

## Report — View Demurrage Calculation

Legacy: `PrintDemurrageCalc()`.

| Parameter | Value |
|---|---|
| Jasper | `Shipping/SH/LINE_DEMURRAGE_CALC.jrxml` |
| `DOC_ID` | `100-144` |
| `DOC_KEY_POID` | BL POID as a String (**not** a document POID — the enquiry stores nothing) |
| `P_TILL_DATE` | To Date as **`yyyy-MM-dd`** (declared `java.lang.String`) |
| `P_DISCOUNT` | Discount %, plain number as a string, `0` when not entered |
| `SUBREPORT_DEMURRAGE_MASTER` | compiled `Shipping/SH/LINE_DEMURRAGE_MASTER.jrxml` |
| `SUBREPORT_DEMURRAGE_DTL` | compiled `Shipping/SH/LINE_DEMURRAGE_DTL.jrxml` |
| `SUB_HEADER`, `LOGIN_COMP_POID`, `LOGIN_USER_POID`, `LOGIN_GROUP_POID` | supplied by `PrintService.buildBaseParams` |

The three templates were copied from `Alsharif_Code/Reports/SH/` into
`src/main/resources/jasper/Shipping/SH/`. The main report's three `subreportExpression`s originally
resolved `.jasper` files off the `SUBREPORT_DIR` network share
(`\\10.100.100.159\ERP_REPORTS\Reports\`); they now read `$P{SUB_HEADER}`,
`$P{SUBREPORT_DEMURRAGE_MASTER}` and `$P{SUBREPORT_DEMURRAGE_DTL}`, matching how
`PORT_STORAGE_CALC.jrxml` was migrated. `MASTER` and `LINE_DEMURRAGE_DTL` have no nested subreports
and were copied unchanged. `DemurrageCalcReportCompilationTest` compiles all three and asserts the
parameter contract.

### Bold styling

The templates mark their headings, column headers, labels and totals with `isBold="true"`, but that
alone is dropped by the PDF exporter: with no font extension providing the family it falls back to
plain `Helvetica` and everything prints in normal weight - which is why the first migrated output
looked flat next to the legacy print. Every bold `<font>` in the three templates therefore also
carries `pdfFontName="Helvetica-Bold"` (35 elements). `DemurrageCalcReportCompilationTest` fails the
build if a bold font is added without it, and asserts `Helvetica-Bold` really lands in the exported
PDF.

Worth knowing when migrating further reports from `Alsharif_Code/Reports/` - the same silent loss
applies to any of them.

### Headings clipped away

Jasper empties a text element whose box is shorter than one line of its own font instead of
overflowing it - nothing is drawn and nothing is logged. The **Demurrage Calculation** heading (14pt
in a 19px box, in the column header band of `LINE_DEMURRAGE_DTL`) vanished exactly that way: the
element was laid out, its text came out empty. The legacy font metrics fitted; this JVM's do not.

Heights raised for the two headings that sat on the limit:

| Template | Element | Was | Now |
|---|---|---|---|
| `LINE_DEMURRAGE_DTL` | `Demurrage Calculation` | 19 | 24 (column headers start at y=38) |
| `LINE_DEMURRAGE_CALC` | `Demurrage/Detention Tariff` | 20 | 23 (fills its 23px band) |

Rule of thumb when porting: a text box needs roughly `fontSize * 1.45` of height, or
`isStretchWithOverflow="true"`. `DemurrageCalcReportCompilationTest.headingsAreTallEnoughToRender`
fills the report and asserts the heading text really is printed.

### Parameter formats the subreports impose

`LINE_DEMURRAGE_DTL` consumes both screen inputs inside its own query, so the formats are not free:

```sql
LEFT JOIN (SELECT TO_CHAR(TO_DATE(SUBSTR($P{P_TILL_DATE},1,10),'RRRR-MM-DD')) TODATE FROM DUAL) TODT ON 1=1
...
DM_AMT1 - ((TO_NUMBER($P{P_DISCOUNT})/100) * DM_AMT1) DM_AMT
```

- `P_TILL_DATE` must be `yyyy-MM-dd`. Any other pattern (`dd-MMM-yyyy` for instance) fails the fill
  with **ORA-01858 - a non-numeric character was found where a numeric was expected**. The legacy fed
  it `attrCreatedDate.getInputValue().toString()`, which is `yyyy-MM-dd`.
- `P_DISCOUNT` must be a plain number as a string - it goes through `TO_NUMBER`.
- The discount is applied a second time *inside* the report; the service does not pre-discount the
  amounts it sends, it only forwards the percentage.

---

## Totals shown on the Charges tab

```text
receiptAmount      = Σ charge.amount                 (tax excluded)
totalTaxAmount     = Σ charge.taxAmount
totalAmountWithVat = receiptAmount + totalTaxAmount
```

Every monetary figure - the container amounts, the charge amounts and taxes, and the four totals -
is written with **3 decimals**, matching the `maxFractionDigits=3 minFractionDigits=3` of the legacy
grid, so `56` serialises as `56.000` instead of on whatever scale Oracle returned. This is enforced
twice: the service scales the values it computes, and `AmountSerializer` scales again on the way to
JSON, so a value that reaches a DTO by any other route is still written correctly.

| DTO | Amount fields |
|---|---|
| `DemurrageEnquiryChargeDto` | `amount`, `taxAmount`, `totalAmount` |
| `DemurrageEnquiryContainerDto` | `dmChargeAmt`, `dmChargeAmtBeforeDiscount` |
| `DemurrageEnquiryResponseDto` | `totalDemurrageAmount`, `receiptAmount`, `totalTaxAmount`, `totalAmountWithVat` |

Percentages (`taxPercentage`, `discountPercentage`) keep their own scale - the legacy `AmtColumns`
listed only the amount columns (`Amount,TaxAmount` on the charges grid, `DmChargeAmt` on the
containers grid) and left the percentages out. Amounts stay JSON numbers; thousands separators are
left to the screen.

A charge row also carries a `remarks` field. The legacy Charges grid binds that column to
`AR_SH_RECEIPT_CHARGES_DTL.REMARKS` - scratch storage this enquiry never writes to, and
`SHIP_BL_MANIFEST_CHARGES_DTL` has no remarks column at all - so it is always null and exists only so
the screen can render the column.

> The legacy added *amount + tax* of the BL manifest charges into `FTotalRcpAmount` and then added
> the VAT again for "Total (Rcpt+Vat)", double counting the tax of those rows. The definition above
> follows the SRS field descriptions instead.

## Tables and functions used

`SHIP_VOYAGE_HDR`, `SHIP_BL_MANIFEST_HDR`, `SHIP_BL_MANIFEST_CONTAINER_DTL`,
`SHIP_BL_MANIFEST_CHARGES_DTL`, `SHIP_LINE_TARIFF_HDR`, `SHIP_LINE_TARIFF_IMP_DTL`,
`SHIP_CONTAINER_INVENTORY`, `SHIP_PORT_CHARGES_HDR`, `SHIP_PORT_CHARGES_DTL`,
`VW_AR_SH_CONTAINER_DEMG_DTTN`, `DO_SH_PRINTING_DTL`, `AR_SH_RECEIPT_HDR`,
`AR_SH_RECEIPT_CHARGES_DTL`, `AR_SH_SALES_INVOICE_HDR`, `AR_SH_SALES_INVOICE_CHARG_DTL`,
`GLOBAL_PARAMETERS`, `GLOBAL_TAX_MASTER`, `GLOBAL_TAX_PERIOD_HDR`, `GLOBAL_TAX_PERIOD_CHARGE_DTL`.

Functions: `FUNC_RTN_DEM_DETTN_FULL`, `GET_CONTAINER_CODE_POID`, `GET_CONTAINER_TYPE`,
`RTN_GLOBAL_PARAMETER`.
