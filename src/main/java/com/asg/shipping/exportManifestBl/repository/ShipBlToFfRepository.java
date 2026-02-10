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

   
    public Optional<ShipBlToFfDto> findByBlNumber(String blNumber, Long companyPoid) {
        String sql = "SELECT RNUMID, MASTER_BL_NO, SHIPPING_MANIFEST_POID, SHIPPING_INVOICE, " +
                "FF_JOBNO, FF_INVOICE, FF_PJ, DELETED " +
                "FROM VW_SHIP_BL_TO_FF " +
                "WHERE MASTER_BL_NO = ? AND COMPANY_POID = ?";

        try {
            List<ShipBlToFfDto> results = jdbcTemplate.query(sql, new ShipBlToFfRowMapper(), blNumber, companyPoid);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            // Log error and return empty
            return Optional.empty();
        }
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

