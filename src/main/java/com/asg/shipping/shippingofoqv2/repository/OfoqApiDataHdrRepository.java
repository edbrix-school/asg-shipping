package com.asg.shipping.shippingofoqv2.repository;


import com.asg.shipping.shippingofoqv2.entity.OfoqApiDataHdrEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface OfoqApiDataHdrRepository extends JpaRepository<OfoqApiDataHdrEntity, Long> {

    Optional<OfoqApiDataHdrEntity> findByTransactionPoid(Long transactionPoid);

    List<OfoqApiDataHdrEntity> findByCompanyPoid(Long companyPoid);

    List<OfoqApiDataHdrEntity> findByVesselPoid(Long vesselPoid);

    @Query("SELECT o FROM OfoqApiDataHdrEntity o WHERE o.arrivalDate BETWEEN :fromDate AND :toDate")
    List<OfoqApiDataHdrEntity> findByArrivalDateBetween(@Param("fromDate") Date fromDate, @Param("toDate") Date toDate);

    @Query("SELECT o FROM OfoqApiDataHdrEntity o WHERE o.deleted = 'N' OR o.deleted IS NULL")
    List<OfoqApiDataHdrEntity> findAllActive();

    List<OfoqApiDataHdrEntity> findByVesselNameContainingIgnoreCase(String vesselName);

    List<OfoqApiDataHdrEntity> findByVoyageNo(String voyageNo);

    @Query("SELECT o FROM OfoqApiDataHdrEntity o WHERE o.companyPoid = :companyPoid AND (o.deleted = 'N' OR o.deleted IS NULL)")
    List<OfoqApiDataHdrEntity> findActiveByCompanyPoid(@Param("companyPoid") Long companyPoid);
}