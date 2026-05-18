package com.asg.shipping.bookingFormSH.repository;

import com.asg.shipping.bookingFormSH.entity.ShipMateStuffingDtl;
import com.asg.shipping.bookingFormSH.entity.ShipMateStuffingDtlld;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShipMateStuffingDtlRepository  extends JpaRepository<ShipMateStuffingDtl, ShipMateStuffingDtlld> {

    List<ShipMateStuffingDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);

    Optional<ShipMateStuffingDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    void deleteByTransactionPoid(Long transactionPoid);

    void deleteByTransactionPoidAndDetRowIdIn( Long transactionPoid, List<Long> detRowIds);

    @Query("SELECT COALESCE(MAX(d.detRowId), 0) FROM ShipMateStuffingDtl d WHERE d.transactionPoid = :transactionPoid")
    Long getMaxDetRowId(@Param("transactionPoid") Long transactionPoid);

}
