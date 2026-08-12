-- Validates the header LOV projection in ImportManifestBlServiceImpl against the eleven
-- single-row queries it replaces.
--
-- Usage (SQL*Plus / SQLcl / any client that supports substitution variables):
--     DEFINE poid = <TRANSACTION_POID of the 2024 BL you want to check>
--     @verify_header_lov_projection.sql
--
-- Query 1 returns one row per LOV field, with the OLD value, the NEW (projected) value, and a
-- MATCH column. Every row must read 'OK'. Any 'MISMATCH' is a bug in the projection.
--
-- Note: the voyage LOV (VESSAL_VOYAGE) is NOT part of the projection — it still goes through
-- LovService because that applies group/company/user scoping. It is not checked here.

DEFINE poid = 0

-- ---------------------------------------------------------------------------
-- 1. Field-by-field comparison: old per-field query vs new projected value
-- ---------------------------------------------------------------------------
WITH projected AS (
    SELECT QTN.TRANSACTION_POID   AS QTN_POID,   QTN.DOC_REF        AS QTN_CODE,
           SLM.SALESMAN_POID      AS SLM_POID,   SLM.SALESMAN_NAME  AS SLM_NAME,
           CMD.COMODITY_POID      AS CMD_POID,   CMD.COMODITY_NAME  AS CMD_NAME,
           CNS.ADDRESS_MASTER_POID AS CNS_POID,  CNS.ADDRESS_NAME   AS CNS_NAME,
           NF1.ADDRESS_MASTER_POID AS NF1_POID,  NF1.ADDRESS_NAME   AS NF1_NAME,
           CUS.CUSTOMER_POID      AS CUS_POID,   CUS.CUSTOMER_NAME  AS CUS_NAME,
           RCP.PORT_POID          AS RCP_POID,   RCP.PORT_NAME      AS RCP_NAME,
           DLV.PORT_POID          AS DLV_POID,   DLV.PORT_NAME      AS DLV_NAME,
           LOD.PORT_POID          AS LOD_POID,   LOD.PORT_NAME      AS LOD_NAME,
           DIS.PORT_POID          AS DIS_POID,   DIS.PORT_NAME      AS DIS_NAME
    FROM SHIP_BL_MANIFEST_HDR HDR
    LEFT JOIN SALES_QUOTATION_SHIP_HDR QTN ON QTN.TRANSACTION_POID    = HDR.QUOTATION_TRANSACTION_POID
    LEFT JOIN SALES_SALESMAN_MASTER    SLM ON SLM.SALESMAN_POID       = HDR.SALESMAN_POID
    LEFT JOIN SHIP_COMODITY_MASTER     CMD ON CMD.COMODITY_POID       = HDR.COMODITY_POID
    LEFT JOIN GLOBAL_ADDRESS_MASTER    CNS ON CNS.ADDRESS_MASTER_POID = HDR.CONSIGNEE_POID
    LEFT JOIN GLOBAL_ADDRESS_MASTER    NF1 ON NF1.ADDRESS_MASTER_POID = HDR.NOTIFY_POID_1
    LEFT JOIN SALES_CUSTOMER_MASTER    CUS ON CUS.CUSTOMER_POID       = HDR.BOOKING_PARTY_POID
    LEFT JOIN SHIP_PORT_MASTER         RCP ON RCP.PORT_POID           = HDR.PLACE_OF_RECIEPT_POID
    LEFT JOIN SHIP_PORT_MASTER         DLV ON DLV.PORT_POID           = HDR.PLACE_OF_DELIEVERY_POID
    LEFT JOIN SHIP_PORT_MASTER         LOD ON LOD.PORT_POID           = HDR.PORT_OF_LOADING_POID
    LEFT JOIN SHIP_PORT_MASTER         DIS ON DIS.PORT_POID           = HDR.PORT_OF_DISCHARGE_POID
    WHERE HDR.TRANSACTION_POID = &poid
),
expected AS (
    SELECT (SELECT TRANSACTION_POID FROM SALES_QUOTATION_SHIP_HDR WHERE TRANSACTION_POID = H.QUOTATION_TRANSACTION_POID) AS QTN_POID,
           (SELECT DOC_REF          FROM SALES_QUOTATION_SHIP_HDR WHERE TRANSACTION_POID = H.QUOTATION_TRANSACTION_POID) AS QTN_CODE,
           (SELECT SALESMAN_POID    FROM SALES_SALESMAN_MASTER    WHERE SALESMAN_POID    = H.SALESMAN_POID)              AS SLM_POID,
           (SELECT SALESMAN_NAME    FROM SALES_SALESMAN_MASTER    WHERE SALESMAN_POID    = H.SALESMAN_POID)              AS SLM_NAME,
           (SELECT COMODITY_POID    FROM SHIP_COMODITY_MASTER     WHERE COMODITY_POID    = H.COMODITY_POID)              AS CMD_POID,
           (SELECT COMODITY_NAME    FROM SHIP_COMODITY_MASTER     WHERE COMODITY_POID    = H.COMODITY_POID)              AS CMD_NAME,
           (SELECT ADDRESS_MASTER_POID FROM GLOBAL_ADDRESS_MASTER WHERE ADDRESS_MASTER_POID = H.CONSIGNEE_POID)          AS CNS_POID,
           (SELECT ADDRESS_NAME     FROM GLOBAL_ADDRESS_MASTER    WHERE ADDRESS_MASTER_POID = H.CONSIGNEE_POID)          AS CNS_NAME,
           (SELECT ADDRESS_MASTER_POID FROM GLOBAL_ADDRESS_MASTER WHERE ADDRESS_MASTER_POID = H.NOTIFY_POID_1)           AS NF1_POID,
           (SELECT ADDRESS_NAME     FROM GLOBAL_ADDRESS_MASTER    WHERE ADDRESS_MASTER_POID = H.NOTIFY_POID_1)           AS NF1_NAME,
           (SELECT CUSTOMER_POID    FROM SALES_CUSTOMER_MASTER    WHERE CUSTOMER_POID    = H.BOOKING_PARTY_POID)         AS CUS_POID,
           (SELECT CUSTOMER_NAME    FROM SALES_CUSTOMER_MASTER    WHERE CUSTOMER_POID    = H.BOOKING_PARTY_POID)         AS CUS_NAME,
           (SELECT PORT_POID        FROM SHIP_PORT_MASTER         WHERE PORT_POID        = H.PLACE_OF_RECIEPT_POID)      AS RCP_POID,
           (SELECT PORT_NAME        FROM SHIP_PORT_MASTER         WHERE PORT_POID        = H.PLACE_OF_RECIEPT_POID)      AS RCP_NAME,
           (SELECT PORT_POID        FROM SHIP_PORT_MASTER         WHERE PORT_POID        = H.PLACE_OF_DELIEVERY_POID)    AS DLV_POID,
           (SELECT PORT_NAME        FROM SHIP_PORT_MASTER         WHERE PORT_POID        = H.PLACE_OF_DELIEVERY_POID)    AS DLV_NAME,
           (SELECT PORT_POID        FROM SHIP_PORT_MASTER         WHERE PORT_POID        = H.PORT_OF_LOADING_POID)       AS LOD_POID,
           (SELECT PORT_NAME        FROM SHIP_PORT_MASTER         WHERE PORT_POID        = H.PORT_OF_LOADING_POID)       AS LOD_NAME,
           (SELECT PORT_POID        FROM SHIP_PORT_MASTER         WHERE PORT_POID        = H.PORT_OF_DISCHARGE_POID)     AS DIS_POID,
           (SELECT PORT_NAME        FROM SHIP_PORT_MASTER         WHERE PORT_POID        = H.PORT_OF_DISCHARGE_POID)     AS DIS_NAME
    FROM SHIP_BL_MANIFEST_HDR H
    WHERE H.TRANSACTION_POID = &poid
),
compared AS (
    SELECT 'quotation.poid'   AS FIELD, TO_CHAR(e.QTN_POID) AS OLD_VALUE, TO_CHAR(p.QTN_POID) AS NEW_VALUE FROM expected e, projected p
    UNION ALL SELECT 'quotation.code',  e.QTN_CODE,           p.QTN_CODE           FROM expected e, projected p
    UNION ALL SELECT 'salesman.poid',   TO_CHAR(e.SLM_POID),  TO_CHAR(p.SLM_POID)  FROM expected e, projected p
    UNION ALL SELECT 'salesman.name',   e.SLM_NAME,           p.SLM_NAME           FROM expected e, projected p
    UNION ALL SELECT 'commodity.poid',  TO_CHAR(e.CMD_POID),  TO_CHAR(p.CMD_POID)  FROM expected e, projected p
    UNION ALL SELECT 'commodity.name',  e.CMD_NAME,           p.CMD_NAME           FROM expected e, projected p
    UNION ALL SELECT 'consignee.poid',  TO_CHAR(e.CNS_POID),  TO_CHAR(p.CNS_POID)  FROM expected e, projected p
    UNION ALL SELECT 'consignee.name',  e.CNS_NAME,           p.CNS_NAME           FROM expected e, projected p
    UNION ALL SELECT 'notify1.poid',    TO_CHAR(e.NF1_POID),  TO_CHAR(p.NF1_POID)  FROM expected e, projected p
    UNION ALL SELECT 'notify1.name',    e.NF1_NAME,           p.NF1_NAME           FROM expected e, projected p
    UNION ALL SELECT 'bookingParty.poid', TO_CHAR(e.CUS_POID), TO_CHAR(p.CUS_POID) FROM expected e, projected p
    UNION ALL SELECT 'bookingParty.name', e.CUS_NAME,         p.CUS_NAME           FROM expected e, projected p
    UNION ALL SELECT 'placeOfReceipt.poid', TO_CHAR(e.RCP_POID), TO_CHAR(p.RCP_POID) FROM expected e, projected p
    UNION ALL SELECT 'placeOfReceipt.name', e.RCP_NAME,       p.RCP_NAME           FROM expected e, projected p
    UNION ALL SELECT 'placeOfDelivery.poid', TO_CHAR(e.DLV_POID), TO_CHAR(p.DLV_POID) FROM expected e, projected p
    UNION ALL SELECT 'placeOfDelivery.name', e.DLV_NAME,      p.DLV_NAME           FROM expected e, projected p
    UNION ALL SELECT 'portOfLoading.poid', TO_CHAR(e.LOD_POID), TO_CHAR(p.LOD_POID) FROM expected e, projected p
    UNION ALL SELECT 'portOfLoading.name', e.LOD_NAME,        p.LOD_NAME           FROM expected e, projected p
    UNION ALL SELECT 'portOfDischarge.poid', TO_CHAR(e.DIS_POID), TO_CHAR(p.DIS_POID) FROM expected e, projected p
    UNION ALL SELECT 'portOfDischarge.name', e.DIS_NAME,      p.DIS_NAME           FROM expected e, projected p
)
SELECT FIELD, OLD_VALUE, NEW_VALUE,
       CASE WHEN OLD_VALUE = NEW_VALUE
              OR (OLD_VALUE IS NULL AND NEW_VALUE IS NULL) THEN 'OK' ELSE '*** MISMATCH ***' END AS MATCH
