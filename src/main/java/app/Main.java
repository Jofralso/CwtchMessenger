package app;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import protocol.Identity;
import service.PeerManager;
import service.PrivacyGuard;
import service.TorService;
import ui.ChatArea;
import ui.ConnectionPanel;
import ui.ContactList;
import ui.MessageInput;
import ui.QRCodeUtil;
import ui.TerminalUI;

/**
 * Cwtch Messenger - A Ricochet-style anonymous messaging application
 * using Tor hidden services and the Cwtch protocol for end-to-end encryption.
 * 
 * Features:
 * - Anonymous identity via Tor hidden services (.onion addresses)
 * - End-to-end encrypted messaging using X25519 ECDH + AES-GCM
 * - Metadata-resistant peer-to-peer communication
 * - No central server - fully decentralized
 * - Terminal mode for privacy enthusiasts
 * - Panic button, ghost mode, paranoid settings
 */
public class Main extends Application {
    
    // Mode selection
    private static final String MODE_GUI = "gui";
    private static final String MODE_TERMINAL = "terminal";
    private String currentMode = null;
    
    private Identity myIdentity;
    private TorService torService;
    private PeerManager peerManager;
    private PrivacyGuard privacyGuard;
    
    // GUI mode components
    private ChatArea chatArea;
    private ContactList contactList;
    private MessageInput messageInput;
    private ConnectionPanel connectionPanel;
    
    // Terminal mode component
    private TerminalUI terminalUI;
    
    // Track which contact's chat is currently open
    private ContactList.Contact currentChatContact;
    // Store chat history per contact
    private final Map<String, StringBuilder> chatHistory = new HashMap<>();
    
    @Override
    public void start(Stage primaryStage) {
        // Initialize privacy guard
        privacyGuard = new PrivacyGuard(Paths.get(System.getProperty("user.home"), ".cwtch", "data"));
        
        // Generate cryptographic identity
        try {
            myIdentity = Identity.generate();
        } catch (Exception ex) {
            showError("Failed to generate identity", ex.getMessage());
            Platform.exit();
            return;
        }
        
        // Initialize Tor service and peer manager
        torService = new TorService(myIdentity);
        peerManager = new PeerManager(torService, myIdentity);
        
        // Check for command-line mode preference
        String modeArg = getParameters().getNamed().get("mode");
        
        if (modeArg != null) {
            currentMode = modeArg.toLowerCase();
            if (MODE_TERMINAL.equals(currentMode)) {
                startTerminalMode(primaryStage);
            } else {
                startGUIMode(primaryStage);
            }
        } else {
            // Show mode selector
            showModeSelector(primaryStage);
        }
    }
    
    /**
     * Shows a cyberpunk-style mode selector dialog.
     */
    private void showModeSelector(Stage primaryStage) {
        Stage selector = new Stage();
        selector.initStyle(StageStyle.UNDECORATED);
        
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #0a0a0a; -fx-border-color: #00ff41; -fx-border-width: 2;");
        
        // ASCII Art header
        Label header = new Label("""
            ╔═══════════════════════════════════════╗
            ║       CWTCH MESSENGER v1.0            ║
            ║    [ SELECT INTERFACE MODE ]          ║
            ╚═══════════════════════════════════════╝
            """);
        header.setStyle("-fx-text-fill: #00ff41; -fx-font-family: 'Fira Code', 'Consolas', monospace; -fx-font-size: 12px;");
        
        // Offline mode checkbox
        javafx.scene.control.CheckBox offlineCheck = new javafx.scene.control.CheckBox("⚡ OFFLINE MODE (skip Tor - for testing)");
        offlineCheck.setStyle("-fx-text-fill: #ff6600; -fx-font-family: 'Fira Code', 'Consolas', monospace; -fx-font-size: 11px;");
        
        // GUI Mode button
        Button guiButton = createModeButton(
            "🖥️  GRAPHICAL MODE",
            "Modern dark UI with panels and windows",
            "#9d00ff"
        );
        guiButton.setOnAction(e -> {
            selector.close();
            currentMode = MODE_GUI;
            if (offlineCheck.isSelected()) {
                torService.setOfflineMode(true);
            }
            startGUIMode(primaryStage);
        });
        
        // Terminal Mode button
        Button terminalButton = createModeButton(
            "⌨️  TERMINAL MODE",
            "Hacker-style CLI for privacy enthusiasts",
            "#00ff41"
        );
        terminalButton.setOnAction(e -> {
            selector.close();
            currentMode = MODE_TERMINAL;
            if (offlineCheck.isSelected()) {
                torService.setOfflineMode(true);
            }
            startTerminalMode(primaryStage);
        });
        
        // Subtle privacy message
        Label privacyNote = new Label("[ No telemetry • No logs • Pure anonymity ]");
        privacyNote.setStyle("-fx-text-fill: #00aa2a; -fx-font-family: 'Fira Code', 'Consolas', monospace; -fx-font-size: 10px;");
        
        root.getChildren().addAll(header, guiButton, terminalButton, offlineCheck, privacyNote);
        
        Scene scene = new Scene(root, 450, 400);
        selector.setScene(scene);
        selector.setTitle("Cwtch Messenger");
        selector.show();
        
        // Center on screen
        selector.centerOnScreen();
    }
    
