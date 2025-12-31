package com.asg.shipping.address.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressDetailsRepository extends JpaRepository<AddressDetails, String> {
    List<AddressDetails> findByAddressMasterPoidAndAddressType(Long addressMasterPoid, String addressType);
}
