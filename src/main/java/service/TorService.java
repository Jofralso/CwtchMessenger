package service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import protocol.Identity;
import protocol.TorManager;

/**
 * TorService manages the Tor hidden service lifecycle and incoming connections.
 * Provides a Ricochet-style anonymous addressing system using onion addresses.
 */
public class TorService {
    private static final int DEFAULT_HIDDEN_SERVICE_PORT = 9878;
    
    private final TorManager torManager;
    private final Identity identity;
    private final ExecutorService executor;
    
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private boolean offlineMode = false;
    
    // Observable properties for UI binding
    private final StringProperty onionAddress = new SimpleStringProperty("Not connected");
    private final StringProperty statusMessage = new SimpleStringProperty("Initializing...");
    private final BooleanProperty connected = new SimpleBooleanProperty(false);
    private final DoubleProperty connectionProgress = new SimpleDoubleProperty(0);
    
    private Consumer<Socket> incomingConnectionHandler;
    
    public TorService(Identity identity) {
        this.identity = identity;
        this.torManager = new TorManager();
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        
        // Set up progress callbacks
        torManager.setStatusCallback(status -> 
            Platform.runLater(() -> statusMessage.set(status)));
        torManager.setProgressCallback(percent -> 
            Platform.runLater(() -> connectionProgress.set(percent / 100.0)));
    }
    
    /**
     * Enable offline/demo mode - generates a fake onion address for testing.
     */
    public void setOfflineMode(boolean offline) {
        this.offlineMode = offline;
    }
    
    public boolean isOfflineMode() {
        return offlineMode;
    }
    
    /**
     * Starts the Tor hidden service and begins accepting connections.
     */
    public void start() {
        executor.submit(() -> {
            try {
                if (offlineMode) {
                    startOfflineMode();
                    return;
                }
                
                updateStatus("Starting Tor hidden service...");
                updateProgress(0.05);
                
                // Start local server socket first
                serverSocket = new ServerSocket(DEFAULT_HIDDEN_SERVICE_PORT);
                
                // Start Tor hidden service (with progress callbacks)
                String onion = torManager.startHiddenService(DEFAULT_HIDDEN_SERVICE_PORT);
                
                Platform.runLater(() -> {
                    onionAddress.set(onion);
                    connected.set(true);
                    connectionProgress.set(1.0);
                    statusMessage.set("Connected - " + shortenOnion(onion));
                });
                
                running = true;
                acceptConnections();
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusMessage.set("Tor error: " + e.getMessage());
                    connected.set(false);
                    connectionProgress.set(0);
                });
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Starts in offline/demo mode with a simulated onion address.
     */
    private void startOfflineMode() throws IOException {
        updateStatus("Starting in OFFLINE mode...");
        updateProgress(0.3);
        
        // Generate a fake but realistic-looking onion address
        String fakeOnion = generateFakeOnion();
        
        // Simulate startup delay
        try {
            Thread.sleep(500);
            updateStatus("Generating identity...");
            updateProgress(0.6);
            Thread.sleep(500);
            updateStatus("Setting up local server...");
            updateProgress(0.8);
            
            serverSocket = new ServerSocket(DEFAULT_HIDDEN_SERVICE_PORT);
            
            Thread.sleep(300);
        } catch (InterruptedException ignored) {}
        
        Platform.runLater(() -> {
            onionAddress.set(fakeOnion + " [OFFLINE]");
            connected.set(true);
            connectionProgress.set(1.0);
            statusMessage.set("OFFLINE MODE - " + shortenOnion(fakeOnion));
        });
        
        running = true;
        acceptConnections();
    }
    
    /**
     * Generates a fake v3 onion address for demo purposes.
     */
    private String generateFakeOnion() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyz234567";
        for (int i = 0; i < 56; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString() + ".onion";
    }
    
    /**
     * Cancels the current connection attempt.
     */
    public void cancel() {
        torManager.cancel();
        running = false;
        Platform.runLater(() -> {
            statusMessage.set("Connection cancelled");
            connected.set(false);
            connectionProgress.set(0);
        });
    }
    
    /**
     * Accepts incoming peer connections in a loop.
     */
    private void acceptConnections() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket incoming = serverSocket.accept();
                if (incomingConnectionHandler != null) {
                    executor.submit(() -> incomingConnectionHandler.accept(incoming));
                }
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Connects to a peer's onion address.
     */
    public Socket connectToPeer(String peerOnionAddress) throws IOException {
        updateStatus("Connecting to " + shortenOnion(peerOnionAddress) + "...");
        
        // Connect through Tor SOCKS proxy (default port 9050)
        java.net.Proxy torProxy = new java.net.Proxy(
            java.net.Proxy.Type.SOCKS,
            new java.net.InetSocketAddress("127.0.0.1", 9050)
        );
        
        Socket socket = new Socket(torProxy);
        socket.connect(new java.net.InetSocketAddress(peerOnionAddress, DEFAULT_HIDDEN_SERVICE_PORT), 60000);
        
        updateStatus("Connected to " + shortenOnion(peerOnionAddress));
        return socket;
    }
    
    /**
     * Stops the Tor service and closes all connections.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        torManager.stop();
        executor.shutdownNow();
        
        Platform.runLater(() -> {
            connected.set(false);
            statusMessage.set("Disconnected");
            onionAddress.set("Not connected");
        });
    }
    
    /**
     * Sets the handler for incoming peer connections.
     */
    public void setIncomingConnectionHandler(Consumer<Socket> handler) {
        this.incomingConnectionHandler = handler;
    }
    
    private void updateStatus(String status) {
        Platform.runLater(() -> statusMessage.set(status));
    }
    
    private void updateProgress(double progress) {
        Platform.runLater(() -> connectionProgress.set(progress));
    }
    
    private String shortenOnion(String onion) {
        if (onion == null || onion.length() < 16) return onion;
        return onion.substring(0, 8) + "..." + onion.substring(onion.length() - 8);
    }
    
    // Property getters for JavaFX binding
    public StringProperty onionAddressProperty() { return onionAddress; }
    public StringProperty statusMessageProperty() { return statusMessage; }
    public BooleanProperty connectedProperty() { return connected; }
    public DoubleProperty connectionProgressProperty() { return connectionProgress; }
    
    public String getOnionAddress() { return onionAddress.get(); }
    public Identity getIdentity() { return identity; }
}
