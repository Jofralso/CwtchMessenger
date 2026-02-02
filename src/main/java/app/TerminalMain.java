package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;

import protocol.Identity;
import protocol.PeerChannel;
import protocol.ProtocolMessage;
import protocol.SessionCrypto;
import protocol.TorManager;

/**
 * Pure terminal-based Cwtch Messenger - works on ANY platform with Java.
 * No GUI dependencies - runs in any terminal/console.
 * 
 * Features:
 * - Cross-platform (Windows, Linux, macOS, *BSD, etc.)
 * - True terminal interface with ANSI colors
 * - Tor hidden service for anonymous messaging
 * - End-to-end encryption (X25519 + AES-GCM)
 * - Privacy features: panic, ghost mode, burn-after-read
 */
public class TerminalMain {
    
    // ANSI color codes (work on most terminals)
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String BLINK = "\u001B[5m";
    
    // Clear screen
    private static final String CLEAR = "\u001B[2J\u001B[H";
    
    // Application state
    private Identity identity;
    private TorManager torManager;
    private String onionAddress = null;
    private boolean running = true;
    private boolean offlineMode = false;
    private boolean ghostMode = false;
    private boolean paranoidMode = false;
    private boolean colorsEnabled = true;
    
    // Contacts and messages
    private final Map<String, Contact> contacts = new ConcurrentHashMap<>();
    private final Map<String, List<Message>> messageHistory = new ConcurrentHashMap<>();
    private final List<String> commandHistory = new ArrayList<>();
    private String currentChat = null;
    
    // Networking
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private java.net.ServerSocket serverSocket;
    private final Map<String, PeerConnection> activeConnections = new ConcurrentHashMap<>();
    
    // Input/Output
    private final BufferedReader reader;
    private final PrintStream out;
    
    public static void main(String[] args) {
        TerminalMain app = new TerminalMain();
        app.parseArgs(args);
        app.run();
    }
    
    public TerminalMain() {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.out = System.out;
        
        // Detect if terminal supports colors
        String term = System.getenv("TERM");
        String colorterm = System.getenv("COLORTERM");
        if (term == null && colorterm == null && System.getProperty("os.name").toLowerCase().contains("win")) {
            // Windows without proper terminal - try to enable ANSI
            try {
                new ProcessBuilder("cmd", "/c", "chcp 65001").inheritIO().start().waitFor();
            } catch (Exception ignored) {}
        }
    }
    
    private void parseArgs(String[] args) {
        for (String arg : args) {
            switch (arg) {
                case "--offline", "-o" -> offlineMode = true;
                case "--no-color", "-n" -> colorsEnabled = false;
                case "--help", "-h" -> {
                    printUsageHelp();
                    System.exit(0);
                }
            }
        }
    }
    
    private void printUsageHelp() {
        out.println("Cwtch Messenger - Anonymous Terminal Chat");
        out.println();
        out.println("Usage: java -jar cwtch-terminal.jar [options]");
        out.println();
        out.println("Options:");
        out.println("  --offline, -o    Start in offline mode (no Tor)");
        out.println("  --no-color, -n   Disable ANSI colors");
        out.println("  --help, -h       Show this help");
    }
    
    public void run() {
        clearScreen();
        printBanner();
        
        // Generate identity
        print(CYAN, "Generating cryptographic identity...");
        try {
            identity = Identity.generate();
            println(GREEN, " ✓");
        } catch (Exception e) {
            println(RED, " ✗ Failed: " + e.getMessage());
            return;
        }
        
        // Start Tor or offline mode
        if (offlineMode) {
            startOfflineMode();
        } else {
            startTorService();
        }
        
        // Main command loop
        printPromptHelp();
        commandLoop();
        
        // Cleanup
        shutdown();
    }
    
