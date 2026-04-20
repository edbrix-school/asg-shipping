package com.asg.shipping.vesselvoyagecreation.repository;

import com.asg.shipping.vesselvoyagecreation.entity.ShipLineMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VoyageLineMasterRepository extends JpaRepository<ShipLineMasterEntity, Long> {

    @Query("select l.companyPoid from ShipLineMasterEntity l where l.linePoid = :linePoid")
    Optional<Long> findCompanyPoidByLinePoid(@Param("linePoid") Long linePoid);

    @Query("select l.lineCode from ShipLineMasterEntity l where l.linePoid = :linePoid")
    Optional<String> findLineCodeByLinePoid(@Param("linePoid") Long linePoid);

    // Used to detect MSC line for conditional UI rendering (legacy RenderMscLineVesselVoyage)
    @Query("select count(l) > 0 from ShipLineMasterEntity l where l.lineCode = 'MSC' and l.linePoid = :linePoid")
    boolean isMscLine(@Param("linePoid") Long linePoid);
}