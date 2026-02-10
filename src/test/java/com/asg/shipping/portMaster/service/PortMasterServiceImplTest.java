package com.asg.shipping.portMaster.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.data.domain.Pageable;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.common.entity.GlobalCountryMaster;
import com.asg.shipping.common.repository.GlobalCountryMasterRepository;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.portMaster.dto.PortMasterRequest;
import com.asg.shipping.portMaster.dto.PortMasterResponse;
import com.asg.shipping.portMaster.entity.PortMaster;
import com.asg.shipping.portMaster.entity.PortMasterId;
import com.asg.shipping.portMaster.repository.PortMasterRepository;
import com.asg.shipping.tradelanemaster.dto.response.ShipTradelaneResponse;
import com.asg.shipping.tradelanemaster.service.ShipTradeLaneService;

@ExtendWith(MockitoExtension.class)
class PortMasterServiceImplTest {

	@Mock
	private PortMasterRepository repository;

	@Mock
	private GlobalCountryMasterRepository countryRepository;

	@Mock
	private DocumentSearchService documentService;

	@Mock
	private ShipTradeLaneService tradeLaneService;

	@Mock
	private LoggingService loggingService;

	@InjectMocks
	private PortMasterServiceImpl service;

	private PortMaster entity;

	@BeforeEach
	void setup() {
		entity = new PortMaster();
		entity.setGroupPoid(100L);
		entity.setPortPoid(1L);
		entity.setPortCode("P001");
		entity.setPortName("Port A");
		entity.setCountryPoid(10L);
		entity.setTradelanePoid(20L);
		entity.setActive("Y");
		entity.setDeleted("N");
		entity.setCreatedDate(LocalDateTime.now());
	}

	@Test
	void createPort_Success() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P003");
		request.setPortName("Port C");

		PortMaster saved = new PortMaster();
		saved.setPortPoid(99L);

		when(repository.findByGroupPoidAndPortCode(100L, "P003")).thenReturn(Optional.empty())
				.thenReturn(Optional.of(saved));

		when(repository.findByGroupPoidAndPortName(100L, "Port C")).thenReturn(Optional.empty());

		Map<String, Object> result = service.createPort(100L, request, "admin");

		assertEquals(99L, result.get("portPoid"));
	}

	@Test
	void createPort_PortCodeExists_Throws() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P001");

		when(repository.findByGroupPoidAndPortCode(100L, "P001")).thenReturn(Optional.of(new PortMaster()));

		assertThrows(RuntimeException.class, () -> service.createPort(100L, request, "admin"));
	}

	@Test
	void createPort_PortNameExists_Throws() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P010");
		request.setPortName("Port A");

		when(repository.findByGroupPoidAndPortCode(100L, "P010")).thenReturn(Optional.empty());

		when(repository.findByGroupPoidAndPortName(100L, "Port A")).thenReturn(Optional.of(new PortMaster()));

		assertThrows(RuntimeException.class, () -> service.createPort(100L, request, "admin"));
	}

	@Test
	void updatePort_Success() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P001");
		request.setPortName("Port Updated");

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));

		when(tradeLaneService.getById(any())).thenReturn(new ShipTradelaneResponse());

		when(countryRepository.findById(any())).thenReturn(Optional.of(new GlobalCountryMaster()));

		PortMasterResponse response = service.updatePort(100L, 1L, request, "admin");

		assertNotNull(response);
	}

	@Test
	void updatePort_PortNotFound_Throws() {
		when(repository.findById(any())).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> service.updatePort(100L, 1L, new PortMasterRequest(), "admin"));
	}

	@Test
	void updatePort_DuplicatePortCode_Throws() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("NEW_CODE");
		request.setPortName("Port A");

		PortMaster existing = new PortMaster();
		existing.setPortPoid(99L);
		existing.setPortCode("NEW_CODE");

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));

		when(repository.findByPortCode("NEW_CODE")).thenReturn(Optional.of(existing));

		assertThrows(RuntimeException.class, () -> service.updatePort(100L, 1L, request, "admin"));
	}

	@Test
	void updatePort_DuplicatePortName_Throws() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P001");
		request.setPortName("NEW_NAME");

		PortMaster existing = new PortMaster();
		existing.setPortPoid(99L);
		existing.setPortName("NEW_NAME");

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));

		when(repository.findByPortName("NEW_NAME")).thenReturn(Optional.of(existing));

		assertThrows(RuntimeException.class, () -> service.updatePort(100L, 1L, request, "admin"));
	}

	@Test
	void getPortById_Success() {
		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));

		ShipTradelaneResponse tradeLane = new ShipTradelaneResponse();
		tradeLane.setTradeLanePoid(20L);
		tradeLane.setTradeLaneCode("TL01");
		tradeLane.setTradeLaneName("Asia");

		GlobalCountryMaster country = new GlobalCountryMaster();
		country.setCountryPoid(10L);
		country.setCountryCode("IN");
		country.setCountryName("India");

		when(tradeLaneService.getById(any())).thenReturn(tradeLane);
		when(countryRepository.findById(any())).thenReturn(Optional.of(country));

		PortMasterResponse response = service.getPortById(100L, 1L);

		assertNotNull(response);
		assertNotNull(response.getCountryDetail());
		assertNotNull(response.getTradelaneDetail());
	}

	@Test
	void getPortById_NotFound_Throws() {
		when(repository.findById(any())).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> service.getPortById(100L, 1L));
	}

	@Test
	void getPortById_CountryNotFound_Throws() {
		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));

		when(tradeLaneService.getById(any())).thenReturn(new ShipTradelaneResponse());

		when(countryRepository.findById(any())).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.getPortById(100L, 1L));
	}

	@Test
	void getAllPorts_Success() {
		FilterRequestDto filters = new FilterRequestDto("OR", "false", List.of());
		Pageable pageable = PageRequest.of(0, 10);

		RawSearchResult raw = new RawSearchResult(List.of(Map.of("PORT_POID", 1L, "PORT_NAME", "Port A")),
				Map.of("PORT_POID", "Port ID", "PORT_NAME", "Port Name"), 1L);

		when(documentService.resolveOperator(filters)).thenReturn("OR");
		when(documentService.resolveIsDeleted(filters)).thenReturn("false");
		when(documentService.resolveFilters(filters)).thenReturn(List.of());
		when(documentService.search(anyString(), anyList(), anyString(), eq(pageable), anyString(), anyString(),
				anyString())).thenReturn(raw);

		Map<String, Object> result = service.getAllPorts("DOC1", filters, pageable);

		assertNotNull(result);
	}

	@Test
	void deletePort_Success() {
		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));

		service.deletePort(100L, 1L, "admin");

		assertEquals("Y", entity.getDeleted());
	}

	@Test
	void deletePort_AlreadyDeleted_Throws() {
		entity.setDeleted("Y");

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));

		assertThrows(RuntimeException.class, () -> service.deletePort(100L, 1L, "admin"));
	}

	@Test
	void deletePort_NotFound_Throws() {
		when(repository.findById(any())).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> service.deletePort(100L, 1L, "admin"));
	}
}
