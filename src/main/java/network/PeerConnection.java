package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * PeerConnection handles both server (host) and client (connect) roles for a peer-to-peer chat.
 * It uses Java Sockets to send and receive messages.
 */
public class PeerConnection {
    private Socket socket; // The socket for communication
    private BufferedReader in; // For reading messages
    private PrintWriter out; // For sending messages
    private Thread listenerThread; // Thread for listening to incoming messages
    private MessageListener messageListener; // Callback for received messages
    // For encrypted messaging
    private javax.crypto.SecretKey aesKey;
    private java.security.PublicKey myPublicKey;
    private java.security.PrivateKey myPrivateKey;
    private java.security.PublicKey peerPublicKey;
    private boolean handshakeComplete = false;

    /**
     * Interface for message received callback.
     */
    public interface MessageListener {
        void onMessageReceived(String message);
    }

    /**
     * Start as host (server): listens for a connection on the given port.
     */
    public void startAsHost(int port, MessageListener listener) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            this.socket = serverSocket.accept(); // Wait for a client to connect
            setupStreams(listener);
            // Only accept one connection for now
        } // Wait for a client to connect
    }

    /**
     * Start as client: connects to a host at the given address and port.
     */
    public void startAsClient(String host, int port, MessageListener listener) throws IOException {
        this.socket = new Socket(host, port);
        setupStreams(listener);
    }

    /**
     * Set the AES key for encrypting/decrypting messages on this connection.
     */
    public void setAESKey(javax.crypto.SecretKey key) {
        this.aesKey = key;
    }

    /**
     * Set up input/output streams and start listening for messages.
     */
    private void setupStreams(MessageListener listener) throws IOException {
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.messageListener = listener;
        // Start a thread to listen for incoming messages
        listenerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (aesKey != null) {
                        try {
                            String decrypted = crypto.SimpleCrypto.decryptAES(line, aesKey);
                            messageListener.onMessageReceived(decrypted);
                        } catch (Exception e) {
                            // If decryption fails, show raw
                            messageListener.onMessageReceived("[Encrypted/Corrupt] " + line);
                        }
                    } else {
                        messageListener.onMessageReceived(line);
                    }
                }
            } catch (IOException e) {
                // Connection closed or error
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Send a message to the peer.
     */
    public void sendMessage(String message) {
        if (out != null) {
            if (aesKey != null) {
                try {
                    String encrypted = crypto.SimpleCrypto.encryptAES(message, aesKey);
                    out.println(encrypted);
                } catch (Exception e) {
                    // Fallback to plaintext if encryption fails
                    out.println(message);
                }
            } else {
                out.println(message);
            }
        }
    }

    /**
     * Close the connection and stop listening.
     */
    public void close() throws IOException {
        if (listenerThread != null) listenerThread.interrupt();
        if (in != null) in.close();
        if (out != null) out.close();
        if (socket != null) socket.close();
    }

    // --- Protocol handshake (call after socket connect) ---
    public void doHandshakeAsHost(java.security.PublicKey myPub, java.security.PrivateKey myPriv, MessageListener listener) throws Exception {
        this.myPublicKey = myPub;
        this.myPrivateKey = myPriv;
        this.messageListener = listener;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        // 1. Receive handshake from client (client sends their public key)
        String handshakeJson = in.readLine();
        ProtocolMessage handshakeMsg = ProtocolMessage.fromJson(handshakeJson);
        if (!"handshake".equals(handshakeMsg.type)) throw new IOException("Expected handshake");
        byte[] peerPubBytes = java.util.Base64.getDecoder().decode(handshakeMsg.payload.get("publicKey"));
        java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(peerPubBytes);
        this.peerPublicKey = java.security.KeyFactory.getInstance("RSA").generatePublic(keySpec);
        // 2. Generate AES session key
        javax.crypto.SecretKey sessionKey = crypto.SimpleCrypto.generateAESKey();
        this.aesKey = sessionKey;
        // 3. Encrypt AES key with peer's public key
        String encSessionKey = crypto.SimpleCrypto.encryptRSA(crypto.SimpleCrypto.encodeAESKey(sessionKey), peerPublicKey);
        // 4. Send handshake response (with encrypted session key and our public key)
        ProtocolMessage response = new ProtocolMessage("handshake_ack");
        response.payload.put("publicKey", java.util.Base64.getEncoder().encodeToString(myPublicKey.getEncoded()));
        response.payload.put("encSessionKey", encSessionKey);
        out.println(response.toJson());
        this.handshakeComplete = true;
        // Start listening for messages
        startProtocolListener();
    }

    public void doHandshakeAsClient(java.security.PublicKey myPub, java.security.PrivateKey myPriv, java.security.PublicKey peerPub, MessageListener listener) throws Exception {
        this.myPublicKey = myPub;
        this.myPrivateKey = myPriv;
        this.peerPublicKey = peerPub;
        this.messageListener = listener;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        // 1. Send handshake (with our public key)
        ProtocolMessage handshake = new ProtocolMessage("handshake");
        handshake.payload.put("publicKey", java.util.Base64.getEncoder().encodeToString(myPublicKey.getEncoded()));
        out.println(handshake.toJson());
        // 2. Receive handshake_ack (with peer's public key and encrypted session key)
        String ackJson = in.readLine();
        ProtocolMessage ackMsg = ProtocolMessage.fromJson(ackJson);
        if (!"handshake_ack".equals(ackMsg.type)) throw new IOException("Expected handshake_ack");
        byte[] peerPubBytes = java.util.Base64.getDecoder().decode(ackMsg.payload.get("publicKey"));
        java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(peerPubBytes);
        this.peerPublicKey = java.security.KeyFactory.getInstance("RSA").generatePublic(keySpec);
        // 3. Decrypt AES session key
        String encSessionKey = ackMsg.payload.get("encSessionKey");
        String sessionKeyBase64 = crypto.SimpleCrypto.decryptRSA(encSessionKey, myPrivateKey);
        javax.crypto.SecretKey sessionKey = crypto.SimpleCrypto.decodeAESKey(sessionKeyBase64);
        this.aesKey = sessionKey;
        this.handshakeComplete = true;
        // Start listening for messages
        startProtocolListener();
    }

    // --- Protocol message listener (after handshake) ---
    private void startProtocolListener() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (aesKey != null) {
                        try {
                            String decrypted = crypto.SimpleCrypto.decryptAES(line, aesKey);
                            ProtocolMessage msg = ProtocolMessage.fromJson(decrypted);
                            if ("chat".equals(msg.type)) {
                                messageListener.onMessageReceived(msg.payload.get("text"));
                            } else {
                                // Handle other types as needed
                                messageListener.onMessageReceived("[Protocol] " + msg.type);
                            }
                        } catch (Exception e) {
                            messageListener.onMessageReceived("[Encrypted/Corrupt] " + line);
                        }
                    } else {
                        messageListener.onMessageReceived(line);
                    }
                }
            } catch (IOException e) {
                // Connection closed or error
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    // --- Send protocol chat message (after handshake) ---
    public void sendChatMessage(String text, String sender, String recipient) {
        if (!handshakeComplete || aesKey == null) {
            throw new IllegalStateException("Handshake not complete");
        }
        ProtocolMessage msg = new ProtocolMessage("chat");
        msg.sender = sender;
        msg.recipient = recipient;
        msg.payload.put("text", text);
        try {
            String json = msg.toJson();
            String encrypted = crypto.SimpleCrypto.encryptAES(json, aesKey);
            out.println(encrypted);
        } catch (Exception e) {
            // Fallback: do nothing or log
        }
    }
}
    // Deprecated: replaced by protocol.PeerChannel
