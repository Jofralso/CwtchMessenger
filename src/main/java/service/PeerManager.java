package service;

import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import javax.crypto.SecretKey;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import protocol.Handshake;
import protocol.Identity;
import protocol.PeerChannel;
import protocol.SessionCrypto;

/**
 * PeerManager handles peer connections, handshakes, and message routing.
 * Manages the lifecycle of encrypted peer-to-peer channels.
 */
public class PeerManager {
    
    public static class Peer {
        public final String onionAddress;
        public final String name;
        public PeerChannel channel;
        public volatile boolean connected;
        public volatile long lastSeen;
        
        public Peer(String onionAddress, String name) {
            this.onionAddress = onionAddress;
            this.name = name;
            this.connected = false;
            this.lastSeen = 0;
        }
        
        public String getDisplayName() {
            return name != null && !name.isEmpty() ? name : shortenOnion(onionAddress);
        }
        
        public boolean isConnected() {
            return connected;
        }
        
        private static String shortenOnion(String onion) {
            if (onion == null || onion.length() < 16) return onion;
            return onion.substring(0, 8) + "...";
        }
    }
    
    private final TorService torService;
    private final Identity identity;
    private final Map<String, Peer> peers = new ConcurrentHashMap<>();
    private final ObservableMap<String, Peer> observablePeers = FXCollections.observableMap(peers);
    private final ExecutorService executor;
    
    // Message handler: (peer, message) -> void
    private BiConsumer<Peer, String> messageHandler;
    private BiConsumer<Peer, Boolean> connectionStatusHandler;
    
    public PeerManager(TorService torService, Identity identity) {
        this.torService = torService;
        this.identity = identity;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        
        // Handle incoming connections
        torService.setIncomingConnectionHandler(this::handleIncomingConnection);
    }
    
    /**
     * Adds a peer contact and optionally initiates connection.
     */
    public Peer addPeer(String onionAddress, String name) {
        String normalizedAddress = normalizeOnionAddress(onionAddress);
        Peer peer = new Peer(normalizedAddress, name);
        peers.put(normalizedAddress, peer);
        return peer;
    }
    