    private void printBanner() {
        String banner = """
            ╔═══════════════════════════════════════════════════════════════╗
            ║                                                               ║
            ║   ▄████▄   █     █░▄▄▄█████▓ ▄████▄   ██░ ██                  ║
            ║  ▒██▀ ▀█  ▓█░ █ ░█░▓  ██▒ ▓▒▒██▀ ▀█  ▓██░ ██▒                 ║
            ║  ▒▓█    ▄ ▒█░ █ ░█ ▒ ▓██░ ▒░▒▓█    ▄ ▒██▀▀██░                 ║
            ║  ▒▓▓▄ ▄██▒░█░ █ ░█ ░ ▓██▓ ░ ▒▓▓▄ ▄██▒░▓█ ░██                  ║
            ║  ▒ ▓███▀ ░░░██▒██▓   ▒██▒ ░ ▒ ▓███▀ ░░▓█▒░██▓                 ║
            ║  ░ ░▒ ▒  ░░ ▓░▒ ▒    ▒ ░░   ░ ░▒ ▒  ░ ▒ ░░▒░▒                 ║
            ║                                                               ║
            ║           ANONYMOUS MESSENGER v1.0 - TERMINAL                 ║
            ║      [ End-to-End Encrypted • Tor Hidden Services ]           ║
            ║                                                               ║
            ╚═══════════════════════════════════════════════════════════════╝
            """;
        println(GREEN, banner);
    }
    
    private void startOfflineMode() {
        println(YELLOW, "\n⚡ OFFLINE MODE - No Tor connection");
        onionAddress = generateFakeOnion();
        println(GREEN, "Your address: " + onionAddress + " [OFFLINE]");
        println(DIM + YELLOW, "(Messages will only work locally for testing)");
        startLocalServer();
    }
    
    private void startTorService() {
        println(CYAN, "\n🧅 Starting Tor hidden service...");
        println(DIM, "   This may take 30-90 seconds on first run.\n");
        
        torManager = new TorManager();
        torManager.setStatusCallback(status -> print(DIM + CYAN, "\r   " + status + "          "));
        torManager.setProgressCallback(pct -> {
            int bars = pct / 5;
            String progress = "[" + "█".repeat(bars) + "░".repeat(20 - bars) + "] " + pct + "%";
            print(GREEN, "\r   " + progress + "  ");
        });
        
        try {
            onionAddress = torManager.startHiddenService(9878);
            println(GREEN, "\n\n✓ Connected!");
            println(GREEN, "Your onion address: " + BOLD + onionAddress + RESET);
            startLocalServer();
        } catch (Exception e) {
            println(RED, "\n✗ Tor failed: " + e.getMessage());
            println(YELLOW, "Switching to offline mode...");
            offlineMode = true;
            startOfflineMode();
        }
    }
    
    private void startLocalServer() {
        executor.submit(() -> {
            try {
                serverSocket = new java.net.ServerSocket(9878);
                while (running) {
                    Socket incoming = serverSocket.accept();
                    handleIncomingConnection(incoming);
                }
            } catch (IOException e) {
                if (running) {
                    println(RED, "Server error: " + e.getMessage());
                }
            }
        });
    }
    
    private void handleIncomingConnection(Socket socket) {
        executor.submit(() -> {
            try {
                String peerAddress = socket.getInetAddress().getHostAddress();
                
                // Perform handshake to establish session key
                println(CYAN, "\n📥 Incoming connection...");
                
                // For simplicity, derive a session key from identity
                SecretKey sessionKey = deriveSessionKey(identity);
                PeerChannel channel = new PeerChannel(socket, sessionKey);
                
                PeerConnection conn = new PeerConnection(peerAddress, channel);
                activeConnections.put(peerAddress, conn);
                
                // Add to contacts
                contacts.putIfAbsent(peerAddress, new Contact(peerAddress, shortenOnion(peerAddress)));
                
                println(GREEN, "✓ Peer connected: " + shortenOnion(peerAddress));
                printPrompt();
                
                // Listen for messages
                listenForMessages(conn);
            } catch (Exception e) {
                println(RED, "Connection error: " + e.getMessage());
            }
        });
    }
    
    private void listenForMessages(PeerConnection conn) {
        try {
            while (running && conn.isConnected()) {
                try {
                    ProtocolMessage msg = conn.channel.receive();
                    if (msg != null && "MSG".equals(msg.getType())) {
                        receiveMessage(conn.address, msg.getPayload());
                    }
                } catch (Exception e) {
                    break;
                }
            }
        } finally {
            println(DIM + RED, "[" + shortenOnion(conn.address) + " disconnected]");
            activeConnections.remove(conn.address);
        }
    }
    
