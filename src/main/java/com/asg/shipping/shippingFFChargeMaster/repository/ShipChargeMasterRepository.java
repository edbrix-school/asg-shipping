package com.asg.shipping.shippingFFChargeMaster.repository;

import com.asg.shipping.shippingFFChargeMaster.entity.ShipChargeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipChargeMasterRepository extends JpaRepository<ShipChargeMaster, Long> {


    Optional<ShipChargeMaster> findByChargePoid(Long chargePoid);

    boolean existsByChargeCodeAndDivisionCodeAndDeletedNot(
            String chargeCode,
            String divisionCode,
            String deleted
    );

    boolean existsByChargeNameAndDivisionCodeAndDeletedNot(
            String chargeName,
            String divisionCode,
            String deleted
    );

/*    boolean existsByChargeCodeAndDivisionCodeAndChargePoidNotAndDeletedNot(
            String chargeCode,
            String divisionCode,
            Long chargePoid,
            String deleted
    );*/

    boolean existsByChargeNameAndDivisionCodeAndChargePoidNotAndDeletedNot(
            String chargeName,
            String divisionCode,
            Long chargePoid,
            String deleted
    );
}
