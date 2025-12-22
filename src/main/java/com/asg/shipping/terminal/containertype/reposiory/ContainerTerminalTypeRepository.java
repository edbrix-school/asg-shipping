package com.asg.shipping.terminal.containertype.reposiory;

import com.asg.shipping.terminal.containertype.entity.ContainerTerminalTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContainerTerminalTypeRepository extends JpaRepository<ContainerTerminalTypeEntity, Long> {
    Optional<ContainerTerminalTypeEntity> findByContainerTerminalTypePoidAndGroupPoid(Long poid, Long groupPoid);

    boolean existsByContainerTerminalTypeCodeAndGroupPoid(String code, Long groupPoid);

    boolean existsByContainerTerminalTypeNameAndGroupPoid(String name, Long groupPoid);

}
