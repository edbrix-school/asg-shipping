package com.asg.shipping.tradelanemaster.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ShipTradelaneMasterTest {

    @Test
    void testNoArgsConstructor() {
        ShipTradelaneMaster entity = new ShipTradelaneMaster();
        assertNotNull(entity);
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        ShipTradelaneMaster entity = new ShipTradelaneMaster(
                1L, 2L, "TL001", "Trade Lane 1", "TL1", "Y", 1L, "N", 3L
        );
        
        assertEquals(1L, entity.getTradeLanePoid());
        assertEquals(2L, entity.getGroupPoid());
        assertEquals("TL001", entity.getTradeLaneCode());
        assertEquals("Trade Lane 1", entity.getTradeLaneName());
        assertEquals("TL1", entity.getTradeLaneName2());
        assertEquals("Y", entity.getActive());
        assertEquals(1L, entity.getSeqNo());
        assertEquals("N", entity.getDeleted());
        assertEquals(3L, entity.getRegionPoid());
    }

    @Test
    void testSettersAndGetters() {
        ShipTradelaneMaster entity = new ShipTradelaneMaster();
        LocalDateTime now = LocalDateTime.now();
        
        entity.setTradeLanePoid(1L);
        entity.setGroupPoid(2L);
        entity.setTradeLaneCode("TL001");
        entity.setTradeLaneName("Trade Lane 1");
        entity.setTradeLaneName2("TL1");
        entity.setActive("Y");
        entity.setSeqNo(1L);
        entity.setCreatedBy("user1");
        entity.setCreatedDate(now);
        entity.setLastModifiedBy("user2");
        entity.setLastModifiedDate(now);
        entity.setDeleted("N");
        entity.setRegionPoid(3L);
        
        assertEquals(1L, entity.getTradeLanePoid());
        assertEquals(2L, entity.getGroupPoid());
        assertEquals("TL001", entity.getTradeLaneCode());
        assertEquals("Trade Lane 1", entity.getTradeLaneName());
        assertEquals("TL1", entity.getTradeLaneName2());
        assertEquals("Y", entity.getActive());
        assertEquals(1L, entity.getSeqNo());
        assertEquals("user1", entity.getCreatedBy());
        assertEquals(now, entity.getCreatedDate());
        assertEquals("user2", entity.getLastModifiedBy());
        assertEquals(now, entity.getLastModifiedDate());
        assertEquals("N", entity.getDeleted());
        assertEquals(3L, entity.getRegionPoid());
    }

    @Test
    void testEqualsAndHashCode() {
        ShipTradelaneMaster entity1 = new ShipTradelaneMaster();
        entity1.setTradeLanePoid(1L);
        entity1.setTradeLaneCode("TL001");
        
        ShipTradelaneMaster entity2 = new ShipTradelaneMaster();
        entity2.setTradeLanePoid(1L);
        entity2.setTradeLaneCode("TL001");
        
        assertEquals(entity1, entity2);
        assertEquals(entity1.hashCode(), entity2.hashCode());
    }

    @Test
    void testToString() {
        ShipTradelaneMaster entity = new ShipTradelaneMaster();
        entity.setTradeLaneCode("TL001");
        entity.setTradeLaneName("Trade Lane 1");
        
        String toString = entity.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("TL001"));
        assertTrue(toString.contains("Trade Lane 1"));
    }
}