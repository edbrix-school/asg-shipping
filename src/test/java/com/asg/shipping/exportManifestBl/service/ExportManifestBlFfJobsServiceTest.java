package com.asg.shipping.exportManifestBl.service;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.shipping.exportManifestBl.dto.ShipBlToFfDto;
import com.asg.shipping.exportManifestBl.entity.ExportManifestBlHdr;
import com.asg.shipping.exportManifestBl.repository.*;
import com.asg.shipping.exportManifestBl.util.ExportManifestBlMapper;
import com.asg.shipping.common.service.LovService;
import com.asg.shipping.exportManifestUpdate.service.ExportManifestBlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportManifestBlFfJobsServiceTest {

    @Mock private ExportManifestBlHdrRepository repository;
    @Mock private ExportManifestBlGeneralDtlRepository generalDtlRepository;
    @Mock private ExportManifestBlCargoDtlRepository cargoDtlRepository;
    @Mock private ExportManifestBlContainerDtlRepository containerDtlRepository;
    @Mock private ExportManifestBlChargesDtlRepository chargesDtlRepository;
    @Mock private ExportManifestBlService manifestUpdateService;
    @Mock private DocumentSearchService documentService;
    @Mock private ExportManifestBlMapper mapper;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private ShipBlToFfRepository shipBlToFfRepository;
    @Mock private PrintService printService;
    @Mock private DataSource dataSource;
    @Mock private LoggingService loggingService;
    @Mock private LovService lovService;

    @InjectMocks
    private ExportManifestBlServiceImpl service;

    @Test
    void getShipBlToFfByManifestPoid_SetsDeleteAllowedWhenFfPjExists() {
        ExportManifestBlHdr hdr = new ExportManifestBlHdr();
        hdr.setTransactionPoid(268427L);
        hdr.setCompanyPoid(3L);
        hdr.setDeleted("N");

        ShipBlToFfDto withPj = ShipBlToFfDto.builder().rnumid(43L).ffPj("ASG61563").build();
        ShipBlToFfDto withoutPj = ShipBlToFfDto.builder().rnumid(51L).ffPj(null).build();

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCompanyPoid).thenReturn(3L);
            when(repository.findByTransactionPoid(268427L)).thenReturn(Optional.of(hdr));
            when(shipBlToFfRepository.findAllByManifestPoid(268427L, 3L)).thenReturn(List.of(withPj, withoutPj));

            List<ShipBlToFfDto> result = service.getShipBlToFfByManifestPoid(268427L);

            assertTrue(result.get(0).getDeleteAllowed());
            assertFalse(result.get(1).getDeleteAllowed());
        }
    }

    @Test
    void deleteFfPurchaseJournal_ThrowsWhenNoFfPj() {
        ExportManifestBlHdr hdr = new ExportManifestBlHdr();
        hdr.setTransactionPoid(268427L);
        hdr.setCompanyPoid(3L);
        hdr.setDeleted("N");

        ShipBlToFfDto row = ShipBlToFfDto.builder().rnumid(51L).masterBlNo("MSCUBH236180").build();

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCompanyPoid).thenReturn(3L);
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            userContext.when(UserContext::getUserPoid).thenReturn(3483L);
            when(repository.findByTransactionPoid(268427L)).thenReturn(Optional.of(hdr));
            when(shipBlToFfRepository.findByRnumidAndManifestPoid(51L, 268427L, 3L)).thenReturn(Optional.of(row));

            assertThrows(ValidationException.class, () -> service.deleteFfPurchaseJournal(268427L, 51L));
        }
    }

    @Test
    void deleteFfPurchaseJournal_ThrowsWhenRowNotFound() {
        ExportManifestBlHdr hdr = new ExportManifestBlHdr();
        hdr.setTransactionPoid(268427L);
        hdr.setCompanyPoid(3L);
        hdr.setDeleted("N");

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCompanyPoid).thenReturn(3L);
            userContext.when(UserContext::getGroupPoid).thenReturn(1L);
            when(repository.findByTransactionPoid(268427L)).thenReturn(Optional.of(hdr));
            when(shipBlToFfRepository.findByRnumidAndManifestPoid(99L, 268427L, 3L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.deleteFfPurchaseJournal(268427L, 99L));
        }
    }
}
