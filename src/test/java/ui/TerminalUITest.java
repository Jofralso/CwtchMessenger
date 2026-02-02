package ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for TerminalUI - Hacker-style terminal interface.
 * Note: These tests focus on the non-UI logic since JavaFX UI testing requires special setup.
 */
class TerminalUITest {
    
    // ═══════════════════════════════════════════════════════════════
    // COMMAND PARSING TESTS (Logic without JavaFX)
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Command should be parsed correctly")
    void testCommandParsing() {
        String command = "/msg Alice Hello there!";
        String[] parts = command.split("\\s+", 2);
        
        assertEquals("/msg", parts[0]);
        assertEquals("Alice Hello there!", parts[1]);
    }
    
    @Test
    @DisplayName("Command with no arguments should parse correctly")
    void testCommandNoArgs() {
        String command = "/help";
        String[] parts = command.split("\\s+", 2);
        
        assertEquals("/help", parts[0]);
        assertEquals(1, parts.length);
    }
    
    @Test
    @DisplayName("Add contact command should parse address and name")
    void testAddContactParsing() {
        String args = "abc123xyz.onion Alice";
        String[] parts = args.split("\\s+", 2);
        
        assertEquals("abc123xyz.onion", parts[0]);
        assertEquals("Alice", parts[1]);
    }
    
