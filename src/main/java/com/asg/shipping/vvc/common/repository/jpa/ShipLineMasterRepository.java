package com.asg.shipping.vvc.common.repository.jpa;

import com.asg.shipping.vvc.common.entity.ShipLineMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShipLineMasterRepository extends JpaRepository<ShipLineMasterEntity, Long> {

    @Query("select l.companyPoid from ShipLineMasterEntity l where l.linePoid = :linePoid")
    Optional<Long> findCompanyPoidByLinePoid(@Param("linePoid") Long linePoid);

    @Query("select l.lineCode from ShipLineMasterEntity l where l.linePoid = :linePoid")
    Optional<String> findLineCodeByLinePoid(@Param("linePoid") Long linePoid);
}


