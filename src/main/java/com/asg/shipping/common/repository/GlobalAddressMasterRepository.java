package com.asg.shipping.common.repository;

import com.asg.shipping.common.entity.GlobalAddressMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalAddressMasterRepository extends JpaRepository<GlobalAddressMaster, Long> {
}
