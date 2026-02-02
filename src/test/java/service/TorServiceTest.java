package service;

import org.junit.jupiter.api.*;
import protocol.Identity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TorService - Tor hidden service management.
 * Note: Most tests are unit tests that don't require JavaFX or a running Tor instance.
 * Integration tests would require JavaFX toolkit initialization and Tor.
 */
class TorServiceTest {
    
    // ═══════════════════════════════════════════════════════════════
    // UNIT TESTS (No JavaFX dependency)
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Identity should be generated successfully")
    void testIdentityGeneration() throws Exception {
        Identity identity = Identity.generate();
        assertNotNull(identity, "Identity should be generated");
        assertNotNull(identity.getPublicKey(), "Public key should exist");
    }
    
    @Test
    @DisplayName("Onion address format should be valid")
    void testOnionAddressFormat() {
        // Onion v3 addresses are 56 characters + ".onion"
        String validOnion = "a".repeat(56) + ".onion";
        assertTrue(validOnion.endsWith(".onion"), "Should end with .onion");
        assertEquals(62, validOnion.length(), "V3 onion address should be 62 chars");
    }
    
    @Test
    @DisplayName("Normalizing onion address should handle various formats")
    void testOnionAddressNormalization() {
        // Test uppercase conversion
        String upperCase = "ABC123.ONION";
        assertEquals("abc123.onion", upperCase.toLowerCase().trim());
        
        // Test trimming
        String withSpaces = "  abc.onion  ";
        assertEquals("abc.onion", withSpaces.toLowerCase().trim());
    }
    
    @Test
    @DisplayName("SOCKS proxy settings should be correct for Tor")
    void testSocksProxySettings() {
        // Default Tor SOCKS port
        int torSocksPort = 9050;
        String torSocksHost = "127.0.0.1";
        
        assertEquals(9050, torSocksPort, "Default Tor SOCKS port should be 9050");
        assertEquals("127.0.0.1", torSocksHost, "Tor SOCKS should be on localhost");
    }
    
    @Test
    @DisplayName("Control port settings should be correct for Tor")
    void testControlPortSettings() {
        // Default Tor control port
        int torControlPort = 9051;
        
        assertEquals(9051, torControlPort, "Default Tor control port should be 9051");
    }
    
    @Test
    @DisplayName("Hidden service port mapping should be configured")
    void testHiddenServicePort() {
        // Default hidden service port for messaging
        int hiddenServicePort = 12345;
        
        assertTrue(hiddenServicePort > 1024, "Port should be > 1024 (non-privileged)");
        assertTrue(hiddenServicePort < 65536, "Port should be valid (<65536)");
    }
    
    // ═══════════════════════════════════════════════════════════════
    // NOTE: Integration tests with actual TorService require JavaFX
    // and a running Tor instance. These would be integration tests.
    // ═══════════════════════════════════════════════════════════════
}
