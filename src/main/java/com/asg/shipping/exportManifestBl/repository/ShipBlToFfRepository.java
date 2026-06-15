package com.asg.shipping.exportManifestBl.repository;

import com.asg.shipping.exportManifestBl.dto.ShipBlToFfDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ShipBlToFfRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String BASE_SQL =
            "SELECT v.RNUMID, v.MASTER_BL_NO, v.SHIPPING_MANIFEST_POID, v.SHIPPING_INVOICE, " +
            "v.FF_JOBNO, v.FF_INVOICE, v.FF_PJ, v.DELETED " +
            "FROM VW_SHIP_BL_TO_FF v " +
            "INNER JOIN SHIP_BL_MANIFEST_HDR h ON h.TRANSACTION_POID = v.SHIPPING_MANIFEST_POID " +
            "WHERE NVL(v.DELETED, 'N') = 'N' " +
            "AND NVL(h.DELETED, 'N') = 'N' ";

    public List<ShipBlToFfDto> findAllByMasterBlNo(String masterBlNo, Long companyPoid) {
        String sql = BASE_SQL + "AND v.MASTER_BL_NO = ? AND h.COMPANY_POID = ? ORDER BY v.RNUMID";
        return jdbcTemplate.query(sql, new ShipBlToFfRowMapper(), masterBlNo, companyPoid);
    }

    public List<ShipBlToFfDto> findAllByManifestPoid(Long transactionPoid, Long companyPoid) {
        String sql = BASE_SQL + "AND v.SHIPPING_MANIFEST_POID = ? AND h.COMPANY_POID = ? ORDER BY v.RNUMID";
        return jdbcTemplate.query(sql, new ShipBlToFfRowMapper(), transactionPoid, companyPoid);
    }

    public Optional<ShipBlToFfDto> findByBlNumber(String blNumber, Long companyPoid) {
        List<ShipBlToFfDto> results = findAllByMasterBlNo(blNumber, companyPoid);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<ShipBlToFfDto> findByRnumidAndManifestPoid(Long rnumid, Long transactionPoid, Long companyPoid) {
        String sql = BASE_SQL + "AND v.RNUMID = ? AND v.SHIPPING_MANIFEST_POID = ? AND h.COMPANY_POID = ?";
        List<ShipBlToFfDto> results = jdbcTemplate.query(sql, new ShipBlToFfRowMapper(), rnumid, transactionPoid, companyPoid);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    private static class ShipBlToFfRowMapper implements RowMapper<ShipBlToFfDto> {
        @Override
        public ShipBlToFfDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return ShipBlToFfDto.builder()
                    .rnumid(rs.getObject("RNUMID") != null ? rs.getLong("RNUMID") : null)
                    .masterBlNo(rs.getString("MASTER_BL_NO"))
                    .shippingManifestPoid(rs.getObject("SHIPPING_MANIFEST_POID") != null ?
                            rs.getLong("SHIPPING_MANIFEST_POID") : null)
                    .shippingInvoice(rs.getString("SHIPPING_INVOICE"))
                    .ffJobno(rs.getString("FF_JOBNO"))
                    .ffInvoice(rs.getString("FF_INVOICE"))
                    .ffPj(rs.getString("FF_PJ"))
                    .deleted(rs.getString("DELETED"))
                    .build();
        }
    }
}
