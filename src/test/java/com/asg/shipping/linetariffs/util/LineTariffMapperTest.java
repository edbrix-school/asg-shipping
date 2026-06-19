package com.asg.shipping.linetariffs.util;

import com.asg.shipping.linetariffs.dto.LineTariffUpdateDTO;
import com.asg.shipping.linetariffs.entity.ShipLineTariffHdr;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineTariffMapperTest {

    private final LineTariffMapper mapper = new LineTariffMapper();

    @Test
    void mapUpdateDTOToEntity_PreservesDocRefWhenUpdatePayloadIsBlank() {
        ShipLineTariffHdr entity = new ShipLineTariffHdr();
        entity.setDocRef("ASG162");

        LineTariffUpdateDTO dto = LineTariffUpdateDTO.builder()
                .description("Updated description")
                .docRef("")
                .build();

        mapper.mapUpdateDTOToEntity(dto, entity);

        assertEquals("ASG162", entity.getDocRef());
        assertEquals("Updated description", entity.getDescription());
    }

    @Test
    void mapUpdateDTOToEntity_UpdatesTransactionDate() {
        ShipLineTariffHdr entity = new ShipLineTariffHdr();
        entity.setTransactionDate(LocalDate.of(2026, 1, 1));

        LineTariffUpdateDTO dto = LineTariffUpdateDTO.builder()
                .transactionDate(LocalDate.of(2026, 3, 15))
                .build();

        mapper.mapUpdateDTOToEntity(dto, entity);

        assertEquals(LocalDate.of(2026, 3, 15), entity.getTransactionDate());
    }

    @Test
    void mapUpdateDTOToEntity_PreservesTransactionDateWhenNotProvided() {
        ShipLineTariffHdr entity = new ShipLineTariffHdr();
        entity.setTransactionDate(LocalDate.of(2026, 1, 1));

        LineTariffUpdateDTO dto = LineTariffUpdateDTO.builder()
                .description("Updated")
                .build();

        mapper.mapUpdateDTOToEntity(dto, entity);

        assertEquals(LocalDate.of(2026, 1, 1), entity.getTransactionDate());
    }
}
