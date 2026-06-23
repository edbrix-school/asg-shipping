package com.asg.shipping.linetariffs.repository;

import com.asg.common.lib.dto.FilterDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LineTariffListRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private LineTariffListRepository repository;

    @BeforeEach
    void setUp() {
        repository = new LineTariffListRepository(jdbcTemplate, "PRODUCTION");
    }

    @Test
    void search_GlobalSearch_CorrelatesLineMasterToTariffRow() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        repository.search(
                1L,
                1L,
                null,
                null,
                "N",
                List.of(new FilterDto("GLOBALSEARCH", "MAERSK")),
                PageRequest.of(0, 20));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), eq(Long.class), any(Object[].class));

        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("FROM PRODUCTION.SHIP_LINE_TARIFF_HDR t "));
        assertTrue(sql.contains("lm.LINE_POID = t.LINE_POID"));
        assertTrue(sql.contains("UPPER(lm.LINE_NAME) LIKE ?"));
        assertTrue(sql.contains("UPPER(lm.LINE_CODE) LIKE ?"));
    }
}
