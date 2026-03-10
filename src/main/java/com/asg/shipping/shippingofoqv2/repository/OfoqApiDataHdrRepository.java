package com.asg.shipping.shippingofoqv2.repository;


import com.asg.shipping.shippingofoqv2.entity.OfoqApiDataHdrEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OfoqApiDataHdrRepository extends JpaRepository<OfoqApiDataHdrEntity, Long> {

    Optional<OfoqApiDataHdrEntity> findByTransactionPoid(Long transactionPoid);
}