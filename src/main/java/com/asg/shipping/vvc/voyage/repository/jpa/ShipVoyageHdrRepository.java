package com.asg.shipping.vvc.voyage.repository.jpa;

import com.asg.shipping.vvc.voyage.entity.ShipVoyageHdrEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShipVoyageHdrRepository extends JpaRepository<ShipVoyageHdrEntity, Long> {

    Optional<ShipVoyageHdrEntity> findByTransactionPoidAndGroupPoid(Long transactionPoid, Long groupPoid);

    boolean existsByGroupPoidAndLinePoidAndVesselPoidAndVoyageNo(Long groupPoid, Long linePoid, Long vesselPoid, String voyageNo);

    @Query("""
            select case when count(v) > 0 then true else false end
            from ShipVoyageHdrEntity v
            where v.groupPoid = :groupPoid
              and v.linePoid = :linePoid
              and v.vesselPoid = :vesselPoid
              and v.voyageNo = :voyageNo
              and v.transactionPoid <> :excludePoid
            """)
    boolean existsDuplicateExcludingPoid(
            @Param("groupPoid") Long groupPoid,
            @Param("linePoid") Long linePoid,
            @Param("vesselPoid") Long vesselPoid,
            @Param("voyageNo") String voyageNo,
            @Param("excludePoid") Long excludePoid
    );
}


