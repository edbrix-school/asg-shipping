package com.asg.shipping.mafitrailerdateupdateform.repository;

import com.asg.shipping.mafitrailerdateupdateform.entity.ShipBlMafiDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipBlMafiDtlRepository
        extends JpaRepository<ShipBlMafiDtl, ShipBlMafiDtl.CompositeKey> {

    List<ShipBlMafiDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    @Query(value = """
            SELECT
                d.TRANSACTION_POID,
                d.DET_ROW_ID,
                d.BL_POID,
                b.BL_NO            AS blNumber,
                d.MAFI_REF,
                d.MAFI_SIZE,
                d.MAFI_FREE_DAYS,
                d.BACK_LOAD_DATE,
                d.MAFI_EMPTY_DATE,
                d.REMARKS,
                d.CREATED_BY,
                d.CREATED_DATE,
                d.LASTMODIFIED_BY,
                d.LASTMODIFIED_DATE
            FROM SHIP_BL_MAFI_DTL d
            LEFT JOIN SHIP_BL_HDR b
                   ON b.BL_HDR_POID = d.BL_POID
            WHERE d.TRANSACTION_POID = :transactionPoid
            ORDER BY d.DET_ROW_ID
            """, nativeQuery = true)
    List<Object[]> findDetailsWithBlInfo(
            @Param("transactionPoid") Long transactionPoid
    );

    @Query(value = """
            SELECT COALESCE(MAX(d.DET_ROW_ID), 0) + 1
            FROM SHIP_BL_MAFI_DTL d
            WHERE d.TRANSACTION_POID = :transactionPoid
            """, nativeQuery = true)
    Long getNextDetRowId(@Param("transactionPoid") Long transactionPoid);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                UPDATE ShipBlMafiDtl d
                SET d.remarks = :remarks,
                    d.backLoadDate = :backLoadDate,
                    d.mafiEmptyDate = :mafiEmptyDate,
                    d.lastModifiedBy = :userId,
                    d.lastModifiedDate = CURRENT_TIMESTAMP
                WHERE d.id.transactionPoid = :transactionPoid
                  AND d.id.detRowId = :detRowId
            """)
    int updateByTransactionPoidAndDetRowId(
            @Param("transactionPoid") Long transactionPoid,
            @Param("detRowId") Long detRowId,
            @Param("remarks") String remarks,
            @Param("backLoadDate") LocalDate backLoadDate,
            @Param("mafiEmptyDate") LocalDate mafiEmptyDate,
            @Param("userId") String userId
    );

    @Query("SELECT COALESCE(MAX(u.detRowId), 0) FROM ShipBlMafiDtl u WHERE u.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(Long transactionPoid);

    Optional<ShipBlMafiDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);
}