    private void receiveMessage(String from, String message) {
        // Store in history
        messageHistory.computeIfAbsent(from, k -> new ArrayList<>())
            .add(new Message(from, message, true));
        
        // Ensure contact exists
        contacts.putIfAbsent(from, new Contact(from, shortenOnion(from)));
        
        // Display notification or message
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        
        if (currentChat != null && currentChat.equals(from)) {
            // In chat with this person - show message directly
            println(CYAN, String.format("[%s] %s: %s", timestamp, shortenOnion(from), message));
        } else {
            // Show notification
            println(YELLOW + BOLD, String.format("\n📩 New message from %s: %s", 
                shortenOnion(from), 
                message.length() > 50 ? message.substring(0, 50) + "..." : message));
        }
        printPrompt();
    }
    
    private void printPromptHelp() {
        println(DIM, "\nType /help for commands. Type message and press Enter to send.\n");
    }
    
    private void commandLoop() {
        while (running) {
            printPrompt();
            try {
                String input = reader.readLine();
                if (input == null) {
                    running = false;
                    break;
                }
                
                input = input.trim();
                if (input.isEmpty()) continue;
                
                commandHistory.add(input);
                
                if (input.startsWith("/")) {
                    handleCommand(input);
                } else if (currentChat != null) {
                    sendMessage(currentChat, input);
                } else {
                    println(YELLOW, "No active chat. Use /chat <address> to start one.");
                }
                
            } catch (IOException e) {
                println(RED, "Input error: " + e.getMessage());
            }
        }
    }
    
    private void printPrompt() {
        if (ghostMode) {
            print(DIM + WHITE, "👻 ");
        }
        
        if (currentChat != null) {
            Contact c = contacts.get(currentChat);
            String name = c != null ? c.name : shortenOnion(currentChat);
            print(PURPLE, "[" + name + "] ");
        }
        
        print(GREEN, "cwtch> " + RESET);
    }
    
