package com.asg.shipping.common.repository;

import com.asg.shipping.common.entity.GlMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlMasterRepository extends JpaRepository<GlMaster, Long> {
    boolean existsByGlPoid(Long glPoid);
}
