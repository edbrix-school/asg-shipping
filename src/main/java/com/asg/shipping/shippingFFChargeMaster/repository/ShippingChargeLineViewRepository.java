package com.asg.shipping.shippingFFChargeMaster.repository;

import com.asg.shipping.shippingFFChargeMaster.entity.ShippingChargeLineViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShippingChargeLineViewRepository   extends JpaRepository<ShippingChargeLineViewEntity, Long> {

    List<ShippingChargeLineViewEntity> findByChargePoid(Long chargePoid);
}
