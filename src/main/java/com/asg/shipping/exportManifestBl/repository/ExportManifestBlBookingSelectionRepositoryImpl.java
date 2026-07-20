package com.asg.shipping.exportManifestBl.repository;

import com.asg.shipping.exportManifestBl.dto.BookingSelectionRowDto;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ExportManifestBlBookingSelectionRepositoryImpl implements ExportManifestBlBookingSelectionRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * {@code VW_PENDING_MATE_TO_BL} with legacy issue-voyage scope ({@code PendingMateBookingToBLView} /
     * {@code Pvoyagepoid}): mate triple matches {@code SHIP_VOYAGE_HDR}, or mate is linked on
     * {@code MATE_LOAD_VOYAGE_POID} (Booking Data tab assignment when header voyage/vessel differ).
     */
    private static final String PENDING_VIEW_SQL = """
            SELECT
                P.TRANSACTION_POID AS MATE_TRANSACTION_POID,
                P.DET_ROW_ID AS CONTAINER_DET_ROW_ID,
                P.BOOKING_ISSUE_NO AS BOOKING_ISSUE_NO,
                P.CONTAINER_NO AS CONTAINER_NO,
                P.VOYAGE_NO AS VOYAGE_NO,
                P.LINE_POID AS LINE_POID,
                P.LINE_NAME AS LINE_CODE,
                P.VESSEL_NAME AS VESSEL_NAME,
                P.EQUIPMENT_ISO_TYPE AS EQUIPMENT_ISO_TYPE,
                P.EQUIPMENT_SIZE AS EQUIPMENT_SIZE,
                P.DESTINATION_PORT_POID AS DESTINATION_PORT_POID,
                NULL AS DESTINATION_PORT_CODE,
                P.PORT_OF_DISCHARGE_NAME AS DESTINATION_PORT_NAME,
                P.SALESMAN_POID AS SALESMAN_POID,
                NULL AS SALESMAN_CODE,
                P.SALESMAN_NAME AS SALESMAN_NAME
            FROM VW_PENDING_MATE_TO_BL P
            WHERE P.GROUP_POID = ?
              AND P.COMPANY_POID = ?
              AND (
                    EXISTS (
                        SELECT 1
                        FROM SHIP_VOYAGE_HDR SV
                        WHERE SV.TRANSACTION_POID = ?
                          AND SV.GROUP_POID = ?
                          AND P.COMPANY_POID = SV.COMPANY_POID
                          AND P.VESSEL_POID = SV.VESSEL_POID
                          AND TRIM(P.VOYAGE_NO) = TRIM(SV.VOYAGE_NO)
                    )
                    OR P.MATE_LOAD_VOYAGE_POID = ?
                  )
            """;

    @Override
    public List<BookingSelectionRowDto> findPendingMateToBl(
            Long groupPoid,
            Long companyPoid,
            Long issueVesselVoyagePoid,
            String bookingMateVoyageNo,
            Long linePoid,
            String containerNo,
            String bookingNo) {

        StringBuilder sql = new StringBuilder(PENDING_VIEW_SQL);
        List<Object> params = new ArrayList<>();
        params.add(groupPoid);
        params.add(companyPoid);
        params.add(issueVesselVoyagePoid);
        params.add(groupPoid);
        params.add(issueVesselVoyagePoid);

        appendOptionalFilters(sql, params, bookingMateVoyageNo, linePoid, containerNo, bookingNo, "P");
        sql.append(" ORDER BY P.BOOKING_ISSUE_NO, P.DET_ROW_ID ");

        List<BookingSelectionRowDto> rows = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> mapRow(rs, issueVesselVoyagePoid, null),
                params.toArray());
        log.debug(
                "booking-selection issueVoyage={} mateVoyageNo={} linePoid={} -> {} row(s)",
                issueVesselVoyagePoid,
                bookingMateVoyageNo,
                linePoid,
                rows.size());
        return rows;
    }

    private static void appendOptionalFilters(
            StringBuilder sql,
            List<Object> params,
            String bookingMateVoyageNo,
            Long linePoid,
            String containerNo,
            String bookingNo,
            String viewAlias) {
        appendOptionalFilters(sql, params, bookingMateVoyageNo, linePoid, containerNo, bookingNo, viewAlias, viewAlias);
    }

    private static void appendOptionalFilters(
            StringBuilder sql,
            List<Object> params,
            String bookingMateVoyageNo,
            Long linePoid,
            String containerNo,
            String bookingNo,
            String hdrAlias,
            String containerAlias) {

        if (StringUtils.hasText(bookingMateVoyageNo)) {
            sql.append(" AND TRIM(").append(hdrAlias).append(".VOYAGE_NO) = TRIM(?) ");
            params.add(bookingMateVoyageNo.trim());
        }
        if (linePoid != null && linePoid != 0L) {
            sql.append(" AND ").append(hdrAlias).append(".LINE_POID = ? ");
            params.add(linePoid);
        }
        if (StringUtils.hasText(containerNo)) {
            sql.append(" AND UPPER(").append(containerAlias).append(".CONTAINER_NO) LIKE UPPER(?) ");
            params.add("%" + containerNo.trim() + "%");
        }
        if (StringUtils.hasText(bookingNo)) {
            sql.append(" AND UPPER(").append(hdrAlias).append(".BOOKING_ISSUE_NO) LIKE UPPER(?) ");
            params.add("%" + bookingNo.trim() + "%");
        }
    }

    private static BookingSelectionRowDto mapRow(
            java.sql.ResultSet rs, Long issueVesselVoyagePoid, String isOog) throws java.sql.SQLException {
        String size = rs.getString("EQUIPMENT_SIZE");
        String iso = rs.getString("EQUIPMENT_ISO_TYPE");
        String sizeOrOw = buildSizeOrOw(size, iso, isOog);

        return BookingSelectionRowDto.builder()
                .mateTransactionPoid(rs.getLong("MATE_TRANSACTION_POID"))
                .containerDetRowId(rs.getLong("CONTAINER_DET_ROW_ID"))
                .voyageTransactionPoid(issueVesselVoyagePoid)
                .voyageNo(rs.getString("VOYAGE_NO"))
                .vesselName(rs.getString("VESSEL_NAME"))
                .linePoid(rs.getObject("LINE_POID") != null ? rs.getLong("LINE_POID") : null)
                .lineCode(rs.getString("LINE_CODE"))
                .bookingIssueNo(rs.getString("BOOKING_ISSUE_NO"))
                .containerNo(rs.getString("CONTAINER_NO"))
                .equipmentSize(size)
                .equipmentIsoType(iso)
                .sizeOrOw(sizeOrOw)
                .destinationPortPoid(
                        rs.getObject("DESTINATION_PORT_POID") != null
                                ? rs.getLong("DESTINATION_PORT_POID")
                                : null)
                .destinationPortCode(rs.getString("DESTINATION_PORT_CODE"))
                .destinationPortName(rs.getString("DESTINATION_PORT_NAME"))
                .salesmanPoid(rs.getObject("SALESMAN_POID") != null ? rs.getLong("SALESMAN_POID") : null)
                .salesmanCode(rs.getString("SALESMAN_CODE"))
                .salesmanName(rs.getString("SALESMAN_NAME"))
                .isSelected("N")
                .build();
    }

    private static String buildSizeOrOw(String size, String iso, String isOog) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(size)) {
            sb.append(size.trim());
        }
        if (StringUtils.hasText(iso)) {
            if (!sb.isEmpty()) {
                sb.append('/');
            }
            sb.append(iso.trim());
        }
        if ("Y".equalsIgnoreCase(isOog)) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append("OOG");
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