FROM compared
ORDER BY FIELD;


-- ---------------------------------------------------------------------------
-- 2. Row-count guard: the projection MUST return exactly one row.
--    More than one means a join fans out (a master table has duplicate keys),
--    which would silently pick an arbitrary row in Java.
-- ---------------------------------------------------------------------------
SELECT COUNT(*) AS PROJECTED_ROWS,
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE '*** JOIN FANS OUT ***' END AS VERDICT
FROM SHIP_BL_MANIFEST_HDR HDR
LEFT JOIN SALES_QUOTATION_SHIP_HDR QTN ON QTN.TRANSACTION_POID    = HDR.QUOTATION_TRANSACTION_POID
LEFT JOIN SALES_SALESMAN_MASTER    SLM ON SLM.SALESMAN_POID       = HDR.SALESMAN_POID
LEFT JOIN SHIP_COMODITY_MASTER     CMD ON CMD.COMODITY_POID       = HDR.COMODITY_POID
LEFT JOIN GLOBAL_ADDRESS_MASTER    CNS ON CNS.ADDRESS_MASTER_POID = HDR.CONSIGNEE_POID
LEFT JOIN GLOBAL_ADDRESS_MASTER    NF1 ON NF1.ADDRESS_MASTER_POID = HDR.NOTIFY_POID_1
LEFT JOIN SALES_CUSTOMER_MASTER    CUS ON CUS.CUSTOMER_POID       = HDR.BOOKING_PARTY_POID
LEFT JOIN SHIP_PORT_MASTER         RCP ON RCP.PORT_POID           = HDR.PLACE_OF_RECIEPT_POID
LEFT JOIN SHIP_PORT_MASTER         DLV ON DLV.PORT_POID           = HDR.PLACE_OF_DELIEVERY_POID
LEFT JOIN SHIP_PORT_MASTER         LOD ON LOD.PORT_POID           = HDR.PORT_OF_LOADING_POID
LEFT JOIN SHIP_PORT_MASTER         DIS ON DIS.PORT_POID           = HDR.PORT_OF_DISCHARGE_POID
WHERE HDR.TRANSACTION_POID = &poid;


-- ---------------------------------------------------------------------------
-- 3. Pick a 2024 BL to test with, if you need one.
-- ---------------------------------------------------------------------------
-- SELECT TRANSACTION_POID, BL_NUMBER, TRANSACTION_DATE
-- FROM SHIP_BL_MANIFEST_HDR
-- WHERE TRANSACTION_DATE BETWEEN DATE '2024-01-01' AND DATE '2024-12-31'
--   AND DELETED = 'N'
--   AND QUOTATION_TRANSACTION_POID IS NOT NULL
--   AND CONSIGNEE_POID IS NOT NULL
--   AND BOOKING_PARTY_POID IS NOT NULL
-- ORDER BY TRANSACTION_DATE DESC
-- FETCH FIRST 10 ROWS ONLY;
