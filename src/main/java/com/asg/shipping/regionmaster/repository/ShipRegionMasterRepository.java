package com.asg.shipping.regionmaster.repository;

import com.asg.shipping.regionmaster.entity.ShipRegionMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipRegionMasterRepository extends JpaRepository<ShipRegionMasterEntity, Long> {
    
    Optional<ShipRegionMasterEntity> findByRegionPoidAndGroupPoid(Long regionPoid, Long groupPoid);
    
    boolean existsByRegionCodeAndGroupPoidAndDeletedNot(String regionCode, Long groupPoid, String deleted);
    
    boolean existsByRegionNameAndGroupPoidAndDeletedNot(String regionName, Long groupPoid, String deleted);
    
    boolean existsByRegionCodeAndGroupPoidAndDeletedNotAndRegionPoidNot(String regionCode, Long groupPoid, String deleted, Long excludePoid);
    
    boolean existsByRegionNameAndGroupPoidAndDeletedNotAndRegionPoidNot(String regionName, Long groupPoid, String deleted, Long excludePoid);
}

