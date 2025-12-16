package com.asg.shipping.common.repository;

import com.asg.shipping.common.entity.GlobalAddressDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalAddressDetailsRepository extends JpaRepository<GlobalAddressDetails, Long> {
}
