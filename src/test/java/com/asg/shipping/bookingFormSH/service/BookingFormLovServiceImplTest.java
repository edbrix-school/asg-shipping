package com.asg.shipping.bookingFormSH.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.asg.shipping.common.dto.LovItem;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingFormLovServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private BookingFormLovServiceImpl service;

    private LovItem sampleLovItem;

    @BeforeEach
    void setUp() {
        service = new BookingFormLovServiceImpl(jdbcTemplate);

        sampleLovItem = new LovItem();
        sampleLovItem.setPoid(1L);
        sampleLovItem.setCode("CODE1");
        sampleLovItem.setDescription("Desc1");
        sampleLovItem.setValue(1L);
        sampleLovItem.setLabel("CODE1");
    }

    // ============================================================
    // getLineMasterLov
    // ============================================================

    @Test
    void getLineMasterLov_withNullPoid_callsQueryWithoutParams() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(sampleLovItem));

        List<LovItem> result = service.getLineMasterLov(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class));
        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    void getLineMasterLov_withNonNullPoid_callsQueryWithParams() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(sampleLovItem));

        List<LovItem> result = service.getLineMasterLov(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    // ============================================================
    // getVesselMasterLov
    // ============================================================

    @Test
    void getVesselMasterLov_withNullPoid_callsQueryWithoutParams() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(sampleLovItem));

        List<LovItem> result = service.getVesselMasterLov(null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getVesselMasterLov_withNonNullPoid_callsQueryWithParams() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(sampleLovItem));

        List<LovItem> result = service.getVesselMasterLov(20L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ============================================================
    // getQuotaionLov
    // ============================================================

    @Test
    void getQuotaionLov_withNullPoid_callsQueryWithoutParams() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(sampleLovItem));

        List<LovItem> result = service.getQuotaionLov(null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getQuotaionLov_withNonNullPoid_callsQueryWithParams() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(sampleLovItem));

        List<LovItem> result = service.getQuotaionLov(30L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ============================================================
    // getSalesmanLov
    // ============================================================

    @Test
    void getSalesmanLov_withNullPoid_callsQueryWithoutParams() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(sampleLovItem));

        List<LovItem> result = service.getSalesmanLov(null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getSalesmanLov_withNonNullPoid_callsQueryWithParams() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(sampleLovItem));

        List<LovItem> result = service.getSalesmanLov(40L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ============================================================
    // getCommodityMasterLov
    // ============================================================

    @Test
    void getCommodityMasterLov_withNullPoid_callsQueryWithoutParams() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(sampleLovItem));

        List<LovItem> result = service.getCommodityMasterLov(null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getCommodityMasterLov_withNonNullPoid_callsQueryWithParams() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(sampleLovItem));

        List<LovItem> result = service.getCommodityMasterLov(50L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ============================================================
    // getPortMasterLov
    // ============================================================

    @Test
    void getPortMasterLov_withNullPoid_callsQueryWithoutParams() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(sampleLovItem));

        List<LovItem> result = service.getPortMasterLov(null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getPortMasterLov_withNonNullPoid_callsQueryWithParams() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(sampleLovItem));

        List<LovItem> result = service.getPortMasterLov(60L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ============================================================
    // getVoyageMasterLov
    // ============================================================

    @Test
    void getVoyageMasterLov_withNullPoid_callsQueryWithoutParams() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(sampleLovItem));

        List<LovItem> result = service.getVoyageMasterLov(null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getVoyageMasterLov_withNonNullPoid_callsQueryWithParams() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(sampleLovItem));

        List<LovItem> result = service.getVoyageMasterLov(70L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ============================================================
    // getChargeMasterLov
    // ============================================================

    @Test
    void getChargeMasterLov_withNullPoid_callsQueryWithoutParams() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(sampleLovItem));

        List<LovItem> result = service.getChargeMasterLov(null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getChargeMasterLov_withNonNullPoid_callsQueryWithParams() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(sampleLovItem));

        List<LovItem> result = service.getChargeMasterLov(80L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ============================================================
    // Empty result cases
    // ============================================================

    @Test
    void getLineMasterLov_returnsEmptyList_whenNoneFound() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        List<LovItem> result = service.getLineMasterLov(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getVesselMasterLov_returnsEmptyList_whenNoneFound() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        List<LovItem> result = service.getVesselMasterLov(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
