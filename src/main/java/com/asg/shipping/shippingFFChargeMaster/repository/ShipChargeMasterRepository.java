package com.asg.shipping.shippingFFChargeMaster.repository;

import com.asg.shipping.shippingFFChargeMaster.entity.ShipChargeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ShipChargeMasterRepository extends JpaRepository<ShipChargeMaster, Long> {


    Optional<ShipChargeMaster> findByChargePoid(Long chargePoid);

    boolean existsByChargeCodeAndDivisionCode(String chargeCode, String divisionCode);

    boolean existsByChargeNameIgnoreCaseAndDivisionCode(String chargeName, String divisionCode);

    boolean existsByChargeNameIgnoreCaseAndDivisionCodeAndChargePoidNot(
            String chargeName, String divisionCode, Long chargePoid);

    boolean existsByChargeCodeAndDivisionCodeAndChargePoidNot(
            String chargeCode, String divisionCode, Long chargePoid);

    boolean existsByChargePoid(long chargePoid);
}
