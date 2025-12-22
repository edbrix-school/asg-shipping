package com.asg.shipping.remuneration.service;

import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.shipping.common.repository.GlMasterRepository;
import com.asg.shipping.common.repository.ShipChargeMasterRepository;
import com.asg.shipping.remuneration.dto.ShipRemunerationMasterRequestDto;
import com.asg.shipping.remuneration.dto.ShipRemunerationMasterResponseDto;
import com.asg.shipping.remuneration.entity.ShipRemunerationMaster;
import com.asg.shipping.remuneration.repository.ShipRemunerationMasterRepository;
import jakarta.xml.bind.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RemunerationMasterServiceImplTest {

    @Mock
    private ShipRemunerationMasterRepository repository;

    @Mock
    private GlMasterRepository glMasterRepository;

    @Mock
    private ShipChargeMasterRepository shipChargeMasterRepository;

    @Mock
    private DocumentSearchService documentService;

    @InjectMocks
    private RemunerationMasterServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateRemuneration_Success() throws ValidationException {
        ShipRemunerationMasterRequestDto request = new ShipRemunerationMasterRequestDto();
        request.setRemunCode("TEST001");
        request.setRemunDescription("Test Description");
        request.setRemunChargeCodePoid(1L);
        request.setGlPoid(1L);

        ShipRemunerationMaster entity = new ShipRemunerationMaster();
        entity.setRemunerationPoid(1L);
        entity.setRemunCode("TEST001");

        when(repository.existsByRemunCodeIgnoreCase(anyString())).thenReturn(false);
        when(shipChargeMasterRepository.existsByChargePoid(any(BigDecimal.class))).thenReturn(true);
        when(glMasterRepository.existsByGlPoid(anyLong())).thenReturn(true);
        when(repository.save(any())).thenReturn(entity);
        when(repository.findById(anyLong())).thenReturn(Optional.of(entity));

        ShipRemunerationMasterResponseDto result = service.createRemuneration(request);

        assertNotNull(result);
        verify(repository, times(1)).save(any());
    }

    @Test
    void testCreateRemuneration_BlankCode() {
        ShipRemunerationMasterRequestDto request = new ShipRemunerationMasterRequestDto();
        request.setRemunCode("");

        assertThrows(ValidationException.class, () -> service.createRemuneration(request));
    }

    @Test
    void testCreateRemuneration_DuplicateCode() {
        ShipRemunerationMasterRequestDto request = new ShipRemunerationMasterRequestDto();
        request.setRemunCode("TEST001");

        when(repository.existsByRemunCodeIgnoreCase(anyString())).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> service.createRemuneration(request));
    }

    @Test
    void testUpdateRemuneration_Success() {
        ShipRemunerationMasterRequestDto request = new ShipRemunerationMasterRequestDto();
        request.setRemunDescription("Updated");
        request.setRemunChargeCodePoid(1L);
        request.setGlPoid(1L);

        ShipRemunerationMaster entity = new ShipRemunerationMaster();
        entity.setRemunerationPoid(1L);

        when(repository.findById(anyLong())).thenReturn(Optional.of(entity));
        when(shipChargeMasterRepository.existsByChargePoid(any(BigDecimal.class))).thenReturn(true);
        when(glMasterRepository.existsByGlPoid(anyLong())).thenReturn(true);
        when(repository.save(any())).thenReturn(entity);

        ShipRemunerationMasterResponseDto result = service.updateRemuneration(1L, request);

        assertNotNull(result);
        verify(repository, times(1)).save(any());
    }

    @Test
    void testUpdateRemuneration_NotFound() {
        ShipRemunerationMasterRequestDto request = new ShipRemunerationMasterRequestDto();

        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateRemuneration(1L, request));
    }

    @Test
    void testGetRemunerationById_Success() {
        ShipRemunerationMaster entity = new ShipRemunerationMaster();
        entity.setRemunerationPoid(1L);

        when(repository.findById(anyLong())).thenReturn(Optional.of(entity));

        ShipRemunerationMasterResponseDto result = service.getRemunerationById(1L);

        assertNotNull(result);
        verify(repository, times(1)).findById(eq(1L));
    }

    @Test
    void testGetRemunerationById_NotFound() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getRemunerationById(1L));
    }

    @Test
    void testSoftDeleteRemuneration_Success() {
        ShipRemunerationMaster entity = new ShipRemunerationMaster();
        entity.setRemunerationPoid(1L);

        when(repository.findById(anyLong())).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        service.softDeleteRemuneration(1L);

        assertEquals("Y", entity.getDeleted());
        assertEquals("N", entity.getActive());
        verify(repository, times(1)).save(any());
    }

    @Test
    void testSoftDeleteRemuneration_NotFound() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.softDeleteRemuneration(1L));
    }
}
