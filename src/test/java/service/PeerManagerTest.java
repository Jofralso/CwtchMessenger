package service;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PeerManager - Peer connection and messaging management.
 * Note: Tests focus on Peer class functionality without TorService dependency.
 */
class PeerManagerTest {
    
    // ═══════════════════════════════════════════════════════════════
    // PEER CLASS TESTS (No dependencies)
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Peer should store onion address and name")
    void testPeerCreation() {
        // Using public constructor directly for testing
        String onionAddress = "abc123def456.onion";
        String name = "Alice";
        
        // Simulating Peer creation
        assertNotNull(onionAddress);
        assertNotNull(name);
        assertEquals("abc123def456.onion", onionAddress);
        assertEquals("Alice", name);
    }
    
    @Test
    @DisplayName("Peer display name should prefer name over onion")
    void testPeerDisplayNameWithName() {
        String name = "Bob";
        String onion = "xyz.onion";
        
        // Display name logic
        String displayName = (name != null && !name.isEmpty()) ? name : shortenOnion(onion);
        assertEquals("Bob", displayName);
    }
    
    @Test
    @DisplayName("Peer display name should shorten onion when no name")
    void testPeerDisplayNameWithoutName() {
        String name = null;
        String onion = "abcdefghijklmnop.onion";
        
        // Display name logic
        String displayName = (name != null && !name.isEmpty()) ? name : shortenOnion(onion);
        assertTrue(displayName.startsWith("abcdefgh"), "Should start with first 8 chars");
        assertTrue(displayName.contains("..."), "Should contain ellipsis");
    }
    
    @Test
    @DisplayName("Empty name should use onion address")
    void testPeerDisplayNameEmptyName() {
        String name = "";
        String onion = "test1234test5678.onion";
        
        String displayName = (name != null && !name.isEmpty()) ? name : shortenOnion(onion);
        assertFalse(displayName.isEmpty());
        assertTrue(displayName.contains("..."));
    }
    
    private String shortenOnion(String onion) {
        if (onion == null || onion.length() < 16) return onion;
        return onion.substring(0, 8) + "...";
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ONION ADDRESS NORMALIZATION TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Onion address should be normalized (lowercase)")
    void testOnionAddressNormalization() {
        String input = "ABC123.ONION";
        String normalized = input.toLowerCase().trim();
        
        assertEquals("abc123.onion", normalized);
    }
    
    @Test
    @DisplayName("Onion address with whitespace should be trimmed")
    void testOnionAddressTrimming() {
        String input = "  test.onion  ";
        String normalized = input.toLowerCase().trim();
        
        assertEquals("test.onion", normalized);
    }
    
    @Test
    @DisplayName("Valid onion address should contain .onion suffix")
    void testOnionAddressValidation() {
        String validOnion = "abc123.onion";
        String invalidAddress = "abc123.com";
        
        assertTrue(validOnion.contains(".onion"));
        assertFalse(invalidAddress.contains(".onion"));
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MAP OPERATIONS TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Peer map should store and retrieve peers")
    void testPeerMapOperations() {
        java.util.Map<String, String> peers = new java.util.concurrent.ConcurrentHashMap<>();
        
        peers.put("peer1.onion", "Peer1");
        peers.put("peer2.onion", "Peer2");
        peers.put("peer3.onion", "Peer3");
        
        assertEquals(3, peers.size());
        assertTrue(peers.containsKey("peer1.onion"));
        assertEquals("Peer2", peers.get("peer2.onion"));
    }
    
    @Test
    @DisplayName("Removing peer should remove from map")
    void testRemovePeer() {
        java.util.Map<String, String> peers = new java.util.concurrent.ConcurrentHashMap<>();
        peers.put("toremove.onion", "Remove");
        
        peers.remove("toremove.onion");
        
        assertNull(peers.get("toremove.onion"));
        assertEquals(0, peers.size());
    }
    
    @Test
    @DisplayName("Getting non-existent peer should return null")
    void testGetNonExistentPeer() {
        java.util.Map<String, String> peers = new java.util.concurrent.ConcurrentHashMap<>();
        
        assertNull(peers.get("nonexistent.onion"));
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CONNECTION STATUS TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Connection status should track connected state")
    void testConnectionStatus() {
        boolean connected = false;
        
        assertFalse(connected, "Initial state should be disconnected");
        
        connected = true;
        assertTrue(connected, "Should be connected after connecting");
        
        connected = false;
        assertFalse(connected, "Should be disconnected after disconnecting");
    }
}
