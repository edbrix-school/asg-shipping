package com.asg.shipping.commoditymaster.repository;

import com.asg.shipping.commoditymaster.entity.CommodityMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommodityMasterRepository extends JpaRepository<CommodityMaster, Long> {

    @Query("SELECT c FROM CommodityMaster c WHERE c.commodityPoid = :commodityPoid")
    Optional<CommodityMaster> findByCommodityPoid(@Param("commodityPoid") Long commodityPoid);

    @Query("SELECT c FROM CommodityMaster c WHERE c.commodityPoid = :commodityPoid AND (c.deleted IS NULL OR c.deleted != 'Y')")
    Optional<CommodityMaster> findActiveByCommodityPoid(@Param("commodityPoid") Long commodityPoid);

    @Query("SELECT c FROM CommodityMaster c WHERE c.commodityCode = :commodityCode")
    Optional<CommodityMaster> findByCommodityCode(@Param("commodityCode") String commodityCode);

    @Query("SELECT c FROM CommodityMaster c WHERE (c.deleted IS NULL OR c.deleted != 'Y') ORDER BY c.seqno, c.commodityName")
    List<CommodityMaster> findActive();
}