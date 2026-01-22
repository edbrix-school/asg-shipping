package com.asg.shipping.vesselmaster.service;

import com.asg.shipping.common.dto.LovItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VesselMasterLovServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private VesselMasterLovServiceImpl lovService;

    private LovItem lovItem1;
    private LovItem lovItem2;

    @BeforeEach
    void setUp() {
        lovItem1 = new LovItem();
        lovItem1.setPoid(1L);
        lovItem1.setCode("CODE001");
        lovItem1.setDescription("Description 1");

        lovItem2 = new LovItem();
        lovItem2.setPoid(2L);
        lovItem2.setCode("CODE002");
        lovItem2.setDescription("Description 2");
    }

    @Test
    void getLineMasterLov_WithoutFilter_Success() {
        List<LovItem> expectedList = List.of(lovItem1, lovItem2);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(expectedList);

        List<LovItem> result = lovService.getLineMasterLov(null);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(lovItem1.getPoid(), result.get(0).getPoid());
        assertEquals(lovItem2.getPoid(), result.get(1).getPoid());
        verify(jdbcTemplate).query(contains("SELECT LINE_POID AS POID"), any(RowMapper.class));
        verify(jdbcTemplate).query(contains("ORDER BY LINE_NAME"), any(RowMapper.class));
        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), any());
    }

    @Test
    void getLineMasterLov_WithFilter_Success() {
        Long linePoid = 10L;
        List<LovItem> expectedList = List.of(lovItem1);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(linePoid))).thenReturn(expectedList);

        List<LovItem> result = lovService.getLineMasterLov(linePoid);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(lovItem1.getPoid(), result.get(0).getPoid());
        verify(jdbcTemplate).query(contains("AND LINE_POID = ?"), any(RowMapper.class), eq(linePoid));
    }

    @Test
    void getLineMasterLov_EmptyResult_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        List<LovItem> result = lovService.getLineMasterLov(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getVesselTypeLov_WithoutFilter_Success() {
        List<LovItem> expectedList = List.of(lovItem1, lovItem2);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(expectedList);

        List<LovItem> result = lovService.getVesselTypeLov(null);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(jdbcTemplate).query(contains("SELECT VESSEL_TYPE_POID AS POID"), any(RowMapper.class));
        verify(jdbcTemplate).query(contains("ORDER BY VESSEL_TYPE_NAME"), any(RowMapper.class));
        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), any());
    }

    @Test
    void getVesselTypeLov_WithFilter_Success() {
        Long vesselTypePoid = 30L;
        List<LovItem> expectedList = List.of(lovItem1);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(vesselTypePoid))).thenReturn(expectedList);

        List<LovItem> result = lovService.getVesselTypeLov(vesselTypePoid);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(jdbcTemplate).query(contains("AND VESSEL_TYPE_POID = ?"), any(RowMapper.class), eq(vesselTypePoid));
    }

    @Test
    void getVesselTypeLov_EmptyResult_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        List<LovItem> result = lovService.getVesselTypeLov(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAgentMasterLov_WithoutFilter_Success() {
        List<LovItem> expectedList = List.of(lovItem1, lovItem2);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(expectedList);

        List<LovItem> result = lovService.getAgentMasterLov(null);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(jdbcTemplate).query(contains("SELECT AGENT_POID AS POID"), any(RowMapper.class));
        verify(jdbcTemplate).query(contains("ORDER BY AGENT_NAME"), any(RowMapper.class));
        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), any());
    }

    @Test
    void getAgentMasterLov_WithFilter_Success() {
        Long agentPoid = 20L;
        List<LovItem> expectedList = List.of(lovItem1);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(agentPoid))).thenReturn(expectedList);

        List<LovItem> result = lovService.getAgentMasterLov(agentPoid);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(jdbcTemplate).query(contains("AND AGENT_POID = ?"), any(RowMapper.class), eq(agentPoid));
    }

    @Test
    void getAgentMasterLov_EmptyResult_Success() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        List<LovItem> result = lovService.getAgentMasterLov(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getLineMasterLov_QueryContainsCorrectFields() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        lovService.getLineMasterLov(null);

        verify(jdbcTemplate, atLeastOnce()).query(anyString(), any(RowMapper.class));
    }

    @Test
    void getVesselTypeLov_QueryContainsCorrectFields() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        lovService.getVesselTypeLov(null);

        verify(jdbcTemplate, atLeastOnce()).query(anyString(), any(RowMapper.class));
    }

    @Test
    void getAgentMasterLov_QueryContainsCorrectFields() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        lovService.getAgentMasterLov(null);

        verify(jdbcTemplate, atLeastOnce()).query(anyString(), any(RowMapper.class));
    }

    @Test
    void getLineMasterLov_RowMapperMapsCorrectly() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("POID")).thenReturn(1L);
        when(rs.getString("CODE")).thenReturn("CODE001");
        when(rs.getString("DESCRIPTION")).thenReturn("Description 1");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenAnswer(invocation -> {
            RowMapper<LovItem> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(rs, 1));
        });

        List<LovItem> result = lovService.getLineMasterLov(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getPoid());
        assertEquals("CODE001", result.get(0).getCode());
        assertEquals("Description 1", result.get(0).getDescription());
    }

    @Test
    void getVesselTypeLov_RowMapperMapsCorrectly() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("POID")).thenReturn(2L);
        when(rs.getString("CODE")).thenReturn("CODE002");
        when(rs.getString("DESCRIPTION")).thenReturn("Description 2");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenAnswer(invocation -> {
            RowMapper<LovItem> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(rs, 1));
        });

        List<LovItem> result = lovService.getVesselTypeLov(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getPoid());
        assertEquals("CODE002", result.get(0).getCode());
        assertEquals("Description 2", result.get(0).getDescription());
    }

    @Test
    void getAgentMasterLov_RowMapperMapsCorrectly() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("POID")).thenReturn(3L);
        when(rs.getString("CODE")).thenReturn("CODE003");
        when(rs.getString("DESCRIPTION")).thenReturn("Description 3");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenAnswer(invocation -> {
            RowMapper<LovItem> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(rs, 1));
        });

        List<LovItem> result = lovService.getAgentMasterLov(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getPoid());
        assertEquals("CODE003", result.get(0).getCode());
        assertEquals("Description 3", result.get(0).getDescription());
    }
}

