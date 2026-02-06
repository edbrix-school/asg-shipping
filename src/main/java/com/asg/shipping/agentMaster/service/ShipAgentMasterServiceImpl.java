package com.asg.shipping.agentMaster.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.agentMaster.dto.ShipAgentMasterRequestDto;
import com.asg.shipping.agentMaster.dto.ShipAgentMasterResponseDto;
import com.asg.shipping.agentMaster.entity.ShipAgentMasterEntity;
import com.asg.shipping.agentMaster.repository.ShipAgentMasterRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ShipAgentMasterServiceImpl implements ShipAgentMasterService{

    private final ShipAgentMasterRepository repository;
    private final DocumentSearchService documentService;
    private final LoggingService loggingService;


    @Override
    public ShipAgentMasterResponseDto createAgentMaster(ShipAgentMasterRequestDto request) {
        log.info("Creating agent master with name: {}", request.getAgentName());

        ShipAgentMasterEntity entity = ShipAgentMasterEntity.builder()
                .groupPoid(UserContext.getGroupPoid())
                .agentName(request.getAgentName())
                .agentName2(request.getAgentName2())
                .contactPerson(request.getContactPerson())
                .details(request.getDetails())
                .linePoid(request.getLinePoid())
                .linePoid(Long.valueOf(request.getLinePoid().toString()))
                .portPoid(request.getPortPoid())
                .portPoid(Long.valueOf(request.getPortPoid().toString()))
                .email(request.getEmail() != null && !request.getEmail().isEmpty() ? String.join(",", request.getEmail()) : null)
                .contactNo(request.getContactNo())
                .faxNo(request.getFaxNo())
                .remarks(request.getRemarks())
                .seqNo(request.getSeqNo())
                .countryPoid(request.getCountryPoid())
                .active(Boolean.TRUE.equals(request.getActive()) ? "Y" : "N")
                .deleted("N")
                .createdBy(getCurrentUser())
                .createdDate(LocalDateTime.now())
                .build();

        ShipAgentMasterEntity saved = repository.save(entity);

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), saved.getAgentPoid().toString());
        log.debug("Create action logged for agent master id: {}", saved.getAgentPoid());

        log.info("Successfully created agent master with id: {}", saved.getAgentPoid());
        return mapToResponse(saved);
    }

    @Override
    public ShipAgentMasterResponseDto updateAgentMaster(Long agentPoid, ShipAgentMasterRequestDto request) {
        log.info("Updating agent master with id: {}", agentPoid);

        ShipAgentMasterEntity entity = repository.findById(agentPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with","AgentPoid",agentPoid));

        ShipAgentMasterEntity oldEntity = new ShipAgentMasterEntity();
        BeanUtils.copyProperties(entity, oldEntity);

        entity.setAgentName(request.getAgentName());
        entity.setAgentName2(request.getAgentName2());
        entity.setContactPerson(request.getContactPerson());
        entity.setDetails(request.getDetails());
        entity.setLinePoid(request.getLinePoid());
        entity.setCountryPoid(request.getCountryPoid());
        entity.setPortPoid(request.getPortPoid());
        entity.setEmail(request.getEmail() != null && !request.getEmail().isEmpty() ? String.join(",", request.getEmail()) : null);
        entity.setContactNo(request.getContactNo());
        entity.setFaxNo(request.getFaxNo());
        entity.setRemarks(request.getRemarks());
        entity.setSeqNo(request.getSeqNo());
        entity.setActive(Boolean.TRUE.equals(request.getActive()) ? "Y" : "N");
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());

        ShipAgentMasterEntity shipAgentMasterEntity =  repository.save(entity);

        log.debug("Logging changes for agent master update");
        loggingService.logChanges(oldEntity, shipAgentMasterEntity, ShipAgentMasterEntity.class, UserContext.getDocumentId(), agentPoid.toString(), LogDetailsEnum.MODIFIED, "AGENT_POID");
        log.debug("Changes logged successfully");

        log.info("Successfully updated agent master with id: {}", agentPoid);
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public ShipAgentMasterResponseDto findByIdAgentMaster(Long agentPoid) {
        log.info("Getting agent master with id: {}", agentPoid);
        ShipAgentMasterResponseDto response = repository.findById(agentPoid)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with","AgentPoid",agentPoid));

        log.debug("View action logged for agent master id: {}", agentPoid);

        log.info("Successfully retrieved agent master with id: {}", agentPoid);
        return response;
    }


    @Override
    public void deleteAgentMaster(Long agentPoid) {
        log.info("Deleting agent master with id: {}", agentPoid);

        ShipAgentMasterEntity entity = repository.findById(agentPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with","AgentPoid",agentPoid));

        if ("Y".equals(entity.getDeleted())) {
            log.info("Agent master with id: {} is already deleted", agentPoid);
            return;
        }

        entity.setDeleted("Y");
        entity.setActive("N");
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());

        repository.save(entity);

        log.debug("Logging delete action for agent master");
        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, UserContext.getDocumentId(), agentPoid.toString());
        String logDetail = String.format("KeyId = AGENT_POID:%s", agentPoid);
        String tableName = ShipAgentMasterEntity.class.getAnnotation(jakarta.persistence.Table.class).name();
        loggingService.createLogDetailsEntry(UserContext.getDocumentId(), agentPoid.toString(), "Deleted", "N", "Y", logDetail, tableName);
        loggingService.createLogDetailsEntry(UserContext.getDocumentId(), agentPoid.toString(), "Active", "Y", "N", logDetail, tableName);
        log.debug("Delete action logged successfully");

        log.info("Successfully deleted agent master with id: {}", agentPoid);
    }

    private ShipAgentMasterResponseDto mapToResponse(ShipAgentMasterEntity e) {
        return ShipAgentMasterResponseDto.builder()
                .agentPoid(e.getAgentPoid())
                .groupPoid(e.getGroupPoid())
                .agentName(e.getAgentName())
                .agentName2(e.getAgentName2())
                .contactPerson(e.getContactPerson())
                .details(e.getDetails())
                .linePoid(e.getLinePoid())
                .portPoid(e.getPortPoid())
                .email(e.getEmail())
                .contactNo(e.getContactNo())
                .faxNo(e.getFaxNo())
                .countryPoid(e.getCountryPoid())
                .remarks(e.getRemarks())
                .seqNo(e.getSeqNo())
                .active(e.getActive())
                .createdBy(e.getCreatedBy())
                .createdDate(e.getCreatedDate())
                .lastModifiedBy(e.getLastModifiedBy())
                .lastModifiedDate(e.getLastModifiedDate())
                .build();
    }

    @Override
    @Transactional
    public Map<String, Object> listAgents(String docId, com.asg.common.lib.dto.FilterRequestDto request, Pageable pageable) {
        log.info("Listing agents with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "AGENT_NAME",
                "AGENT_POID"
        );

        Page<Map<String, Object>> page = new PageImpl<>(
                raw.records(),
                pageable,
                raw.totalRecords()
        );

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    public static String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }
}
