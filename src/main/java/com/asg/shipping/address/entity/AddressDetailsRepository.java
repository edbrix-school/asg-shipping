package com.asg.shipping.address.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AddressDetailsRepository extends JpaRepository<AddressDetails, String> {
    List<AddressDetails> findByAddressMasterPoidAndAddressType(Long addressMasterPoid, String addressType);

    List<AddressDetails> findByAddressMasterPoid(Long addressMasterPoid);
    List<AddressDetails> findByAddressMasterPoidAndAddressType(BigDecimal addressMasterPoid, String addressType);
}
