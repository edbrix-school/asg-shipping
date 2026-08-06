package com.asg.shipping.shippingffchargemaster.repository;

import com.asg.shipping.shippingffchargemaster.entity.ShipChargeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipChargeMasterRepository extends JpaRepository<ShipChargeMaster, Long> {


    Optional<ShipChargeMaster> findByChargePoid(Long chargePoid);

    List<ShipChargeMaster> findByChargePoidIn(List<Long> chargePoids);

    boolean existsByChargeCodeAndDivisionCode(String chargeCode, String divisionCode);

    boolean existsByChargeNameIgnoreCaseAndDivisionCode(String chargeName, String divisionCode);

    boolean existsByChargeNameIgnoreCaseAndDivisionCodeAndChargePoidNot(
            String chargeName, String divisionCode, Long chargePoid);

    boolean existsByChargeCodeAndDivisionCodeAndChargePoidNot(
            String chargeCode, String divisionCode, Long chargePoid);

    boolean existsByChargePoid(long chargePoid);
}
