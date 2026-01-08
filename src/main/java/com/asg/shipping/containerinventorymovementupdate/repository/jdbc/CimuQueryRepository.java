
package com.asg.shipping.containerinventorymovementupdate.repository.jdbc;


import com.asg.shipping.containerinventorymovementupdate.dto.ContainerHistoryRowDto;
import com.asg.shipping.containerinventorymovementupdate.dto.ContainerInfoDto;
import com.asg.shipping.importManifestUpdate.respository.ShipBlManifestContainerDtlRepository;
import com.asg.shipping.importManifestUpdate.respository.ShipBlManifestHdrRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CimuQueryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ShipBlManifestContainerDtlRepository containerDtlRepository;
    private final ShipBlManifestHdrRepository hdrRepository;

    /**
     * Container History tab is backed by VW_CONTAINER_MOVES_INVENTORY; legacy filtered by CONTAINER_NO.
     */
    public List<ContainerHistoryRowDto> fetchHistoryByContainerNo(String containerNo) {
        String sql = """
                SELECT CONTAINER_NO,
                       EXTRA_FREE_DAYS,
                       DISCHARGE_FULL,
                       WITH_CONSIGNEE_FULL,
                       EMPTY_IN,
                       EMPTY_OUT,
                       EXPORT_PORT_FULL,
                       LOAD_FULL,
                       LOAD_EMPTY,
                       SELECTED
                  FROM VW_CONTAINER_MOVES_INVENTORY
                 WHERE CONTAINER_NO = ?
                 ORDER BY DISCHARGE_FULL DESC NULLS LAST
                """;
        log.info("Query history from VW_CONTAINER_MOVES_INVENTORY | containerNo={}", containerNo);
        return jdbcTemplate.query(sql, historyRowMapper(), containerNo);
    }

    /**
     * Container Info is backed by VW_CONTAINER_DETAILS_UPDATE in legacy ADF.
     * Filters:
     * - by CONTAINER_NO (optional)
     * - by BL_NUMBER (optional)
     */
    public List<ContainerInfoDto> fetchContainerInfo(String containerNo, String blNumber) {
        StringBuilder sb = new StringBuilder("""
                SELECT TRANSACTION_POID,
                       COMPANY_POID,
                       VOYAGE_NO,
                       JOB_NO,
                       LINE,
                       VESSEL,
                       ARRIVAL_DATE,
                       BL_NUMBER,
                       BL_ISSUE_TYPE,
                       SHIPPER_EDI_NAME,
                       CONSIGNEE_EDI_NAME,
                       CONSIGNEE,
                       NOTIFY1_EDI_NAME,
                       NOTIFY,
                       LOAD_PORT,
                       FREIGHT_STATUS,
                       CONTAINER_NO,
                       EQUIPMENT_ISO_TYPE,
                       EXTRA_FREE_DAYS,
                       EXTRA_FREE_DAYS_PRNPLS,
                       DEM_AMOUNT,
                       DISCHARGE_FULL,
                       COARRI_DISCHARGE,
                       WITH_CONSIGNEE_FULL,
                       EMPTY_IN,
                       EMPTY_OUT,
                       EXPORT_PORT_FULL,
                       COARRI_LOAD,
                       LOAD_FULL,
                       LOAD_EMPTY,
                       MATE_BOOKING_NO,
                       EXPORT_BL_NUMBER,
                       DO_STATUS,
                       AMOUNT_PER_DAY_AFTER_FREE,
                       ACTUAL_DISCHARGE_DATE
                  FROM VW_CONTAINER_DETAILS_UPDATE
                 WHERE 1=1
                """);

        new Object() {};
        // dynamic params
        List<Object> params = new java.util.ArrayList<>();

        if (containerNo != null && !containerNo.isBlank()) {
            sb.append(" AND CONTAINER_NO = ? ");
            params.add(containerNo.trim());
        }
        if (blNumber != null && !blNumber.isBlank()) {
            sb.append(" AND BL_NUMBER = ? ");
            params.add(blNumber.trim());
        }
        sb.append(" ORDER BY CONTAINER_NO ");

        log.info("Query container info from VW_CONTAINER_DETAILS_UPDATE | containerNo={} blNumber={}", containerNo, blNumber);
        return jdbcTemplate.query(sb.toString(), containerInfoRowMapper(), params.toArray());
    }

    public boolean containerExistsInBl(Long transactionPoid, String containerNo) {
        return containerDtlRepository.existsByIdTransactionPoidAndContainerNo(transactionPoid, containerNo);
    }

    public boolean blNumberMatchesTransaction(Long transactionPoid, String blNumber) {
        return hdrRepository.findByTransactionPoidAndBlNumber(transactionPoid, blNumber).isPresent();
    }

    public BigDecimal fetchTotalCollectedAmount(Long transactionPoid, String containerNo) {
        return containerDtlRepository.findTotalAmountCollected(transactionPoid, containerNo).orElse(null);
    }

    private RowMapper<ContainerHistoryRowDto> historyRowMapper() {
        return (ResultSet rs, int rowNum) -> ContainerHistoryRowDto.builder()
                .containerNo(rs.getString("CONTAINER_NO"))
                .extraFreeDays(toLong(rs.getBigDecimal("EXTRA_FREE_DAYS")))
                .dischargeFull(toIso(rs.getTimestamp("DISCHARGE_FULL")))
                .withConsigneeFull(toIso(rs.getTimestamp("WITH_CONSIGNEE_FULL")))
                .emptyIn(toIso(rs.getTimestamp("EMPTY_IN")))
                .emptyOut(toIso(rs.getTimestamp("EMPTY_OUT")))
                .exportPortFull(toIso(rs.getTimestamp("EXPORT_PORT_FULL")))
                .loadFull(toIso(rs.getTimestamp("LOAD_FULL")))
                .loadEmpty(toIso(rs.getTimestamp("LOAD_EMPTY")))
                .selected(rs.getString("SELECTED"))
                .build();
    }

    private RowMapper<ContainerInfoDto> containerInfoRowMapper() {
        return (ResultSet rs, int rowNum) -> ContainerInfoDto.builder()
                .transactionPoid(toLong(rs.getBigDecimal("TRANSACTION_POID")))
                .companyPoid(toLong(rs.getBigDecimal("COMPANY_POID")))
                .voyageNo(rs.getString("VOYAGE_NO"))
                .jobNo(rs.getString("JOB_NO"))
                .line(rs.getString("LINE"))
                .vessel(rs.getString("VESSEL"))
                .arrivalDate(toIso(rs.getTimestamp("ARRIVAL_DATE")))
                .blNumber(rs.getString("BL_NUMBER"))
                .blIssueType(rs.getString("BL_ISSUE_TYPE"))
                .shipperEdiName(rs.getString("SHIPPER_EDI_NAME"))
                .consigneeEdiName(rs.getString("CONSIGNEE_EDI_NAME"))
                .consignee(rs.getString("CONSIGNEE"))
                .notify1EdiName(rs.getString("NOTIFY1_EDI_NAME"))
                .notify1(rs.getString("NOTIFY"))
                .loadPort(rs.getString("LOAD_PORT"))
                .freightStatus(rs.getString("FREIGHT_STATUS"))
                .containerNo(rs.getString("CONTAINER_NO"))
                .equipmentIsoType(rs.getString("EQUIPMENT_ISO_TYPE"))
                .extraFreeDays(toLong(rs.getBigDecimal("EXTRA_FREE_DAYS")))
                .extraFreeDaysPrnpls(toLong(rs.getBigDecimal("EXTRA_FREE_DAYS_PRNPLS")))
                .demAmount(rs.getBigDecimal("DEM_AMOUNT"))
                .dischargeFull(toIso(rs.getTimestamp("DISCHARGE_FULL")))
                .coarriDischarge(toIso(rs.getTimestamp("COARRI_DISCHARGE")))
                .withConsigneeFull(toIso(rs.getTimestamp("WITH_CONSIGNEE_FULL")))
                .emptyIn(toIso(rs.getTimestamp("EMPTY_IN")))
                .emptyOut(toIso(rs.getTimestamp("EMPTY_OUT")))
                .exportPortFull(toIso(rs.getTimestamp("EXPORT_PORT_FULL")))
                .coarriLoad(toIso(rs.getTimestamp("COARRI_LOAD")))
                .loadFull(toIso(rs.getTimestamp("LOAD_FULL")))
                .loadEmpty(toIso(rs.getTimestamp("LOAD_EMPTY")))
                .mateBookingNo(rs.getString("MATE_BOOKING_NO"))
                .exportBlNumber(rs.getString("EXPORT_BL_NUMBER"))
                .doStatus(rs.getString("DO_STATUS"))
                .amountPerDayAfterFree(rs.getBigDecimal("AMOUNT_PER_DAY_AFTER_FREE"))
                .actualDischargeDate(toIso(rs.getTimestamp("ACTUAL_DISCHARGE_DATE")))
                .build();
    }

    private static Long toLong(BigDecimal bd) {
        return bd == null ? null : bd.longValue();
    }

    private static String toIso(java.sql.Timestamp ts) {
        if (ts == null) return null;
        return ts.toLocalDateTime().toString();
    }

    public void clearInspectionTempTable() {
        String sql = "DELETE FROM SH_CONTAINER_INSPECT_IMP_TBL";
        log.info("Clearing temp table SH_CONTAINER_INSPECT_IMP_TBL");
        jdbcTemplate.update(sql);
    }

    public void insertInspectionTempTable(String slNo, String containerNo, String movesDate,
                                          String lineName, String sizeType, String locationStatus,
                                          String containerStatus, String remarks) {
        String sql = """
                INSERT INTO SH_CONTAINER_INSPECT_IMP_TBL 
                (SL_NO, CONTAINER_NO, MOVES_DATE, LINE_NAME, SIZE_TYPE, LOCATION_STATUS, CONTAINER_STATUS, REMARKS)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql,
                slNo != null ? slNo.trim() : null,  // SL_NO is VARCHAR2 in temp table (DDL line 2548)
                containerNo != null ? containerNo.trim() : null,
                movesDate != null ? movesDate.trim() : null,
                lineName != null ? lineName.trim() : null,
                sizeType != null ? sizeType.trim() : null,
                locationStatus != null ? locationStatus.trim() : null,
                containerStatus != null ? containerStatus.trim() : null,
                remarks != null ? remarks.trim() : null);
    }


}


