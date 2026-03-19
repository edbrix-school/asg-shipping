package com.asg.shipping.portMaster.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
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
	private MockedStatic<UserContext> mockedUserContext;

	@BeforeEach
	void setup() {
		mockedUserContext = mockStatic(UserContext.class);
		mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

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

	@AfterEach
	void tearDown() {
		if (mockedUserContext != null) {
			mockedUserContext.close();
		}
	}

	@Test
	void createPort_Success() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P003");
		request.setPortName("Port C");
		// also test null active fallback -> "Y"
		request.setActive(null);

		PortMaster saved = new PortMaster();
		saved.setPortPoid(99L);

		when(repository.findByGroupPoidAndPortCode(100L, "P003"))
				.thenReturn(Optional.empty()) // First call in validation
				.thenReturn(Optional.of(saved)); // Second call after save

		when(repository.findByGroupPoidAndPortName(100L, "Port C")).thenReturn(Optional.empty());

		Map<String, Object> result = service.createPort(100L, request, "admin");

		assertEquals(99L, result.get("portPoid"));
		verify(repository).save(any(PortMaster.class));
		verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.CREATED), eq("DOC123"), eq("99"));
	}

	@Test
	void createPort_Success_WithActive() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P003");
		request.setPortName("Port C");
		request.setActive("N");

		PortMaster saved = new PortMaster();
		saved.setPortPoid(99L);

		when(repository.findByGroupPoidAndPortCode(100L, "P003"))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(saved));

		when(repository.findByGroupPoidAndPortName(100L, "Port C")).thenReturn(Optional.empty());

		Map<String, Object> result = service.createPort(100L, request, "admin");

		assertEquals(99L, result.get("portPoid"));
		verify(repository).save(any(PortMaster.class));
	}

	@Test
	void createPort_PortCodeExists_Throws() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P001");

		when(repository.findByGroupPoidAndPortCode(100L, "P001")).thenReturn(Optional.of(new PortMaster()));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> service.createPort(100L, request, "admin"));
		assertEquals("Port Code already exists", exception.getMessage());
	}

	@Test
	void createPort_PortNameExists_Throws() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P010");
		request.setPortName("Port A");

		when(repository.findByGroupPoidAndPortCode(100L, "P010")).thenReturn(Optional.empty());

		when(repository.findByGroupPoidAndPortName(100L, "Port A")).thenReturn(Optional.of(new PortMaster()));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> service.createPort(100L, request, "admin"));
		assertEquals("Port Name already exists", exception.getMessage());
	}

	@Test
	void createPort_NotFoundAfterSave_Throws() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P003");
		request.setPortName("Port C");

		when(repository.findByGroupPoidAndPortCode(100L, "P003"))
				.thenReturn(Optional.empty()) // Validation
				.thenReturn(Optional.empty()); // After save

		when(repository.findByGroupPoidAndPortName(100L, "Port C")).thenReturn(Optional.empty());

		RuntimeException exception = assertThrows(RuntimeException.class, () -> service.createPort(100L, request, "admin"));
		assertEquals("Port not found after save", exception.getMessage());
	}

	@Test
	void updatePort_Success_NoCodeOrNameChange() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P001");
		request.setPortName("Port A");
		request.setActive("N");

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));

		ShipTradelaneResponse tradeLane = new ShipTradelaneResponse();
		when(tradeLaneService.getById(any())).thenReturn(tradeLane);

		GlobalCountryMaster country = new GlobalCountryMaster();
		when(countryRepository.findById(any())).thenReturn(Optional.of(country));

		PortMasterResponse response = service.updatePort(100L, 1L, request, "admin");

		assertNotNull(response);
		verify(repository).save(any(PortMaster.class));
		verify(loggingService).logChanges(any(), any(), eq(PortMaster.class), eq("DOC123"), eq("1"), eq(LogDetailsEnum.MODIFIED), eq("PORT_POID"));
	}

	@Test
	void updatePort_Success_WithCodeAndNameChange() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P002");
		request.setPortName("Port B");

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));

		when(repository.findByPortCode("P002")).thenReturn(Optional.empty()); // Or return a port with same portPoid
		when(repository.findByPortName("Port B")).thenReturn(Optional.empty());

		ShipTradelaneResponse tradeLane = new ShipTradelaneResponse();
		when(tradeLaneService.getById(any())).thenReturn(tradeLane);

		GlobalCountryMaster country = new GlobalCountryMaster();
		when(countryRepository.findById(any())).thenReturn(Optional.of(country));

		PortMasterResponse response = service.updatePort(100L, 1L, request, "admin");

		assertNotNull(response);
		verify(repository).save(any(PortMaster.class));
	}

	@Test
	void updatePort_DuplicateCodeFoundButSamePoid_Success() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P002");
		request.setPortName("Port A");

		PortMaster existingSamePoid = new PortMaster();
		existingSamePoid.setPortPoid(1L); // Same ID!

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));
		when(repository.findByPortCode("P002")).thenReturn(Optional.of(existingSamePoid)); 

		ShipTradelaneResponse tradeLane = new ShipTradelaneResponse();
		when(tradeLaneService.getById(any())).thenReturn(tradeLane);

		GlobalCountryMaster country = new GlobalCountryMaster();
		when(countryRepository.findById(any())).thenReturn(Optional.of(country));

		PortMasterResponse response = service.updatePort(100L, 1L, request, "admin");

		assertNotNull(response);
		verify(repository).save(any(PortMaster.class));
	}

    @Test
	void updatePort_DuplicateNameFoundButSamePoid_Success() {
		PortMasterRequest request = new PortMasterRequest();
		request.setPortCode("P001");
		request.setPortName("Port B");

		PortMaster existingSamePoid = new PortMaster();
		existingSamePoid.setPortPoid(1L); // Same ID!

		when(repository.findById(new PortMasterId(100L, 1L))).thenReturn(Optional.of(entity));
		when(repository.findByPortName("Port B")).thenReturn(Optional.of(existingSamePoid)); 

		ShipTradelaneResponse tradeLane = new ShipTradelaneResponse();
		when(tradeLaneService.getById(any())).thenReturn(tradeLane);

		GlobalCountryMaster country = new GlobalCountryMaster();
		when(countryRepository.findById(any())).thenReturn(Optional.of(country));

		PortMasterResponse response = service.updatePort(100L, 1L, request, "admin");

		assertNotNull(response);
		verify(repository).save(any(PortMaster.class));
	}


	@Test
	void updatePort_PortNotFound_Throws() {
		when(repository.findById(any())).thenReturn(Optional.empty());

		RuntimeException exception = assertThrows(RuntimeException.class, () -> service.updatePort(100L, 1L, new PortMasterRequest(), "admin"));
		assertEquals("Port not found", exception.getMessage());
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

		RuntimeException exception = assertThrows(RuntimeException.class, () -> service.updatePort(100L, 1L, request, "admin"));
		assertEquals("PortCode already exists", exception.getMessage());
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

		RuntimeException exception = assertThrows(RuntimeException.class, () -> service.updatePort(100L, 1L, request, "admin"));
		assertEquals("PortName already exists", exception.getMessage());
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
		assertEquals(20L, response.getCountryDetail().get("poid"));
		assertEquals("TL01", response.getCountryDetail().get("code"));
		assertEquals("Asia", response.getCountryDetail().get("description"));

		assertNotNull(response.getTradelaneDetail());
		assertEquals(10L, response.getTradelaneDetail().get("poid"));
		assertEquals("IN", response.getTradelaneDetail().get("code"));
		assertEquals("India", response.getTradelaneDetail().get("description"));
	}

	@Test
	void getPortById_NotFound_Throws() {
		when(repository.findById(any())).thenReturn(Optional.empty());

		RuntimeException exception = assertThrows(RuntimeException.class, () -> service.getPortById(100L, 1L));
		assertEquals("Port not found", exception.getMessage());
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
		verify(documentService).search(eq("DOC1"), anyList(), eq("OR"), eq(pageable), eq("false"), eq("PORT_NAME"), eq("PORT_POID"));
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

		RuntimeException exception = assertThrows(RuntimeException.class, () -> service.deletePort(100L, 1L, "admin"));
		assertEquals("Port has already been deleted.", exception.getMessage());
	}

	@Test
	void deletePort_NotFound_Throws() {
		when(repository.findById(any())).thenReturn(Optional.empty());

		RuntimeException exception = assertThrows(RuntimeException.class, () -> service.deletePort(100L, 1L, "admin"));
		assertEquals("Port not found", exception.getMessage());
	}
}
