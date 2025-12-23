package com.asg.shipping.common.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.asg.shipping.common.entity.GlobalCountryMaster;

@Repository
public interface GlobalCountryMasterRepository extends JpaRepository<GlobalCountryMaster, Long> {

	Optional<GlobalCountryMaster> findById(Long countryPoid);
}