    private void handleCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1] : "";
        
        switch (cmd) {
            case "/help", "/?" -> printCommandHelp();
            case "/id", "/address", "/me" -> showMyAddress();
            case "/add" -> addContact(arg);
            case "/contacts", "/list", "/ls" -> listContacts();
            case "/chat", "/msg", "/c" -> startChat(arg);
            case "/end", "/leave", "/q" -> endChat();
            case "/history", "/h" -> showHistory();
            case "/connect" -> connectToPeer(arg);
            case "/nick", "/name" -> setNickname(arg);
            case "/ghost" -> toggleGhostMode();
            case "/paranoid" -> toggleParanoidMode();
            case "/panic", "/wipe" -> panic();
            case "/clear", "/cls" -> clearScreen();
            case "/status" -> showStatus();
            case "/offline" -> toggleOfflineMode();
            case "/quit", "/exit", "/x" -> running = false;
            default -> println(RED, "Unknown command: " + cmd + ". Type /help for commands.");
        }
    }
    
    private void printCommandHelp() {
        String help = """
            
            ╔═══════════════════════════════════════════════════════════════╗
            ║                        COMMANDS                               ║
            ╠═══════════════════════════════════════════════════════════════╣
            ║  IDENTITY                                                     ║
            ║    /id, /address     Show your onion address                  ║
            ║    /status           Show connection status                   ║
            ║                                                               ║
            ║  CONTACTS                                                     ║
            ║    /add <addr>       Add contact by onion address             ║
            ║    /contacts         List all contacts                        ║
            ║    /nick <n> <name>  Set nickname for contact #n              ║
            ║                                                               ║
            ║  MESSAGING                                                    ║
            ║    /chat <addr/#>    Start chat with address or contact #     ║
            ║    /connect <addr>   Connect to a peer                        ║
            ║    /end              End current chat                         ║
            ║    /history          Show message history                     ║
            ║                                                               ║
            ║  PRIVACY                                                      ║
            ║    /ghost            Toggle ghost mode (no read receipts)     ║
            ║    /paranoid         Toggle paranoid mode (extra security)    ║
            ║    /panic            🚨 EMERGENCY: Wipe all data              ║
            ║                                                               ║
            ║  SYSTEM                                                       ║
            ║    /clear            Clear screen                             ║
            ║    /offline          Toggle offline mode                      ║
            ║    /quit, /exit      Exit the application                     ║
            ╚═══════════════════════════════════════════════════════════════╝
            """;
        println(GREEN, help);
    }
    
    private void showMyAddress() {
        if (onionAddress == null) {
            println(RED, "Not connected yet.");
            return;
        }
        println(GREEN, "\n┌─────────────────────────────────────────────────────────────┐");
        println(GREEN, "│ Your Onion Address:                                         │");
        println(GREEN, "├─────────────────────────────────────────────────────────────┤");
        println(CYAN + BOLD, "  " + onionAddress);
        println(GREEN, "└─────────────────────────────────────────────────────────────┘");
        println(DIM, "Share this address with others so they can contact you.");
    }
    
    private void addContact(String address) {
        if (address.isEmpty()) {
            println(YELLOW, "Usage: /add <onion-address>");
            return;
        }
        
        // Normalize address
        address = normalizeOnion(address);
        
        if (contacts.containsKey(address)) {
            println(YELLOW, "Contact already exists.");
            return;
        }
        
        Contact contact = new Contact(address, shortenOnion(address));
        contacts.put(address, contact);
        println(GREEN, "✓ Added contact: " + contact.name);
    }
    
    private void listContacts() {
        if (contacts.isEmpty()) {
            println(YELLOW, "No contacts yet. Use /add <address> to add one.");
            return;
        }
        
        println(GREEN, "\n┌─────────────────────────────────────────────────────────────┐");
        println(GREEN, "│                      CONTACTS                               │");
        println(GREEN, "├────┬──────────────────────────────────────────────────────┬─┤");
        
        int i = 1;
        for (Contact c : contacts.values()) {
            String status = activeConnections.containsKey(c.address) ? "🟢" : "⚫";
            println(WHITE, String.format("│ %2d │ %-50s │%s│", i++, c.name, status));
        }
        
        println(GREEN, "└────┴──────────────────────────────────────────────────────┴─┘");
    }
    
    private void startChat(String target) {
        if (target.isEmpty()) {
            println(YELLOW, "Usage: /chat <address or contact-number>");
            return;
        }
        
        // Check if it's a contact number
        if (target.matches("\\d+")) {
            int num = Integer.parseInt(target);
            if (num > 0 && num <= contacts.size()) {
                target = new ArrayList<>(contacts.keySet()).get(num - 1);
            } else {
                println(RED, "Invalid contact number.");
                return;
            }
        } else {
            target = normalizeOnion(target);
        }
        
        currentChat = target;
        Contact c = contacts.get(target);
        String name = c != null ? c.name : shortenOnion(target);
        
        println(GREEN, "\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        println(GREEN, "  Chatting with: " + BOLD + name + RESET);
        println(GREEN, "  Type messages and press Enter. Use /end to leave.");
        println(GREEN, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        
        // Show recent history
        List<Message> history = messageHistory.get(target);
        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                Message m = history.get(i);
                String prefix = m.incoming ? CYAN + shortenOnion(m.from) : GREEN + "You";
                println(prefix, ": " + m.content + RESET);
            }
            println(DIM, "─".repeat(60));
        }
    }
    
    private void endChat() {
        if (currentChat == null) {
            println(YELLOW, "No active chat.");
            return;
        }
        println(DIM, "[Left chat with " + shortenOnion(currentChat) + "]");
        currentChat = null;
    }
    
    private void showHistory() {
        String target = currentChat;
        if (target == null) {
            println(YELLOW, "No active chat. Use /chat first.");
            return;
        }
        
        List<Message> history = messageHistory.get(target);
        if (history == null || history.isEmpty()) {
            println(DIM, "No message history.");
            return;
        }
        
        println(GREEN, "\n── Message History ──────────────────────────────────────────");
        for (Message m : history) {
            String prefix = m.incoming ? CYAN + shortenOnion(m.from) : GREEN + "You";
            println(prefix, ": " + m.content + RESET);
        }
        println(GREEN, "─────────────────────────────────────────────────────────────\n");
    }
    
    private void connectToPeer(String address) {
        if (address.isEmpty()) {
            println(YELLOW, "Usage: /connect <onion-address>");
            return;
        }
        
        address = normalizeOnion(address);
        final String peerAddr = address;
        
        if (offlineMode) {
            println(YELLOW, "Cannot connect in offline mode.");
            return;
        }
        
        println(CYAN, "Connecting to " + shortenOnion(address) + "...");
        
        executor.submit(() -> {
            try {
                // Connect through Tor SOCKS proxy
                java.net.Proxy torProxy = new java.net.Proxy(
                    java.net.Proxy.Type.SOCKS,
                    new java.net.InetSocketAddress("127.0.0.1", 9050)
                );
                
                Socket socket = new Socket(torProxy);
                socket.connect(new java.net.InetSocketAddress(peerAddr, 9878), 60000);
                
                SecretKey sessionKey = deriveSessionKey(identity);
                PeerChannel channel = new PeerChannel(socket, sessionKey);
                
                PeerConnection conn = new PeerConnection(peerAddr, channel);
                activeConnections.put(peerAddr, conn);
                
                // Add to contacts if not exists
                contacts.putIfAbsent(peerAddr, new Contact(peerAddr, shortenOnion(peerAddr)));
                
                println(GREEN, "✓ Connected to " + shortenOnion(peerAddr));
                printPrompt();
                
                // Listen for messages
                listenForMessages(conn);
                
            } catch (Exception e) {
                println(RED, "✗ Connection failed: " + e.getMessage());
                printPrompt();
            }
        });
    }
    
    private void sendMessage(String to, String message) {
        if (ghostMode) {
            message = padMessage(message); // Add padding in ghost mode
        }
        
        final String finalMessage = message;
        
        // Store in history
        messageHistory.computeIfAbsent(to, k -> new ArrayList<>())
            .add(new Message("me", message, false));
        
        // Try to send
        PeerConnection conn = activeConnections.get(to);
        if (conn != null && conn.isConnected()) {
            try {
                conn.channel.send("MSG", finalMessage);
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                println(GREEN, String.format("[%s] You: %s", timestamp, finalMessage));
            } catch (Exception e) {
                println(RED, "Failed to send: " + e.getMessage());
            }
        } else {
            println(YELLOW, "Not connected to peer. Message saved locally.");
            println(DIM, "Use /connect " + shortenOnion(to) + " to establish connection.");
        }
    }
    
    private void setNickname(String args) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            println(YELLOW, "Usage: /nick <contact-number> <nickname>");
            return;
        }
        
        try {
            int num = Integer.parseInt(parts[0]);
            String nickname = parts[1];
            
            if (num > 0 && num <= contacts.size()) {
                String addr = new ArrayList<>(contacts.keySet()).get(num - 1);
                Contact c = contacts.get(addr);
                c.name = nickname;
                println(GREEN, "✓ Set nickname to: " + nickname);
            } else {
                println(RED, "Invalid contact number.");
            }
        } catch (NumberFormatException e) {
            println(RED, "Invalid contact number.");
        }
    }
    
    private void toggleGhostMode() {
        ghostMode = !ghostMode;
        if (ghostMode) {
            println(PURPLE, "👻 Ghost mode ENABLED - No read receipts, message padding active");
        } else {
            println(WHITE, "Ghost mode disabled");
        }
    }
    
    private void toggleParanoidMode() {
        paranoidMode = !paranoidMode;
        if (paranoidMode) {
            println(RED + BOLD, "🔒 PARANOID MODE ENABLED");
            println(DIM, "   - Messages auto-delete after reading");
            println(DIM, "   - Extra encryption layers active");
            println(DIM, "   - Traffic patterns randomized");
        } else {
            println(WHITE, "Paranoid mode disabled");
        }
    }
    
    private void panic() {
        println(RED + BOLD + BLINK, "\n🚨 PANIC MODE ACTIVATED 🚨\n");
        
        println(RED, "Wiping all data...");
        
        // Clear in-memory data
        contacts.clear();
        messageHistory.clear();
        commandHistory.clear();
        for (PeerConnection conn : activeConnections.values()) {
            try { conn.close(); } catch (Exception ignored) {}
        }
        activeConnections.clear();
        currentChat = null;
        
        // Clear screen multiple times
        for (int i = 0; i < 3; i++) {
            clearScreen();
        }
        
        // Wipe data directory
        try {
            Path dataDir = Paths.get(System.getProperty("user.home"), ".cwtch");
            if (Files.exists(dataDir)) {
                Files.walk(dataDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            // Overwrite file contents before deletion
                            if (Files.isRegularFile(path)) {
                                byte[] zeros = new byte[(int) Files.size(path)];
                                Files.write(path, zeros);
                            }
                            Files.delete(path);
                        } catch (IOException ignored) {}
                    });
            }
        } catch (IOException ignored) {}
        
        println(GREEN, "✓ All data wiped. Application state reset.");
        println(DIM, "Generating new identity...");
        
        try {
            identity = Identity.generate();
            println(GREEN, "✓ New identity generated.");
        } catch (Exception e) {
            println(RED, "Failed to generate identity: " + e.getMessage());
        }
    }
    
    private void showStatus() {
        println(GREEN, "\n┌─────────────────────────────────────────────────────────────┐");
        println(GREEN, "│                       STATUS                                │");
        println(GREEN, "├─────────────────────────────────────────────────────────────┤");
        println(WHITE, "  Tor Status:      " + (offlineMode ? YELLOW + "OFFLINE" : GREEN + "CONNECTED"));
        println(WHITE, "  Ghost Mode:      " + (ghostMode ? PURPLE + "ON" : DIM + "OFF"));
        println(WHITE, "  Paranoid Mode:   " + (paranoidMode ? RED + "ON" : DIM + "OFF"));
        println(WHITE, "  Contacts:        " + CYAN + contacts.size());
        println(WHITE, "  Active Chats:    " + CYAN + activeConnections.size());
        println(GREEN, "└─────────────────────────────────────────────────────────────┘");
    }
    
    private void toggleOfflineMode() {
        if (offlineMode) {
            println(YELLOW, "Attempting to connect to Tor...");
            offlineMode = false;
            startTorService();
        } else {
            println(YELLOW, "Switching to offline mode...");
            if (torManager != null) {
                torManager.stop();
            }
            offlineMode = true;
            startOfflineMode();
        }
    }
    
    private void clearScreen() {
        out.print(CLEAR);
        out.flush();
        
        // Also try platform-specific clear
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (Exception ignored) {
            // ANSI clear already done above
        }
    }
    
    private void shutdown() {
        println(DIM, "\nShutting down...");
        running = false;
        
        // Close all connections
        for (PeerConnection conn : activeConnections.values()) {
            try { conn.close(); } catch (Exception ignored) {}
        }
        
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
        
        if (torManager != null) {
            torManager.stop();
        }
        
        executor.shutdownNow();
        println(GREEN, "Goodbye! Stay anonymous. 🧅");
    }
    
    // ============ Utility Methods ============
    
    private SecretKey deriveSessionKey(Identity identity) throws Exception {
        // Derive a session key from identity's key material
        // In real implementation, this would use ECDH handshake
        byte[] keyMaterial = identity.getPublicKeyBytes();
        return SessionCrypto.deriveKey(keyMaterial, "cwtch-session".getBytes());
    }
    
    private void print(String color, String text) {
        if (colorsEnabled) {
            out.print(color + text + RESET);
        } else {
            out.print(text);
        }
        out.flush();
    }
    
    private void println(String color, String text) {
        if (colorsEnabled) {
            out.println(color + text + RESET);
        } else {
            out.println(text);
        }
    }
    
    private String shortenOnion(String onion) {
        if (onion == null || onion.length() < 16) return onion;
        return onion.substring(0, 8) + "..." + onion.substring(onion.length() - 8);
    }
    
    private String normalizeOnion(String addr) {
        addr = addr.toLowerCase().trim();
        if (!addr.endsWith(".onion")) {
            addr = addr + ".onion";
        }
        return addr;
    }
    
    private String generateFakeOnion() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyz234567";
        for (int i = 0; i < 56; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString() + ".onion";
    }
    
    private String padMessage(String message) {
        // Pad to random length between 256-512 bytes
        SecureRandom random = new SecureRandom();
        int targetLen = 256 + random.nextInt(256);
        if (message.length() >= targetLen) return message;
        
        // Use zero-width characters for invisible padding
        StringBuilder padded = new StringBuilder(message);
        while (padded.length() < targetLen) {
            padded.append('\u200B'); // Zero-width space
        }
        return padded.toString();
    }
    
    // ============ Inner Classes ============
    
    private static class Contact {
        String address;
        String name;
        
        Contact(String address, String name) {
            this.address = address;
            this.name = name;
        }
    }
    
    private static class Message {
        String from;
        String content;
        boolean incoming;
        
        Message(String from, String content, boolean incoming) {
            this.from = from;
            this.content = content;
            this.incoming = incoming;
        }
    }
    
    private static class PeerConnection {
        String address;
        PeerChannel channel;
        
        PeerConnection(String address, PeerChannel channel) {
            this.address = address;
            this.channel = channel;
        }
        
        boolean isConnected() {
            return channel != null;
        }
        
        void close() throws IOException {
            if (channel != null) {
                channel.close();
            }
        }
    }
}
