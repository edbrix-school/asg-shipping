package com.asg.shipping.shippingofoqv2.repository;

import com.asg.shipping.shippingofoqv2.entity.OfoqApiXmlDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfoqApiXmlDataRepository extends JpaRepository<OfoqApiXmlDataEntity, Long> {

}