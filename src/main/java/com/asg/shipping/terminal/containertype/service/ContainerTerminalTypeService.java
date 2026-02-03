package com.asg.shipping.terminal.containertype.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.terminal.containertype.dto.ContainerTerminalTypeRequest;
import com.asg.shipping.terminal.containertype.dto.ContainerTerminalTypeResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ContainerTerminalTypeService {
    Map<String, Object> listContainerTerminalTypes(
            String docId,
            FilterRequestDto request,
            Pageable pageable);

    ContainerTerminalTypeResponse getById(Long poid, Long groupPoid);

    ContainerTerminalTypeResponse create(
            ContainerTerminalTypeRequest request,
            Long groupPoid,
            String userId,
            String docId);

    ContainerTerminalTypeResponse update(
            Long poid,
            ContainerTerminalTypeRequest request,
            Long groupPoid,
            String userId,
            String docId);

    void toggleActiveStatus(
            Long poid,
            Long groupPoid,
            String userId
    );

    void delete(
            Long poid,
            Long groupPoid,
            String userId
    );

}
