package com.asg.shipping.tradelanemaster.repository;

import com.asg.shipping.tradelanemaster.entity.ShipTradelaneMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;



@Repository
public interface ShipTradeLaneMasterRepository extends JpaRepository<ShipTradelaneMaster, Long> {

    @Query("SELECT COUNT(t) > 0 FROM ShipTradelaneMaster t WHERE t.tradeLaneCode = :tradeLaneCode AND t.tradeLanePoid != :excludeTradeLanePoid")
    boolean existsByTradeLaneCodeExcluding(
            @Param("tradeLaneCode") String tradeLaneCode,
            @Param("excludeTradeLanePoid") Long excludeTradeLanePoid
    );

    @Query("SELECT COUNT(t) > 0 FROM ShipTradelaneMaster t WHERE t.tradeLaneName = :tradeLaneName AND t.tradeLanePoid != :excludeTradeLanePoid")
    boolean existsByTradeLaneNameExcluding(
            @Param("tradeLaneName") String tradeLaneName,
            @Param("excludeTradeLanePoid") Long excludeTradeLanePoid
    );
    
    boolean existsByTradeLaneCode(String tradeLaneCode);
    
    boolean existsByTradeLaneName(String tradeLaneName);
    
}
