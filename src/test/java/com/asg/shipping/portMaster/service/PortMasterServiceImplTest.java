package com.asg.shipping.portMaster.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.shipping.portMaster.dto.PortMasterRequest;
import com.asg.shipping.portMaster.entity.PortMaster;
import com.asg.shipping.portMaster.entity.PortMasterId;
import com.asg.shipping.portMaster.repository.PortMasterRepository;

@ExtendWith(MockitoExtension.class)
class PortMasterServiceImplTest {

	@Mock
	private PortMasterRepository repository;

	@Mock
	private DocumentSearchService documentService;

	@InjectMocks
	private PortMasterServiceImpl service;

	private PortMaster entity;

	@BeforeEach
	void setup() {
		entity = new PortMaster();
		entity.setPortPoid(1L);
		entity.setPortCode("P001");
		entity.setPortName("Port A");
		entity.setActive("Y");
		entity.setCreatedDate(LocalDateTime.now());
	}

	@Test
	void createPort_Success() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P003");
		request.setPortName("Port C");
		request.setActive("Y");

		when(repository.findByGroupPoidAndPortCode(anyLong(), eq("P003"))).thenReturn(Optional.empty());
		when(repository.findByGroupPoidAndPortName(anyLong(), eq("Port C"))).thenReturn(Optional.empty());

		service.createPort(100L, request, "admin");
	}

	@Test
	void createPort_PortCodeExists_Throws() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P001");
		request.setPortName("Port C");

		when(repository.findByGroupPoidAndPortCode(anyLong(), eq("P001"))).thenReturn(Optional.of(new PortMaster()));

		assertThrows(RuntimeException.class, () -> service.createPort(100L, request, "admin"));
	}

	@Test
	void createPort_PortNameExists_Throws() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P003");
		request.setPortName("Port A");

		when(repository.findByGroupPoidAndPortCode(anyLong(), eq("P003"))).thenReturn(Optional.empty());
		when(repository.findByGroupPoidAndPortName(anyLong(), eq("Port A"))).thenReturn(Optional.of(new PortMaster()));

		assertThrows(RuntimeException.class, () -> service.createPort(100L, request, "admin"));
	}

	@Test
	void createPort_WhenActiveIsNull_ShouldDefaultToY() {

		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P10");
		request.setPortName("Port X");
		request.setActive(null);

		when(repository.findByGroupPoidAndPortCode(anyLong(), anyString())).thenReturn(Optional.empty());
		when(repository.findByGroupPoidAndPortName(anyLong(), anyString())).thenReturn(Optional.empty());

		service.createPort(100L, request, "admin");
	}

	@Test
	void updatePort_Success() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P001");
		request.setPortName("Port A Updated");
		request.setActive("N");

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));

		service.updatePort(100L, 1L, request, "admin");
	}

	@Test
	void updatePort_PortCodeExists_Throws() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P002");
		request.setPortName("Port A");

		PortMaster conflictingPort = new PortMaster();
		conflictingPort.setPortPoid(2L);

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));
		lenient().when(repository.findByPortCode("P002")).thenReturn(Optional.of(conflictingPort));

		assertThrows(RuntimeException.class, () -> service.updatePort(100L, 1L, request, "admin"));
	}

	@Test
	void updatePort_PortNameExists_Throws() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P001");
		request.setPortName("Port B");

		PortMaster conflictingPort = new PortMaster();
		conflictingPort.setPortPoid(2L);

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));
		lenient().when(repository.findByPortName("Port B")).thenReturn(Optional.of(conflictingPort));

		assertThrows(RuntimeException.class, () -> service.updatePort(100L, 1L, request, "admin"));
	}

	@Test
	void updatePort_WhenPortCodeIsDifferentAndUnique_ShouldUpdate() {

		PortMaster entity = new PortMaster();
		entity.setPortPoid(1L);
		entity.setPortCode("OLD");
		entity.setPortName("Port A");

		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("NEW");
		request.setPortName("Port A");

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));
		when(repository.findByPortCode("NEW")).thenReturn(Optional.empty());

		service.updatePort(100L, 1L, request, "admin");
	}

	@Test
	void updatePort_PortNotFound_Throws() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P001");
		request.setPortName("Port A");

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> service.updatePort(100L, 1L, request, "admin"));
	}

	@Test
	void updatePort_WhenPortCodeSamePoid_ShouldAllowUpdate() {

		PortMaster entity = new PortMaster();
		entity.setPortPoid(1L);
		entity.setPortCode("P001");
		entity.setPortName("Port A");

		PortMaster samePoidPort = new PortMaster();
		samePoidPort.setPortPoid(1L);

		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("NEWCODE");
		request.setPortName("Port A");

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));
		when(repository.findByPortCode("NEWCODE")).thenReturn(Optional.of(samePoidPort));

		service.updatePort(100L, 1L, request, "admin");
	}

	@Test
	void updatePort_WhenPortNameDifferentAndUnique_ShouldUpdate() {

		PortMaster entity = new PortMaster();
		entity.setPortPoid(1L);
		entity.setPortCode("P001");
		entity.setPortName("OLD");

		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P001");
		request.setPortName("NEW");

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));
		when(repository.findByPortName("NEW")).thenReturn(Optional.empty());

		service.updatePort(100L, 1L, request, "admin");
	}

	@Test
	void updatePort_WhenPortNameSamePoid_ShouldAllowUpdate() {

		PortMaster entity = new PortMaster();
		entity.setPortPoid(1L);
		entity.setPortCode("P001");
		entity.setPortName("OLD");

		PortMaster samePoidPort = new PortMaster();
		samePoidPort.setPortPoid(1L);

		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P001");
		request.setPortName("NEW");

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));
		when(repository.findByPortName("NEW")).thenReturn(Optional.of(samePoidPort));

		service.updatePort(100L, 1L, request, "admin");
	}

	@Test
	void getPortById_Success() {
		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));

		service.getPortById(100L, 1L);
	}

	@Test
	void getPortById_NotFound_Throws() {
		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> service.getPortById(100L, 1L));
	}

	@Test
	void getAllPorts_Success() {
		FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());
		RawSearchResult rawResult = new RawSearchResult(List.of(Map.of("PORT_POID", 1L, "PORT_NAME", "Port A")),
				Map.of("PORT_POID", "Port ID", "PORT_NAME", "Port Name"), 1);

		when(documentService.resolveOperator(filters)).thenReturn("OR");
		when(documentService.resolveIsDeleted(filters)).thenReturn("false");
		when(documentService.resolveFilters(filters)).thenReturn(List.of());
		when(documentService.search(anyString(), anyList(), anyString(), any(PageRequest.class), anyString(),
				anyString(), anyString())).thenReturn(rawResult);

		service.getAllPorts("doc123", filters, PageRequest.of(0, 10));
	}

	@Test
	void deletePort_Success() {
		entity.setDeleted("N");

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));

		service.deletePort(100L, 1L, "admin");
	}

	@Test
	void deletePort_AlreadyDeleted_Throws() {
		entity.setDeleted("Y");

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));

		assertThrows(RuntimeException.class, () -> service.deletePort(100L, 1L, "admin"));
	}

	@Test
	void deletePort_NotFound_Throws() {
		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> service.deletePort(100L, 1L, "admin"));
	}
}
