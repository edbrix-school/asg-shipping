package com.asg.shipping.remuneration.repository;

import com.asg.shipping.remuneration.entity.ShipRemunerationMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipRemunerationMasterRepository extends JpaRepository<ShipRemunerationMaster, Long> {
    boolean existsByRemunCodeIgnoreCase(String remunCode);
}

