package service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for PrivacyGuard - Advanced privacy features.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PrivacyGuardTest {
    
    @TempDir
    Path tempDir;
    
    private PrivacyGuard privacyGuard;
    
    @BeforeEach
    void setUp() {
        privacyGuard = new PrivacyGuard(tempDir);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MESSAGE PADDING TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @Order(1)
    @DisplayName("Message padding should pad to 256-byte blocks")
    void testMessagePadding() {
        privacyGuard.setMessagePadding(true);
        
        String shortMessage = "Hello";
        String paddedMessage = privacyGuard.padMessage(shortMessage);
        
        // Padded message should be Base64 encoded
        assertNotEquals(shortMessage, paddedMessage);
        assertTrue(paddedMessage.length() > shortMessage.length());
        
        // Decoded should be exactly 256 bytes (one block)
        byte[] decoded = java.util.Base64.getDecoder().decode(paddedMessage);
        assertEquals(256, decoded.length, "Padded message should be 256 bytes");
    }
    
    @Test
    @Order(2)
    @DisplayName("Message unpadding should restore original message")
    void testMessageUnpadding() {
        privacyGuard.setMessagePadding(true);
        
        String originalMessage = "This is a secret message!";
        String paddedMessage = privacyGuard.padMessage(originalMessage);
        String unpaddedMessage = privacyGuard.unpadMessage(paddedMessage);
        
        assertEquals(originalMessage, unpaddedMessage, "Unpadded message should match original");
    }
    
    @Test
    @Order(3)
    @DisplayName("Long messages should be padded to multiple blocks")
    void testLongMessagePadding() {
        privacyGuard.setMessagePadding(true);
        
        // Create a message longer than 256 bytes
        String longMessage = "A".repeat(300);
        String paddedMessage = privacyGuard.padMessage(longMessage);
        
        byte[] decoded = java.util.Base64.getDecoder().decode(paddedMessage);
        assertEquals(512, decoded.length, "Long message should be padded to 512 bytes (2 blocks)");
        
        // Verify unpadding works
        String unpaddedMessage = privacyGuard.unpadMessage(paddedMessage);
        assertEquals(longMessage, unpaddedMessage);
    }
    
    @Test
    @Order(4)
    @DisplayName("Padding disabled should return original message")
    void testPaddingDisabled() {
        privacyGuard.setMessagePadding(false);
        
        String message = "No padding here";
        String result = privacyGuard.padMessage(message);
        
        assertEquals(message, result, "With padding disabled, message should be unchanged");
    }
    
    // ═══════════════════════════════════════════════════════════════
    // TRAFFIC SCRAMBLING TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @Order(5)
    @DisplayName("Traffic scrambling should return random delays")
    void testTrafficScrambling() {
        privacyGuard.setTrafficScrambling(true);
        
        int delay1 = privacyGuard.getRandomDelay();
        int delay2 = privacyGuard.getRandomDelay();
        int delay3 = privacyGuard.getRandomDelay();
        
        // Delays should be within range
        assertTrue(delay1 >= 100 && delay1 <= 3000, "Delay should be between 100-3000ms");
        assertTrue(delay2 >= 100 && delay2 <= 3000, "Delay should be between 100-3000ms");
        assertTrue(delay3 >= 100 && delay3 <= 3000, "Delay should be between 100-3000ms");
        
        // At least one should be different (statistically likely)
        // This could theoretically fail but is extremely unlikely
        boolean allSame = (delay1 == delay2) && (delay2 == delay3);
        assertFalse(allSame, "Random delays should vary");
    }
    
    @Test
    @Order(6)
    @DisplayName("Traffic scrambling disabled should return zero delay")
    void testTrafficScramblingDisabled() {
        privacyGuard.setTrafficScrambling(false);
        
        int delay = privacyGuard.getRandomDelay();
        assertEquals(0, delay, "With scrambling disabled, delay should be 0");
    }
    
    @Test
    @Order(7)
    @DisplayName("Cover traffic should generate random data")
    void testCoverTraffic() {
        byte[] cover1 = privacyGuard.generateCoverTraffic();
        byte[] cover2 = privacyGuard.generateCoverTraffic();
        
        assertEquals(256, cover1.length, "Cover traffic should be 256 bytes");
        assertEquals(256, cover2.length, "Cover traffic should be 256 bytes");
        assertFalse(Arrays.equals(cover1, cover2), "Cover traffic should be random");
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PRIVACY MODES TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @Order(8)
    @DisplayName("Paranoid mode should enable all privacy features")
    void testParanoidMode() {
        privacyGuard.enableParanoidMode();
        
        assertTrue(privacyGuard.isParanoidMode(), "Paranoid mode should be enabled");
        assertTrue(privacyGuard.isGhostMode(), "Ghost mode should be enabled");
        assertTrue(privacyGuard.isBurnAfterRead(), "Burn after read should be enabled");
        assertTrue(privacyGuard.isMessagePadding(), "Message padding should be enabled");
        assertTrue(privacyGuard.isTrafficScrambling(), "Traffic scrambling should be enabled");
    }
    
    @Test
    @Order(9)
    @DisplayName("Disabling paranoid mode should reset features")
    void testDisableParanoidMode() {
        privacyGuard.enableParanoidMode();
        privacyGuard.disableParanoidMode();
        
        assertFalse(privacyGuard.isParanoidMode(), "Paranoid mode should be disabled");
        assertFalse(privacyGuard.isGhostMode(), "Ghost mode should be disabled");
        assertFalse(privacyGuard.isBurnAfterRead(), "Burn after read should be disabled");
        assertTrue(privacyGuard.isMessagePadding(), "Message padding should remain enabled (default)");
        assertFalse(privacyGuard.isTrafficScrambling(), "Traffic scrambling should be disabled");
    }
    
    @Test
    @Order(10)
    @DisplayName("Individual privacy settings should work independently")
    void testIndividualSettings() {
        privacyGuard.setGhostMode(true);
        assertTrue(privacyGuard.isGhostMode());
        assertFalse(privacyGuard.isBurnAfterRead());
        
        privacyGuard.setBurnAfterRead(true);
        assertTrue(privacyGuard.isBurnAfterRead());
        
        privacyGuard.setGhostMode(false);
        assertFalse(privacyGuard.isGhostMode());
        assertTrue(privacyGuard.isBurnAfterRead());
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ENCRYPTED STORAGE TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @Order(11)
    @DisplayName("Storage should be locked initially")
    void testStorageLockedInitially() {
        assertThrows(IllegalStateException.class, () -> {
            privacyGuard.saveEncrypted("test.dat", "secret".getBytes());
        }, "Should throw when storage is locked");
    }
    
    @Test
    @Order(12)
    @DisplayName("Encrypted storage round-trip should work")
    void testEncryptedStorageRoundTrip() throws Exception {
        char[] passphrase = "MySecretPassphrase123!".toCharArray();
        privacyGuard.unlockStorage(passphrase);
        
        byte[] originalData = "Top secret information!".getBytes();
        privacyGuard.saveEncrypted("secret.dat", originalData);
        
        byte[] loadedData = privacyGuard.loadEncrypted("secret.dat");
        
        assertArrayEquals(originalData, loadedData, "Loaded data should match original");
    }
    
    @Test
    @Order(13)
    @DisplayName("Encrypted file should not contain plaintext")
    void testEncryptedFileNotPlaintext() throws Exception {
        char[] passphrase = "TestPass!".toCharArray();
        privacyGuard.unlockStorage(passphrase);
        
        String secretText = "This is highly confidential";
        privacyGuard.saveEncrypted("confidential.dat", secretText.getBytes());
        
        // Read raw file contents
        byte[] rawContents = Files.readAllBytes(tempDir.resolve("confidential.dat"));
        String rawString = new String(rawContents);
        
        assertFalse(rawString.contains(secretText), "Encrypted file should not contain plaintext");
    }
    
    @Test
    @Order(14)
    @DisplayName("Loading non-existent file should return null")
    void testLoadNonExistentFile() throws Exception {
        char[] passphrase = "pass".toCharArray();
        privacyGuard.unlockStorage(passphrase);
        
        byte[] result = privacyGuard.loadEncrypted("nonexistent.dat");
        assertNull(result, "Loading non-existent file should return null");
    }
    
    // ═══════════════════════════════════════════════════════════════
    // SECURE WIPE TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @Order(15)
    @DisplayName("Secure wipe should overwrite byte array")
    void testSecureWipeBytes() {
        byte[] sensitiveData = "Secret123".getBytes();
        byte[] originalCopy = sensitiveData.clone();
        
        privacyGuard.secureWipe(sensitiveData);
        
        // All bytes should be zero after wipe
        for (byte b : sensitiveData) {
            assertEquals(0, b, "All bytes should be zero after wipe");
        }
        assertFalse(Arrays.equals(originalCopy, sensitiveData), "Data should be different after wipe");
    }
    
    @Test
    @Order(16)
    @DisplayName("Secure wipe should overwrite char array")
    void testSecureWipeChars() {
        char[] password = "MyPassword".toCharArray();
        
        privacyGuard.secureWipe(password);
        
        for (char c : password) {
            assertEquals('\0', c, "All chars should be null after wipe");
        }
    }
    
    @Test
    @Order(17)
    @DisplayName("Panic should delete all data files")
    void testPanic() throws Exception {
        // Create some files
        char[] passphrase = "test".toCharArray();
        privacyGuard.unlockStorage(passphrase);
        privacyGuard.saveEncrypted("file1.dat", "data1".getBytes());
        privacyGuard.saveEncrypted("file2.dat", "data2".getBytes());
        
        // Verify files exist
        assertTrue(Files.exists(tempDir.resolve("file1.dat")));
        assertTrue(Files.exists(tempDir.resolve("file2.dat")));
        
        // Panic!
        privacyGuard.panic();
        
        // Verify files are deleted
        assertFalse(Files.exists(tempDir.resolve("file1.dat")), "File1 should be deleted");
        assertFalse(Files.exists(tempDir.resolve("file2.dat")), "File2 should be deleted");
    }
    
    // ═══════════════════════════════════════════════════════════════
    // FINGERPRINT TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @Order(18)
    @DisplayName("Fingerprint should be consistent for same key")
    void testFingerprintConsistency() {
        byte[] publicKey = "test-public-key-bytes".getBytes();
        
        String fingerprint1 = privacyGuard.generateFingerprint(publicKey);
        String fingerprint2 = privacyGuard.generateFingerprint(publicKey);
        
        assertEquals(fingerprint1, fingerprint2, "Fingerprints should be consistent");
    }
    
    @Test
    @Order(19)
    @DisplayName("Fingerprint should have correct format (XXXX-XXXX-XXXX-XXXX-XXXX)")
    void testFingerprintFormat() {
        byte[] publicKey = "some-key-data".getBytes();
        
        String fingerprint = privacyGuard.generateFingerprint(publicKey);
        
        // Format: XXXX-XXXX-XXXX-XXXX-XXXX (4 groups of 4 hex chars separated by dashes)
        assertTrue(fingerprint.matches("[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}"),
                "Fingerprint should match format: " + fingerprint);
    }
    
    @Test
    @Order(20)
    @DisplayName("Different keys should produce different fingerprints")
    void testDifferentFingerprints() {
        byte[] key1 = "first-key".getBytes();
        byte[] key2 = "second-key".getBytes();
        
        String fingerprint1 = privacyGuard.generateFingerprint(key1);
        String fingerprint2 = privacyGuard.generateFingerprint(key2);
        
        assertNotEquals(fingerprint1, fingerprint2, "Different keys should have different fingerprints");
    }
    
    @Test
    @Order(21)
    @DisplayName("Word fingerprint should contain 6 words")
    void testWordFingerprint() {
        byte[] publicKey = "test-key".getBytes();
        
        String wordFingerprint = privacyGuard.generateWordFingerprint(publicKey);
        String[] words = wordFingerprint.split(" ");
        
        assertEquals(6, words.length, "Word fingerprint should have 6 words");
        for (String word : words) {
            assertTrue(word.length() > 0, "Each word should be non-empty");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // DECOY DATA TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @Order(22)
    @DisplayName("Decoy data should create normal-looking files")
    void testDecoyData() throws Exception {
        Path decoyDir = tempDir.resolve("decoy");
        privacyGuard.createDecoyData(decoyDir);
        
        assertTrue(Files.exists(decoyDir.resolve("notes.txt")), "notes.txt should exist");
        assertTrue(Files.exists(decoyDir.resolve("todo.txt")), "todo.txt should exist");
        
        String notes = Files.readString(decoyDir.resolve("notes.txt"));
        assertTrue(notes.contains("Shopping list"), "Notes should look normal");
    }
}