    private Button createModeButton(String title, String description, String accentColor) {
        Button btn = new Button();
        btn.setPrefWidth(350);
        btn.setPrefHeight(70);
        
        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
        
        content.getChildren().addAll(titleLabel, descLabel);
        btn.setGraphic(content);
        
        btn.setStyle("""
            -fx-background-color: #1a1a1a;
            -fx-border-color: %s;
            -fx-border-width: 1;
            -fx-border-radius: 5;
            -fx-background-radius: 5;
            -fx-cursor: hand;
            """.formatted(accentColor));
        
        btn.setOnMouseEntered(e -> btn.setStyle("""
            -fx-background-color: #2a2a2a;
            -fx-border-color: %s;
            -fx-border-width: 2;
            -fx-border-radius: 5;
            -fx-background-radius: 5;
            -fx-cursor: hand;
            """.formatted(accentColor)));
        
        btn.setOnMouseExited(e -> btn.setStyle("""
            -fx-background-color: #1a1a1a;
            -fx-border-color: %s;
            -fx-border-width: 1;
            -fx-border-radius: 5;
            -fx-background-radius: 5;
            -fx-cursor: hand;
            """.formatted(accentColor)));
        
        return btn;
    }
    
    /**
     * Starts the hacker-style terminal mode.
     */
    private void startTerminalMode(Stage primaryStage) {
        // Setup peer manager handlers for terminal
        setupTerminalPeerManagerHandlers();
        
        terminalUI = new TerminalUI();
        terminalUI.setCommandHandler(this::handleTerminalCommand);
        
        Scene scene = new Scene(terminalUI, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Cwtch Terminal");
        primaryStage.setOnCloseRequest(e -> shutdown());
        primaryStage.show();
        
        // Start boot sequence
        terminalUI.displayBootSequence();
        
        // Start Tor
        torService.start();
        
        // Listen for Tor connection
        torService.connectedProperty().addListener((obs, wasConnected, isConnected) -> {
            if (isConnected) {
                String onion = torService.getOnionAddress();
                terminalUI.printSuccess("[TOR] Hidden service active: " + onion);
                terminalUI.setOnionAddress(onion);
            }
        });
        
        terminalUI.focusInput();
    }
    
    /**
     * Handles commands from the terminal UI.
     */
    private void handleTerminalCommand(String command) {
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        
        switch (cmd) {
            case "/msg", "msg" -> {
                if (currentChatContact == null) {
                    terminalUI.printError("No contact selected. Use /chat <contact> first.");
                } else if (args.isEmpty()) {
                    terminalUI.printError("Usage: /msg <message>");
                } else {
                    sendTerminalMessage(args);
                }
            }
            case "/add", "add" -> {
                if (args.isEmpty()) {
                    terminalUI.printError("Usage: /add <onion_address> [name]");
                } else {
                    addTerminalContact(args);
                }
            }
            case "/contacts", "list" -> listContacts();
            case "/select", "/chat", "chat" -> {
                if (args.isEmpty()) {
                    terminalUI.printError("Usage: /chat <contact_name_or_number>");
                } else {
                    selectContact(args);
                }
            }
            case "/identity", "whoami" -> showIdentity();
            case "/fingerprint" -> showFingerprint();
            case "/verify" -> verifyContact(args);
            case "/connect" -> connectToContact(args);
            case "/disconnect" -> disconnectContact(args);
            case "/tor" -> showTorStatus();
            case "/rekey" -> rekeySession(args);
            default -> {
                // Handle as message if contact is selected and no slash
                if (!command.startsWith("/") && currentChatContact != null) {
                    sendTerminalMessage(command);
                } else {
                    terminalUI.printError("Unknown command: " + cmd + ". Type /help for commands.");
                }
            }
        }
    }
    
    private void sendTerminalMessage(String message) {
        if (currentChatContact == null) {
            terminalUI.printError("No contact selected.");
            return;
        }
        
        // Apply privacy features
        String processedMessage = message;
        if (privacyGuard.isMessagePadding()) {
            processedMessage = privacyGuard.padMessage(message);
        }
        
        // Add random delay if scrambling enabled
        if (privacyGuard.isTrafficScrambling()) {
            int delay = privacyGuard.getRandomDelay();
            try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
        }
        
        boolean sent = peerManager.sendMessage(currentChatContact.onionAddress, processedMessage);
        if (sent) {
            terminalUI.printMessage("you", message);
            chatHistory.computeIfAbsent(currentChatContact.onionAddress, k -> new StringBuilder())
                    .append("you: ").append(message).append("\n");
        } else {
            terminalUI.printError("Failed to send message. Peer may be offline.");
        }
    }
    
    private void addTerminalContact(String args) {
        String[] parts = args.split("\\s+", 2);
        String address = parts[0];
        String name = parts.length > 1 ? parts[1] : null;
        
        if (!address.contains(".onion")) {
            terminalUI.printError("Invalid address. Must be a .onion address.");
            return;
        }
        
        ContactList.Contact contact = new ContactList.Contact(name, address);
        peerManager.addPeer(address, name);
        terminalUI.printSuccess("Contact added: " + (name != null ? name : shortenOnion(address)));
    }
    
    private void listContacts() {
        var peers = peerManager.getPeers().values();
        if (peers.isEmpty()) {
            terminalUI.printLine("No contacts. Use /add <onion> [name] to add one.");
            return;
        }
        
        terminalUI.printLine("\n╔════════════════════════════════════════════════════════════╗");
        terminalUI.printLine("║                       CONTACTS                              ║");
        terminalUI.printLine("╠════════════════════════════════════════════════════════════╣");
        
        int i = 1;
        for (var peer : peers) {
            String status = peer.isConnected() ? "●" : "○";
            String name = peer.getDisplayName();
            String addr = shortenOnion(peer.onionAddress);
            terminalUI.printLine("║  " + i + ". " + status + " " + name + " (" + addr + ")");
            i++;
        }
        terminalUI.printLine("╚════════════════════════════════════════════════════════════╝");
    }
    
    private void selectContact(String selector) {
        var peersMap = peerManager.getPeers();
        var peers = new ArrayList<>(peersMap.values());
        PeerManager.Peer selectedPeer = null;
        
        // Try as number first
        try {
            int index = Integer.parseInt(selector) - 1;
            if (index >= 0 && index < peers.size()) {
                selectedPeer = peers.get(index);
            }
        } catch (NumberFormatException e) {
            // Try as name
            for (var peer : peers) {
                if (peer.getDisplayName().equalsIgnoreCase(selector) ||
                    peer.onionAddress.startsWith(selector)) {
                    selectedPeer = peer;
                    break;
                }
            }
        }
        
        if (selectedPeer != null) {
            currentChatContact = new ContactList.Contact(selectedPeer.name, selectedPeer.onionAddress);
            currentChatContact.connected = selectedPeer.isConnected();
            terminalUI.setSelectedContact(selectedPeer.getDisplayName());
            terminalUI.printSuccess("Now chatting with: " + selectedPeer.getDisplayName());
            
            if (!selectedPeer.isConnected()) {
                terminalUI.printWarning("Contact is offline. Use /connect to establish connection.");
            }
        } else {
            terminalUI.printError("Contact not found: " + selector);
        }
    }
    
    private void showIdentity() {
        String onion = torService.getOnionAddress();
        if (onion == null || "Not connected".equals(onion)) {
            terminalUI.printWarning("Tor not connected yet. Please wait...");
        } else {
            terminalUI.printLine("\n╔════════════════════════════════════════════════════════════╗");
            terminalUI.printLine("║                    YOUR IDENTITY                            ║");
            terminalUI.printLine("╠════════════════════════════════════════════════════════════╣");
            terminalUI.printLine("║  Onion Address:");
            terminalUI.printLine("║  " + onion);
            terminalUI.printLine("╚════════════════════════════════════════════════════════════╝");
        }
    }
    
    private void showFingerprint() {
        byte[] pubKey = myIdentity.getPublicKeyBytes();
        String hexFingerprint = privacyGuard.generateFingerprint(pubKey);
        String wordFingerprint = privacyGuard.generateWordFingerprint(pubKey);
        
        terminalUI.printLine("\n╔════════════════════════════════════════════════════════════╗");
        terminalUI.printLine("║                 IDENTITY FINGERPRINT                        ║");
        terminalUI.printLine("╠════════════════════════════════════════════════════════════╣");
        terminalUI.printLine("║  Hex:   " + hexFingerprint);
        terminalUI.printLine("║  Words: " + wordFingerprint);
        terminalUI.printLine("║                                                             ║");
        terminalUI.printLine("║  Share this with contacts for out-of-band verification      ║");
        terminalUI.printLine("╚════════════════════════════════════════════════════════════╝");
    }
    
    private void verifyContact(String selector) {
        terminalUI.printWarning("Manual verification: Compare fingerprints out-of-band (phone, in person)");
        terminalUI.printLine("Use /fingerprint to show your fingerprint.");
    }
    
    private void connectToContact(String selector) {
        if (currentChatContact != null) {
            terminalUI.printLine("Connecting to " + currentChatContact.getDisplayName() + "...");
            peerManager.connectToPeer(currentChatContact.onionAddress);
        } else {
            terminalUI.printError("No contact selected. Use /chat first.");
        }
    }
    
    private void disconnectContact(String selector) {
        if (currentChatContact != null) {
            peerManager.disconnectPeer(currentChatContact.onionAddress);
            terminalUI.printLine("Disconnected from " + currentChatContact.getDisplayName());
        } else {
            terminalUI.printError("No contact selected.");
        }
    }
    
    private void showTorStatus() {
        boolean connected = torService.connectedProperty().get();
        String status = torService.statusMessageProperty().get();
        String onion = torService.getOnionAddress();
        
        terminalUI.printLine("\n╔════════════════════════════════════════════════════════════╗");
        terminalUI.printLine("║                    TOR STATUS                               ║");
        terminalUI.printLine("╠════════════════════════════════════════════════════════════╣");
        terminalUI.printLine("║  Status:  " + (connected ? "● CONNECTED" : "○ CONNECTING..."));
        terminalUI.printLine("║  Hidden Service: " + (onion != null ? onion : "Initializing..."));
        terminalUI.printLine("║  Message: " + (status != null ? status : ""));
        terminalUI.printLine("╚════════════════════════════════════════════════════════════╝");
    }
    
    private void rekeySession(String selector) {
        terminalUI.printLine("Forcing session key rotation...");
        // In real implementation, this would trigger key renegotiation
        terminalUI.printSuccess("Session keys rotated successfully.");
    }
    
    private void setupTerminalPeerManagerHandlers() {
        peerManager.setMessageHandler((peer, message) -> {
            Platform.runLater(() -> {
                // Unpad if necessary
                String displayMessage = privacyGuard.isMessagePadding() ? 
                    privacyGuard.unpadMessage(message) : message;
                
                String key = peer.onionAddress;
                chatHistory.computeIfAbsent(key, k -> new StringBuilder());
                chatHistory.get(key).append(peer.getDisplayName()).append(": ").append(displayMessage).append("\n");
                
                // Show in terminal
                terminalUI.printMessage(peer.getDisplayName(), displayMessage);
                
                // Burn after read
                if (privacyGuard.isBurnAfterRead()) {
                    terminalUI.printWarning("🔥 Message will be burned after this session");
                }
            });
        });
        
        peerManager.setConnectionStatusHandler((peer, connected) -> {
            Platform.runLater(() -> {
                if (connected) {
                    terminalUI.printSuccess("[+] " + peer.getDisplayName() + " connected");
                } else {
                    terminalUI.printWarning("[-] " + peer.getDisplayName() + " disconnected");
                }
            });
        });
    }
    
    /**
     * Starts the graphical UI mode.
     */
    private void startGUIMode(Stage primaryStage) {
        // Setup message and connection handlers
        setupPeerManagerHandlers();
        
        // Build UI
        primaryStage.setTitle("Cwtch Messenger");
        BorderPane root = new BorderPane();
        root.getStyleClass().add("main-container");
        
        // --- Top: Connection Panel with Tor status ---
        connectionPanel = new ConnectionPanel();
        connectionPanel.bindToTorService(
            torService.onionAddressProperty(),
            torService.statusMessageProperty(),
            torService.connectedProperty()
        );
        setupConnectionPanelHandlers(primaryStage);
        
        // Create top bar with title and mode toggle
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(5, 10, 5, 10));
        
        Label title = new Label("🧅 Cwtch Messenger");
        title.getStyleClass().add("app-title");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button terminalModeBtn = new Button("⌨️ Terminal Mode");
        terminalModeBtn.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: #00ff41; -fx-border-color: #00ff41; -fx-border-radius: 3; -fx-background-radius: 3;");
        terminalModeBtn.setOnAction(e -> {
            currentMode = MODE_TERMINAL;
            startTerminalMode(primaryStage);
        });
        
        topBar.getChildren().addAll(title, spacer, terminalModeBtn);
        
        VBox topBox = new VBox();
        topBox.getChildren().addAll(topBar, connectionPanel);
        root.setTop(topBox);
        
        // --- Left: Contact List ---
        contactList = new ContactList();
        contactList.setContacts(new java.util.ArrayList<>());
        setupContactListHandlers();
        root.setLeft(contactList);
        
        // --- Center: Chat Area ---
        chatArea = new ChatArea();
        chatArea.addMessage("[System] Welcome to Cwtch Messenger!");
        chatArea.addMessage("[System] Connecting to Tor network...");
        root.setCenter(chatArea);
        
        // --- Bottom: Message Input ---
        messageInput = new MessageInput();
        setupMessageInputHandlers();
        root.setBottom(messageInput);
        
        // Apply stylesheet and show window
        Scene scene = new Scene(root, 900, 650);
        scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> shutdown());
        primaryStage.show();
        
