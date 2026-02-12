package com.asg.shipping.lineprofile.repository;

import com.asg.shipping.lineprofile.entity.ShipLineProfileContactDtlEntity;
import com.asg.shipping.lineprofile.entity.ShipLineProfileContactDtlId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipLineProfileContactDtlRepository extends JpaRepository<ShipLineProfileContactDtlEntity, ShipLineProfileContactDtlId> {
    List<ShipLineProfileContactDtlEntity> findByLineProfilePoidOrderByDetRowId(Long lineProfilePoid);
}

