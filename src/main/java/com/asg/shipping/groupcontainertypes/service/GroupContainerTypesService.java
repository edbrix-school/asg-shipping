package com.asg.shipping.groupcontainertypes.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.groupcontainertypes.dto.ContainerGroupCreateDTO;
import com.asg.shipping.groupcontainertypes.dto.ContainerGroupDto;
import com.asg.shipping.groupcontainertypes.dto.ContainerGroupUpdateDTO;

import java.util.Map;

public interface GroupContainerTypesService {
    Map<String, Object> searchContainerGroups(String docId, FilterRequestDto request, org.springframework.data.domain.Pageable pageable);
    ContainerGroupDto getContainerGroup(Long id);
    ContainerGroupDto createContainerGroup(ContainerGroupCreateDTO dto, Long groupPoid, Long userPoid);
    ContainerGroupDto updateContainerGroup(Long id, ContainerGroupUpdateDTO dto, Long groupPoid, Long userPoid);
    void toggleActive(Long id);
    void deleteContainerGroup(Long id, DeleteReasonDto deleteReasonDto);
}
