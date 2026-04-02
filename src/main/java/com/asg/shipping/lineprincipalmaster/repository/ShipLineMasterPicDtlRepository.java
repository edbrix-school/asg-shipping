package com.asg.shipping.lineprincipalmaster.repository;

import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterPicDtl;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterPicDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipLineMasterPicDtlRepository extends JpaRepository<ShipLineMasterPicDtl, ShipLineMasterPicDtlId> {

    List<ShipLineMasterPicDtl> findByLinePoidOrderByDetRowId(Long linePoid);

    @Query("SELECT MAX(p.detRowId) FROM ShipLineMasterPicDtl p WHERE p.linePoid = :linePoid")
    Long findMaxDetRowIdByLinePoid(@Param("linePoid") Long linePoid);
}