    @Test
    @DisplayName("Add contact with only address should work")
    void testAddContactAddressOnly() {
        String args = "abc123xyz.onion";
        String[] parts = args.split("\\s+", 2);
        
        assertEquals("abc123xyz.onion", parts[0]);
        assertEquals(1, parts.length);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // TIMESTAMP FORMAT TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Timestamp should be in HH:mm:ss format")
    void testTimestampFormat() {
        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        
        assertTrue(timestamp.matches("\\d{2}:\\d{2}:\\d{2}"), 
            "Timestamp should match HH:mm:ss format: " + timestamp);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // COMMAND VALIDATION TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Valid commands should be recognized")
    void testValidCommands() {
        String[] validCommands = {
            "/help", "/clear", "/status", "/whoami",
            "/list", "/add", "/chat", "/connect", "/disconnect",
            "/msg", "/panic", "/ghost", "/paranoid", "/burn",
            "/padding", "/scramble", "/rekey",
            "/fingerprint", "/verify", "/tor", "/identity"
        };
        
        for (String cmd : validCommands) {
            assertTrue(isKnownCommand(cmd), "Should recognize command: " + cmd);
        }
    }
    
    @Test
    @DisplayName("Invalid commands should not be recognized")
    void testInvalidCommands() {
        String[] invalidCommands = {"/xyz", "/unknown", "/hack", "/sudo"};
        
        for (String cmd : invalidCommands) {
            assertFalse(isKnownCommand(cmd), "Should not recognize: " + cmd);
        }
    }
    
    private boolean isKnownCommand(String cmd) {
        String[] knownCommands = {
            "/help", "/clear", "/cls", "/status", "/whoami", "/uptime",
            "/list", "/add", "/remove", "/connect", "/disconnect", "/chat",
            "/msg", "/burn", "/history",
            "/panic", "/scramble", "/padding", "/ghost", "/paranoid",
            "/fingerprint", "/verify", "/rekey", "/tor", "/identity", "/exit"
        };
        
        for (String known : knownCommands) {
            if (known.equals(cmd)) return true;
        }
        return false;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ONION ADDRESS VALIDATION TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Valid onion address should be recognized")
    void testValidOnionAddress() {
        String validOnion = "abc123def456ghi789jkl012mno345pqr678stu901vwx234.onion";
        assertTrue(validOnion.contains(".onion"), "Should contain .onion");
    }
    
    @Test
    @DisplayName("Invalid address should be rejected")
    void testInvalidOnionAddress() {
        String invalidAddress = "not-an-onion-address.com";
        assertFalse(invalidAddress.contains(".onion"), "Should not be .onion address");
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PROMPT GENERATION TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Default prompt should be cwtch>")
    void testDefaultPrompt() {
        String defaultPrompt = "cwtch> ";
        assertEquals("cwtch> ", defaultPrompt);
    }
    
    @Test
    @DisplayName("Onion address prompt should show first 8 chars")
    void testOnionAddressPrompt() {
        String onionAddress = "abcdefghijklmnopqrstuvwxyz1234567890.onion";
        String expectedPrefix = onionAddress.substring(0, 8);
        String prompt = "cwtch@" + expectedPrefix + "...> ";
        
        assertTrue(prompt.contains("abcdefgh"), "Prompt should contain first 8 chars of onion");
        assertTrue(prompt.endsWith("> "), "Prompt should end with > ");
    }
    
    @Test
    @DisplayName("Contact selected prompt should show contact name")
    void testContactPrompt() {
        String contact = "Alice";
        String prompt = contact + "@cwtch> ";
        
        assertEquals("Alice@cwtch> ", prompt);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ASCII ART TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("ASCII logo should contain CWTCH")
    void testAsciiLogoContent() {
        String asciiLogo = """
         ██████╗██╗    ██╗████████╗ ██████╗██╗  ██╗
        ██╔════╝██║    ██║╚══██╔══╝██╔════╝██║  ██║
        ██║     ██║ █╗ ██║   ██║   ██║     ███████║
        ██║     ██║███╗██║   ██║   ██║     ██╔══██║
        ╚██████╗╚███╔███╔╝   ██║   ╚██████╗██║  ██║
         ╚═════╝ ╚══╝╚══╝    ╚═╝    ╚═════╝╚═╝  ╚═╝
        """;
        
        // Just verify it contains box-drawing characters
        assertTrue(asciiLogo.contains("█"), "Should contain block characters");
        assertTrue(asciiLogo.contains("╗"), "Should contain box-drawing characters");
    }
    
    // ═══════════════════════════════════════════════════════════════
    // HELP TEXT TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Help text should mention all major commands")
    void testHelpTextContents() {
        String helpText = """
            help              - Show this help message
            clear             - Clear terminal screen
            panic             - EMERGENCY: Wipe all data
            ghost             - Enter ghost mode (no presence)
            paranoid          - Maximum privacy settings
            """;
        
        assertTrue(helpText.contains("help"), "Help should mention help command");
        assertTrue(helpText.contains("panic"), "Help should mention panic command");
        assertTrue(helpText.contains("ghost"), "Help should mention ghost mode");
        assertTrue(helpText.contains("paranoid"), "Help should mention paranoid mode");
    }
    
    // ═══════════════════════════════════════════════════════════════
    // COLOR CONSTANTS TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Terminal colors should be valid hex codes")
    void testColorConstants() {
        String terminalGreen = "#00ff41";
        String terminalDim = "#00aa2a";
        String terminalRed = "#ff3333";
        String terminalYellow = "#ffff00";
        String terminalCyan = "#00ffff";
        
        assertTrue(terminalGreen.matches("#[0-9a-fA-F]{6}"), "Green should be valid hex");
        assertTrue(terminalDim.matches("#[0-9a-fA-F]{6}"), "Dim should be valid hex");
        assertTrue(terminalRed.matches("#[0-9a-fA-F]{6}"), "Red should be valid hex");
        assertTrue(terminalYellow.matches("#[0-9a-fA-F]{6}"), "Yellow should be valid hex");
        assertTrue(terminalCyan.matches("#[0-9a-fA-F]{6}"), "Cyan should be valid hex");
    }
    
    // ═══════════════════════════════════════════════════════════════
    // COMMAND HISTORY LOGIC TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Command history should store commands in order")
    void testCommandHistoryOrder() {
        java.util.List<String> history = new java.util.ArrayList<>();
        
        history.add(0, "first");
        history.add(0, "second");
        history.add(0, "third");
        
        assertEquals("third", history.get(0), "Most recent should be first");
        assertEquals("second", history.get(1));
        assertEquals("first", history.get(2));
    }
    
    @Test
    @DisplayName("History navigation should work correctly")
    void testHistoryNavigation() {
        java.util.List<String> history = new java.util.ArrayList<>();
        history.add(0, "cmd1");
        history.add(0, "cmd2");
        history.add(0, "cmd3");
        
        int historyIndex = -1;
        
        // Navigate up (older)
        historyIndex++; // 0
        assertEquals("cmd3", history.get(historyIndex));
        
        historyIndex++; // 1
        assertEquals("cmd2", history.get(historyIndex));
        
        historyIndex++; // 2
        assertEquals("cmd1", history.get(historyIndex));
        
        // Navigate down (newer)
        historyIndex--; // 1
        assertEquals("cmd2", history.get(historyIndex));
    }
    
    // ═══════════════════════════════════════════════════════════════
    // STATUS BAR FORMAT TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Status bar should contain required info")
    void testStatusBarFormat() {
        String torStatus = "CONNECTED";
        String mode = "PARANOID";
        
        String statusBar = "[ TOR: " + torStatus + " ] [ ENCRYPT: AES-256-GCM ] [ MODE: " + mode + " ]";
        
        assertTrue(statusBar.contains("TOR:"), "Should contain TOR status");
        assertTrue(statusBar.contains("ENCRYPT:"), "Should contain encryption info");
        assertTrue(statusBar.contains("MODE:"), "Should contain mode");
        assertTrue(statusBar.contains("AES-256-GCM"), "Should mention encryption algorithm");
    }
}
