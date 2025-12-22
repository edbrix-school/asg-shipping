package com.asg.shipping.tradelanemaster.dto.response;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ShipTradeLaneResponseTest {

    @Test
    void testNoArgsConstructor() {
        ShipTradelaneResponse response = new ShipTradelaneResponse();
        assertNotNull(response);
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        ShipTradelaneResponse response = new ShipTradelaneResponse(
                1L, "TL001", "Trade Lane 1", "TL1", 2L, true, 10,
                "user1", now, "user2", now
        );
        
        assertEquals(1L, response.getTradeLanePoid());
        assertEquals("TL001", response.getTradeLaneCode());
        assertEquals("Trade Lane 1", response.getTradeLaneName());
        assertEquals("TL1", response.getTradeLaneName2());
        assertEquals(2L, response.getRegionPoid());
        assertTrue(response.getActive());
        assertEquals(10, response.getSeqNo());
        assertEquals("user1", response.getCreatedBy());
        assertEquals(now, response.getCreatedDate());
        assertEquals("user2", response.getLastModifiedBy());
        assertEquals(now, response.getLastModifiedDate());
    }

    @Test
    void testBuilder() {
        LocalDateTime now = LocalDateTime.now();
        ShipTradelaneResponse response = ShipTradelaneResponse.builder()
                .tradeLanePoid(1L)
                .tradeLaneCode("TL001")
                .tradeLaneName("Trade Lane 1")
                .tradeLaneName2("TL1")
                .regionPoid(2L)
                .active(true)
                .seqNo(10)
                .createdBy("user1")
                .createdDate(now)
                .lastModifiedBy("user2")
                .lastModifiedDate(now)
                .build();
        
        assertEquals(1L, response.getTradeLanePoid());
        assertEquals("TL001", response.getTradeLaneCode());
        assertEquals("Trade Lane 1", response.getTradeLaneName());
        assertEquals("TL1", response.getTradeLaneName2());
        assertEquals(2L, response.getRegionPoid());
        assertTrue(response.getActive());
        assertEquals(10, response.getSeqNo());
        assertEquals("user1", response.getCreatedBy());
        assertEquals(now, response.getCreatedDate());
        assertEquals("user2", response.getLastModifiedBy());
        assertEquals(now, response.getLastModifiedDate());
    }

    @Test
    void testSettersAndGetters() {
        ShipTradelaneResponse response = new ShipTradelaneResponse();
        LocalDateTime now = LocalDateTime.now();
        
        response.setTradeLanePoid(1L);
        response.setTradeLaneCode("TL001");
        response.setTradeLaneName("Trade Lane 1");
        response.setTradeLaneName2("TL1");
        response.setRegionPoid(2L);
        response.setActive(true);
        response.setSeqNo(10);
        response.setCreatedBy("user1");
        response.setCreatedDate(now);
        response.setLastModifiedBy("user2");
        response.setLastModifiedDate(now);
        
        assertEquals(1L, response.getTradeLanePoid());
        assertEquals("TL001", response.getTradeLaneCode());
        assertEquals("Trade Lane 1", response.getTradeLaneName());
        assertEquals("TL1", response.getTradeLaneName2());
        assertEquals(2L, response.getRegionPoid());
        assertTrue(response.getActive());
        assertEquals(10, response.getSeqNo());
        assertEquals("user1", response.getCreatedBy());
        assertEquals(now, response.getCreatedDate());
        assertEquals("user2", response.getLastModifiedBy());
        assertEquals(now, response.getLastModifiedDate());
    }

    @Test
    void testEqualsAndHashCode() {
        ShipTradelaneResponse response1 = new ShipTradelaneResponse();
        response1.setTradeLanePoid(1L);
        response1.setTradeLaneCode("TL001");
        
        ShipTradelaneResponse response2 = new ShipTradelaneResponse();
        response2.setTradeLanePoid(1L);
        response2.setTradeLaneCode("TL001");
        
        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    void testToString() {
        ShipTradelaneResponse response = new ShipTradelaneResponse();
        response.setTradeLaneCode("TL001");
        response.setTradeLaneName("Trade Lane 1");
        
        String toString = response.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("TL001"));
        assertTrue(toString.contains("Trade Lane 1"));
    }

    @Test
    void testFalseActiveValue() {
        ShipTradelaneResponse response = new ShipTradelaneResponse();
        response.setActive(false);
        
        assertFalse(response.getActive());
    }
}