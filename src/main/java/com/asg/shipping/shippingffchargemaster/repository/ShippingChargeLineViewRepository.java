package com.asg.shipping.shippingffchargemaster.repository;

import com.asg.shipping.shippingffchargemaster.entity.ShippingChargeLineViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShippingChargeLineViewRepository   extends JpaRepository<ShippingChargeLineViewEntity, Long> {

    List<ShippingChargeLineViewEntity> findByChargePoid(Long chargePoid);
}
