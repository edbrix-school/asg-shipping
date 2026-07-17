package com.asg.shipping.regionmaster.repository;

import com.asg.shipping.regionmaster.entity.ShipRegionMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipRegionMasterRepository extends JpaRepository<ShipRegionMasterEntity, Long> {
    
    Optional<ShipRegionMasterEntity> findByRegionPoidAndGroupPoid(Long regionPoid, Long groupPoid);
    
    boolean existsByRegionCodeIgnoreCaseAndGroupPoidAndDeletedNot(String regionCode, Long groupPoid, String deleted);

    boolean existsByRegionNameIgnoreCaseAndGroupPoidAndDeletedNot(String regionName, Long groupPoid, String deleted);

    boolean existsByRegionCodeIgnoreCaseAndGroupPoidAndDeletedNotAndRegionPoidNot(String regionCode, Long groupPoid, String deleted, Long excludePoid);

    boolean existsByRegionNameIgnoreCaseAndGroupPoidAndDeletedNotAndRegionPoidNot(String regionName, Long groupPoid, String deleted, Long excludePoid);
}

