package com.asg.shipping.deliveryorderissuetocustomer.repository;

import com.asg.shipping.deliveryorderissuetocustomer.entity.ShipBlManifestHDR;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ShipBlManifestHDRRepository extends JpaRepository<ShipBlManifestHDR, Long> {
}

