package com.asg.shipping.common.repository;

import com.asg.shipping.common.entity.ShipChargeMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;

public interface ShipChargeMasterRepository extends JpaRepository<ShipChargeMaster, BigDecimal> {
    boolean existsByChargePoid(BigDecimal chargePoid);
}