    /**
     * Connects to a peer by onion address.
     */
    public void connectToPeer(String onionAddress) {
        String normalizedAddress = normalizeOnionAddress(onionAddress);
        Peer peer = peers.get(normalizedAddress);
        if (peer == null) {
            peer = addPeer(normalizedAddress, null);
        }
        
        final Peer finalPeer = peer;
        executor.submit(() -> {
            try {
                Socket socket = torService.connectToPeer(normalizedAddress);
                performHandshakeInitiator(finalPeer, socket);
            } catch (Exception e) {
                notifyConnectionStatus(finalPeer, false);
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Handles an incoming connection from a peer.
     */
    private void handleIncomingConnection(Socket socket) {
        try {
            performHandshakeResponder(socket);
        } catch (Exception e) {
            e.printStackTrace();
            try { socket.close(); } catch (Exception ignored) {}
        }
    }
    
    /**
     * Performs handshake as the initiator (we connected to them).
     */
    private void performHandshakeInitiator(Peer peer, Socket socket) throws Exception {
        // Generate ephemeral key pair for ECDH
        KeyPair ephemeralKP = Handshake.generateEphemeralKeyPair();
        
        // Send our ephemeral public key
        java.io.PrintWriter out = new java.io.PrintWriter(socket.getOutputStream(), true);
        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
        
        out.println("CWTCH_HELLO:" + Handshake.encodePublicKey(ephemeralKP.getPublic()) + 
                    ":" + identity.getPublicKeyBase64());
        
        // Receive peer's ephemeral public key
        String response = in.readLine();
        if (response == null || !response.startsWith("CWTCH_HELLO:")) {
            throw new Exception("Invalid handshake response");
        }
        
        String[] parts = response.split(":");
        PublicKey peerEphemeralKey = Handshake.decodePublicKey(parts[1]);
        
        // Compute shared secret and derive session key
        byte[] sharedSecret = Handshake.computeSharedSecret(ephemeralKP.getPrivate(), peerEphemeralKey);
        SecretKey sessionKey = SessionCrypto.deriveSessionKey(
            sharedSecret,
            "cwtch-session".getBytes(),
            "handshake-salt".getBytes()
        );
        
        // Create encrypted channel
        peer.channel = new PeerChannel(socket, sessionKey);
        peer.connected = true;
        peer.lastSeen = System.currentTimeMillis();
        
        notifyConnectionStatus(peer, true);
        
        // Start receiving messages
        startMessageReceiver(peer);
    }
    
    /**
     * Performs handshake as the responder (they connected to us).
     */
    private void performHandshakeResponder(Socket socket) throws Exception {
        java.io.PrintWriter out = new java.io.PrintWriter(socket.getOutputStream(), true);
        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
        
        // Receive initiator's hello
        String hello = in.readLine();
        if (hello == null || !hello.startsWith("CWTCH_HELLO:")) {
            throw new Exception("Invalid handshake initiation");
        }
        
        String[] parts = hello.split(":");
        PublicKey peerEphemeralKey = Handshake.decodePublicKey(parts[1]);
        String peerIdentity = parts.length > 2 ? parts[2] : null;
        
        // Generate our ephemeral key pair
        KeyPair ephemeralKP = Handshake.generateEphemeralKeyPair();
        
        // Send our response
        out.println("CWTCH_HELLO:" + Handshake.encodePublicKey(ephemeralKP.getPublic()) +
                    ":" + identity.getPublicKeyBase64());
        
        // Compute shared secret and derive session key
        byte[] sharedSecret = Handshake.computeSharedSecret(ephemeralKP.getPrivate(), peerEphemeralKey);
        SecretKey sessionKey = SessionCrypto.deriveSessionKey(
            sharedSecret,
            "cwtch-session".getBytes(),
            "handshake-salt".getBytes()
        );
        
        // Determine peer's onion address from connection (or use identity)
        String peerAddress = socket.getInetAddress().getHostName();
        Peer peer = peers.get(peerAddress);
        if (peer == null) {
            peer = addPeer(peerAddress, null);
        }
        
        // Create encrypted channel
        peer.channel = new PeerChannel(socket, sessionKey);
        peer.connected = true;
        peer.lastSeen = System.currentTimeMillis();
        
        notifyConnectionStatus(peer, true);
        
        // Start receiving messages
        startMessageReceiver(peer);
    }
    
    /**
     * Starts a background thread to receive messages from a peer.
     */
    private void startMessageReceiver(Peer peer) {
        executor.submit(() -> {
            while (peer.connected && peer.channel != null) {
                try {
                    var msg = peer.channel.receive();
                    if (msg == null) {
                        peer.connected = false;
                        notifyConnectionStatus(peer, false);
                        break;
                    }
                    
                    peer.lastSeen = System.currentTimeMillis();
                    
                    if ("MSG".equals(msg.getType()) && messageHandler != null) {
                        Platform.runLater(() -> messageHandler.accept(peer, msg.getPayload()));
                    }
                } catch (Exception e) {
                    peer.connected = false;
                    notifyConnectionStatus(peer, false);
                    break;
                }
            }
        });
    }
    
    /**
     * Sends a message to a peer.
     */
    public boolean sendMessage(Peer peer, String message) {
        if (peer == null || !peer.connected || peer.channel == null) {
            return false;
        }
        
        try {
            peer.channel.send("MSG", message);
            return true;
        } catch (Exception e) {
            peer.connected = false;
            notifyConnectionStatus(peer, false);
            return false;
        }
    }
    
    /**
     * Sends a message to a peer by onion address.
     */
    public boolean sendMessage(String onionAddress, String message) {
        Peer peer = peers.get(normalizeOnionAddress(onionAddress));
        return sendMessage(peer, message);
    }
    
    /**
     * Disconnects from a peer.
     */
    public void disconnectPeer(String onionAddress) {
        Peer peer = peers.get(normalizeOnionAddress(onionAddress));
        if (peer != null && peer.channel != null) {
            try {
                peer.channel.close();
            } catch (Exception ignored) {}
            peer.connected = false;
            peer.channel = null;
            notifyConnectionStatus(peer, false);
        }
    }
    
    /**
     * Removes a peer from the contact list.
     */
    public void removePeer(String onionAddress) {
        String normalized = normalizeOnionAddress(onionAddress);
        disconnectPeer(normalized);
        peers.remove(normalized);
    }
    
    private String normalizeOnionAddress(String address) {
        if (address == null) return "";
        address = address.trim().toLowerCase();
        if (!address.endsWith(".onion")) {
            address += ".onion";
        }
        return address;
    }
    
    private void notifyConnectionStatus(Peer peer, boolean connected) {
        if (connectionStatusHandler != null) {
            Platform.runLater(() -> connectionStatusHandler.accept(peer, connected));
        }
    }
    
    // Setters for handlers
    public void setMessageHandler(BiConsumer<Peer, String> handler) {
        this.messageHandler = handler;
    }
    
    public void setConnectionStatusHandler(BiConsumer<Peer, Boolean> handler) {
        this.connectionStatusHandler = handler;
    }
    
    public Map<String, Peer> getPeers() {
        return peers;
    }
    
    public Peer getPeer(String onionAddress) {
        return peers.get(normalizeOnionAddress(onionAddress));
    }
    
    public void shutdown() {
        for (Peer peer : peers.values()) {
            if (peer.channel != null) {
                try { peer.channel.close(); } catch (Exception ignored) {}
            }
        }
        executor.shutdownNow();
    }
}