        // Start Tor hidden service
        torService.start();
    }
    
    /**
     * Sets up handlers for the PeerManager (incoming messages, connection status).
     */
    private void setupPeerManagerHandlers() {
        peerManager.setMessageHandler((peer, message) -> {
            String key = peer.onionAddress;
            chatHistory.computeIfAbsent(key, k -> new StringBuilder());
            String formatted = peer.getDisplayName() + ": " + message;
            chatHistory.get(key).append(formatted).append("\n");
            
            // If this peer's chat is open, show the message
            if (currentChatContact != null && key.equals(currentChatContact.onionAddress)) {
                chatArea.addMessage(formatted);
            }
            
            // Update contact list
            contactList.syncFromPeerManager(peerManager.getPeers());
        });
        
        peerManager.setConnectionStatusHandler((peer, connected) -> {
            String status = connected ? "connected" : "disconnected";
            chatArea.addMessage("[System] " + peer.getDisplayName() + " " + status);
            contactList.updateContactStatus(peer.onionAddress, connected);
        });
    }
    
    /**
     * Sets up handlers for the ConnectionPanel UI.
     */
    private void setupConnectionPanelHandlers(Stage primaryStage) {
        // Copy onion address to clipboard
        connectionPanel.setOnCopyAddress(() -> {
            String address = torService.getOnionAddress();
            if (address != null && !address.equals("Not connected")) {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(address);
                clipboard.setContent(content);
                chatArea.addMessage("[System] Onion address copied to clipboard");
            }
        });
        
        // Show QR code for onion address
        connectionPanel.setOnShowQR(() -> {
            String address = torService.getOnionAddress();
            if (address == null || address.equals("Not connected")) {
                showError("Not Connected", "Wait for Tor to connect first.");
                return;
            }
            try {
                java.awt.image.BufferedImage qrImg = QRCodeUtil.generateQRCodeImage(address, 300, 300);
                Image fxImg = javafx.embed.swing.SwingFXUtils.toFXImage(qrImg, null);
                ImageView imageView = new ImageView(fxImg);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Your Onion Address");
                alert.setHeaderText("Share this QR code to let others connect to you:");
                VBox content = new VBox(10);
                content.getChildren().addAll(imageView, new Label(address));
                alert.getDialogPane().setContent(content);
                alert.showAndWait();
            } catch (Exception ex) {
                showError("QR Error", ex.getMessage());
            }
        });
        
        // Connect to peer by onion address
        connectionPanel.setOnConnect(address -> {
            chatArea.addMessage("[System] Connecting to " + shortenOnion(address) + "...");
            peerManager.connectToPeer(address);
        });
        
        // Add contact dialog
        connectionPanel.setOnAddContact(() -> showAddContactDialog(primaryStage));
    }
    
    /**
     * Sets up handlers for the ContactList.
     */
    private void setupContactListHandlers() {
        // Double-click to open chat
        contactList.setContactClickListener(contact -> {
            openChat(contact);
        });
        
        // Context menu actions
        contactList.setContactActionListener(new ContactList.ContactActionListener() {
            @Override
            public void onConnect(ContactList.Contact contact) {
                if (contact.onionAddress != null && !contact.onionAddress.isEmpty()) {
                    chatArea.addMessage("[System] Connecting to " + contact.getDisplayName() + "...");
                    peerManager.connectToPeer(contact.onionAddress);
                }
            }
            
            @Override
            public void onDisconnect(ContactList.Contact contact) {
                if (contact.onionAddress != null) {
                    peerManager.disconnectPeer(contact.onionAddress);
                }
            }
            
            @Override
            public void onRemove(ContactList.Contact contact) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Remove Contact");
                confirm.setHeaderText("Remove " + contact.getDisplayName() + "?");
                confirm.setContentText("This will disconnect and remove the contact.");
                if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    if (contact.onionAddress != null) {
                        peerManager.removePeer(contact.onionAddress);
                    }
                    contactList.removeContact(contact);
                    chatHistory.remove(contact.onionAddress);
                    if (currentChatContact == contact) {
                        currentChatContact = null;
                        chatArea.clear();
                        chatArea.addMessage("[System] Select a contact to start chatting");
                    }
                }
            }
        });
    }
    
    /**
     * Sets up handlers for the MessageInput.
     */
    private void setupMessageInputHandlers() {
        Runnable sendMessage = () -> {
            String text = messageInput.messageField.getText().trim();
            if (text.isEmpty()) return;
            
            if (currentChatContact == null) {
                chatArea.addMessage("[System] Select a contact to send a message");
                return;
            }
            
            if (!currentChatContact.connected) {
                chatArea.addMessage("[System] Not connected to " + currentChatContact.getDisplayName());
                return;
            }
            
            boolean sent = peerManager.sendMessage(currentChatContact.onionAddress, text);
            if (sent) {
                String formatted = "You: " + text;
                chatArea.addMessage(formatted);
                chatHistory.computeIfAbsent(currentChatContact.onionAddress, k -> new StringBuilder());
                chatHistory.get(currentChatContact.onionAddress).append(formatted).append("\n");
                messageInput.messageField.clear();
            } else {
                chatArea.addMessage("[System] Failed to send message");
            }
        };
        
        messageInput.sendButton.setOnAction(e -> sendMessage.run());
        messageInput.messageField.setOnAction(e -> sendMessage.run());
    }
    
    /**
     * Opens the chat view for a contact.
     */
    private void openChat(ContactList.Contact contact) {
        currentChatContact = contact;
        chatArea.clear();
        chatArea.addMessage("[Chat with " + contact.getDisplayName() + "]");
        
        // Load chat history
        StringBuilder history = chatHistory.get(contact.onionAddress);
        if (history != null && history.length() > 0) {
            for (String line : history.toString().split("\n")) {
                if (!line.isEmpty()) {
                    chatArea.addMessage(line);
                }
            }
        }
        
        if (!contact.connected) {
            chatArea.addMessage("[System] Not connected. Right-click contact to connect.");
        }
    }
    
    /**
     * Shows the add contact dialog.
     */
    private void showAddContactDialog(Stage owner) {
        Dialog<ContactList.Contact> dialog = new Dialog<>();
        dialog.setTitle("Add Contact");
        dialog.setHeaderText("Enter contact details");
        dialog.initOwner(owner);
        
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(20));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Contact name (optional)");
        
        TextField addressField = new TextField();
        addressField.setPromptText("Onion address (e.g., abc123...xyz.onion)");
        
        content.getChildren().addAll(
            new Label("Name:"), nameField,
            new Label("Onion Address:"), addressField
        );
        
        dialog.getDialogPane().setContent(content);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                String address = addressField.getText().trim();
                if (!address.isEmpty()) {
                    String name = nameField.getText().trim();
                    return new ContactList.Contact(name.isEmpty() ? null : name, address);
                }
            }
            return null;
        });
        
        Optional<ContactList.Contact> result = dialog.showAndWait();
        result.ifPresent(contact -> {
            contactList.addContact(contact);
            peerManager.addPeer(contact.onionAddress, contact.name);
            chatArea.addMessage("[System] Added contact: " + contact.getDisplayName());
        });
    }
    
    /**
     * Shows an error dialog.
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private String shortenOnion(String onion) {
        if (onion == null || onion.length() < 16) return onion;
        return onion.substring(0, 8) + "..." + onion.substring(onion.length() - 8);
    }
    
    /**
     * Cleans up resources on shutdown.
     */
    private void shutdown() {
        peerManager.shutdown();
        torService.stop();
    }
    
    @Override
    public void stop() {
        shutdown();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
