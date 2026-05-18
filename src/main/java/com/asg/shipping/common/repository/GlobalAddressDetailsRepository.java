package com.asg.shipping.common.repository;

import com.asg.shipping.common.entity.GlobalAddressDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GlobalAddressDetailsRepository extends JpaRepository<GlobalAddressDetails, Long> {

    Optional<GlobalAddressDetails> findByAddressMasterPoidAndAddressType(Long addressMasterPoid, String addressType);

}
