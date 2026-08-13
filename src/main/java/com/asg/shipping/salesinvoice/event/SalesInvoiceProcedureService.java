package com.asg.shipping.salesinvoice.event;

import com.asg.common.lib.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;

import static com.asg.common.lib.security.util.UserContext.getUserPoid;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesInvoiceProcedureService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void callProcShipBlPageSaveAfter(Long groupPoid, Long companyPoid, Long transactionPoid, String flag) {
        try {
            String sql = "{call PROC_SHIP_BL_PAGE_SAVE_AFTER(?, ?, ?, ?, ?, ?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setLong(3, transactionPoid);
                cs.setString(4, null);
                cs.setString(5, flag);
                cs.setLong(6, getUserPoid());
                cs.execute();

                log.info(
                        "PROC_SHIP_BL_PAGE_SAVE_AFTER completed successfully for transactionPoid={}",
                        transactionPoid
                );

                return null;
            });
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_BL_PAGE_SAVE_AFTER for transactionPoid={}", transactionPoid, e);
            throw new ValidationException("Error in post-save processing: " + e.getMessage());
        }
    }
}
