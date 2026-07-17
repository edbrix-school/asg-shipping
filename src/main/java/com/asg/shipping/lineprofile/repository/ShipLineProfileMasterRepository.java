package com.asg.shipping.lineprofile.repository;

import com.asg.shipping.lineprofile.entity.ShipLineProfileMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipLineProfileMasterRepository extends JpaRepository<ShipLineProfileMasterEntity, Long> {
    Optional<ShipLineProfileMasterEntity> findByLineProfilePoidAndGroupPoid(Long lineProfilePoid, Long groupPoid);

    boolean existsByLinePoidAndGroupPoidAndDeleted(Long linePoid, Long groupPoid, String deleted);

    boolean existsByLinePoidAndGroupPoidAndDeletedAndLineProfilePoidNot(Long linePoid, Long groupPoid, String deleted, Long lineProfilePoid);
}

