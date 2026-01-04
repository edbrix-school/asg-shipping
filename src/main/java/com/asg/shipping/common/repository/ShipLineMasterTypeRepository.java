package com.asg.shipping.common.repository;

import com.asg.shipping.common.entity.ShipLineMasterType;
import com.asg.shipping.common.entity.ShipLineMasterTypeId;
import com.asg.shipping.containertypes.entity.ShipContainerTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShipLineMasterTypeRepository extends JpaRepository<ShipLineMasterType, ShipLineMasterTypeId> {

    /**
     * Load container types linked to a Line (SHIP_LINE_MASTER_TYPE_DTL.LINE_POID),
     * returning master entities from SHIP_CONTAINER_TYPE_MASTER.
     */
    @Query("""
            select distinct m
            from ShipLineMasterType d, ShipContainerTypeMaster m
            where d.containerTypePoid = m.containerTypePoid
              and d.linePoid = :linePoid
              and d.containerTypePoid is not null
            order by m.containerTypeCode
            """)
    List<ShipContainerTypeMaster> findContainerTypeMastersByLine(@Param("linePoid") Long linePoid);
}



