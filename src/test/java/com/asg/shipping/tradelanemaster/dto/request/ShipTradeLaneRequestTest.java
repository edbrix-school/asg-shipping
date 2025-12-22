package com.asg.shipping.tradelanemaster.dto.request;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShipTradeLaneRequestTest {

    @Test
    void testNoArgsConstructor() {
        ShipTradelaneRequest request = new ShipTradelaneRequest();
        assertNotNull(request);
    }

    @Test
    void testAllArgsConstructor() {
        ShipTradelaneRequest request = new ShipTradelaneRequest(
                "TL001", "Trade Lane 1", "TL1", 1L, true, 10
        );
        
        assertEquals("TL001", request.getTradeLaneCode());
        assertEquals("Trade Lane 1", request.getTradeLaneName());
        assertEquals("TL1", request.getTradeLaneName2());
        assertEquals(1L, request.getRegionPoid());
        assertTrue(request.getActive());
        assertEquals(10, request.getSeqNo());
    }

    @Test
    void testSettersAndGetters() {
        ShipTradelaneRequest request = new ShipTradelaneRequest();
        
        request.setTradeLaneCode("TL001");
        request.setTradeLaneName("Trade Lane 1");
        request.setTradeLaneName2("TL1");
        request.setRegionPoid(1L);
        request.setActive(true);
        request.setSeqNo(10);
        
        assertEquals("TL001", request.getTradeLaneCode());
        assertEquals("Trade Lane 1", request.getTradeLaneName());
        assertEquals("TL1", request.getTradeLaneName2());
        assertEquals(1L, request.getRegionPoid());
        assertTrue(request.getActive());
        assertEquals(10, request.getSeqNo());
    }

    @Test
    void testEqualsAndHashCode() {
        ShipTradelaneRequest request1 = new ShipTradelaneRequest();
        request1.setTradeLaneCode("TL001");
        request1.setTradeLaneName("Trade Lane 1");
        
        ShipTradelaneRequest request2 = new ShipTradelaneRequest();
        request2.setTradeLaneCode("TL001");
        request2.setTradeLaneName("Trade Lane 1");
        
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testToString() {
        ShipTradelaneRequest request = new ShipTradelaneRequest();
        request.setTradeLaneCode("TL001");
        request.setTradeLaneName("Trade Lane 1");
        
        String toString = request.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("TL001"));
        assertTrue(toString.contains("Trade Lane 1"));
    }

    @Test
    void testNullValues() {
        ShipTradelaneRequest request = new ShipTradelaneRequest();
        
        assertNull(request.getTradeLaneCode());
        assertNull(request.getTradeLaneName());
        assertNull(request.getTradeLaneName2());
        assertNull(request.getRegionPoid());
        assertNull(request.getActive());
        assertNull(request.getSeqNo());
    }

    @Test
    void testFalseActiveValue() {
        ShipTradelaneRequest request = new ShipTradelaneRequest();
        request.setActive(false);
        
        assertFalse(request.getActive());
    }
}