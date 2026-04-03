package com.asg.shipping.lineprincipalmaster.repository;

import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterUserRoleDtl;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterUserRoleDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipLineMasterUserRoleDtlRepository extends JpaRepository<ShipLineMasterUserRoleDtl, ShipLineMasterUserRoleDtlId> {

    List<ShipLineMasterUserRoleDtl> findByLinePoidOrderByDetRowId(Long linePoid);

    @Query("SELECT MAX(r.detRowId) FROM ShipLineMasterUserRoleDtl r WHERE r.linePoid = :linePoid")
    Long findMaxDetRowIdByLinePoid(@Param("linePoid") Long linePoid);

    @Query("SELECT COUNT(r) > 0 FROM ShipLineMasterUserRoleDtl r WHERE r.linePoid = :linePoid AND r.userRolePoid = :userRolePoid")
    boolean existsByLinePoidAndUserRolePoid(@Param("linePoid") Long linePoid, @Param("userRolePoid") Long userRolePoid);

    @Query("SELECT COUNT(r) > 0 FROM ShipLineMasterUserRoleDtl r WHERE r.linePoid = :linePoid AND r.userRolePoid = :userRolePoid AND r.detRowId != :detRowId")
    boolean existsByLinePoidAndUserRolePoidExcluding(@Param("linePoid") Long linePoid, @Param("userRolePoid") Long userRolePoid, @Param("detRowId") Long detRowId);
}
