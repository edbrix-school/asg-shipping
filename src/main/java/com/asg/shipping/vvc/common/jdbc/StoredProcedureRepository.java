package com.asg.shipping.vvc.common.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
@Slf4j
public class StoredProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public String procGlobUserLineListing(Long loginUserPoid) {
        return jdbcTemplate.execute((Connection con) -> {
            CallableStatement cs = con.prepareCall("{call PROC_GLOB_USER_LINE_LISTING(?, ?)}");
            cs.setObject(1, loginUserPoid, Types.NUMERIC);
            cs.registerOutParameter(2, Types.VARCHAR);
            cs.execute();
            return cs.getString(2);
        });
    }

    public String procAttachmentsEdiProcNew(Long groupPoid, Long companyPoid, String docId, Long docKeyPoid, Long jobPoid, String loginUser) {
        return jdbcTemplate.execute((Connection con) -> {
            CallableStatement cs = con.prepareCall("{call PROC_ATTACHMENTS_EDI_PROC_NEW(?, ?, ?, ?, ?, ?, ?)}");
            cs.setObject(1, groupPoid, Types.NUMERIC);
            cs.setObject(2, companyPoid, Types.NUMERIC);
            cs.setString(3, docId);
            cs.setObject(4, docKeyPoid, Types.NUMERIC);
            cs.setObject(5, jobPoid, Types.NUMERIC);
            cs.registerOutParameter(6, Types.VARCHAR); // P_STATUS
            cs.setString(7, loginUser);
            cs.execute();
            return cs.getString(6);
        });
    }

    public void prodResendCan(Long voyageTransactionPoid, Long blTransactionPoid) {
        jdbcTemplate.execute((Connection con) -> {
            CallableStatement cs = con.prepareCall("{call PROD_RESEND_CAN(?, ?)}");
            cs.setObject(1, voyageTransactionPoid, Types.NUMERIC);
            cs.setObject(2, blTransactionPoid, Types.NUMERIC);
            cs.execute();
            return null;
        });
    }

    public String procMateRcptEmptyManifest(Long voyagePoid, String userCode) {
        return jdbcTemplate.execute((Connection con) -> {
            CallableStatement cs = con.prepareCall("{call PROC_MATE_RCPT_EMPTY_MANIFEST(?, ?, ?)}");
            cs.setObject(1, voyagePoid, Types.NUMERIC);
            cs.setString(2, userCode);
            cs.registerOutParameter(3, Types.VARCHAR);
            cs.execute();
            return cs.getString(3);
        });
    }

    public String procLoadTdrOwnLine(Long voyagePoid, Long loginUserPoid) {
        return jdbcTemplate.execute((Connection con) -> {
            CallableStatement cs = con.prepareCall("{call PROC_LOAD_TDR_OWN_LINE(?, ?, ?)}");
            cs.setObject(1, voyagePoid, Types.NUMERIC);
            cs.setObject(2, loginUserPoid, Types.NUMERIC);
            cs.registerOutParameter(3, Types.VARCHAR);
            cs.execute();
            return cs.getString(3);
        });
    }

    public void procShipExportEdiCosco(Long voyagePoid, Long blPoid, Long loginUserPoid) {
        jdbcTemplate.execute((Connection con) -> {
            CallableStatement cs = con.prepareCall("{call PROC_SHIP_EXPORT_EDI_COSCO(?, ?, ?)}");
            cs.setObject(1, voyagePoid, Types.NUMERIC);
            cs.setObject(2, blPoid, Types.NUMERIC);
            cs.setObject(3, loginUserPoid, Types.NUMERIC);
            cs.execute();
            return null;
        });
    }

    public String procGeneralImportCargo(Long groupPoid, Long companyPoid, String loginUser, Long voyagePoid) {
        return jdbcTemplate.execute((Connection con) -> {
            CallableStatement cs = con.prepareCall("{call PROC_GENERAL_IMPORT_CARGO(?, ?, ?, ?, ?)}");
            cs.setObject(1, groupPoid, Types.NUMERIC);
            cs.setObject(2, companyPoid, Types.NUMERIC);
            cs.setString(3, loginUser);
            cs.setObject(4, voyagePoid, Types.NUMERIC);
            cs.registerOutParameter(5, Types.VARCHAR);
            cs.execute();
            return cs.getString(5);
        });
    }

    public void procShipVoyageCurrencyUpd(Long groupPoid, Long companyPoid, Long voyagePoid, String userCode, String recordsUpdate) {
        jdbcTemplate.execute((Connection con) -> {
            CallableStatement cs = con.prepareCall("{call proc_ship_voyage_currency_upd(?, ?, ?, ?, ?)}");
            cs.setObject(1, groupPoid, Types.NUMERIC);
            cs.setObject(2, companyPoid, Types.NUMERIC);
            cs.setObject(3, voyagePoid, Types.NUMERIC);
            cs.setString(4, userCode);
            cs.setString(5, recordsUpdate);
            cs.execute();
            return null;
        });
    }

    public String procShipVoyageHnjnTranImpV2(Long voyagePoid) {
        return jdbcTemplate.execute((Connection con) -> {
            CallableStatement cs = con.prepareCall("{call PROC_SHIP_VOYAGE_HNJN_TRAN_IMP_V2(?, ?)}");
            cs.setObject(1, voyagePoid, Types.NUMERIC);
            cs.registerOutParameter(2, Types.VARCHAR);
            cs.execute();
            return cs.getString(2);
        });
    }

    public String procLoadEdiXlTemplate(Long voyagePoid) {
        return jdbcTemplate.execute((Connection con) -> {
            CallableStatement cs = con.prepareCall("{call PROC_LOAD_EDI_XL_TEMPLATE(?, ?)}");
            cs.setObject(1, voyagePoid, Types.NUMERIC);
            cs.registerOutParameter(2, Types.VARCHAR);
            cs.execute();
            return cs.getString(2);
        });
    }

    public List<Map<String, Object>> queryForList(String sql, Object... args) {
        return jdbcTemplate.queryForList(sql, args);
    }

    public <T> T query(String sql, ResultSetExtractor<T> extractor, Object... args) {
        return jdbcTemplate.query(sql, extractor, args);
    }
}


