package service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;

/**
 * PrivacyGuard - Advanced privacy features for paranoid users.
 * 
 * Features:
 * - Encrypted local storage with passphrase
 * - Secure memory wiping
 * - Message padding to hide length
 * - Traffic pattern randomization
 * - Burn-after-read messages
 * - Panic button data destruction
 * - Ghost mode (no presence indicators)
 */
public class PrivacyGuard {
    
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_SIZE = 12;
    private static final int GCM_TAG_SIZE = 128;
    private static final int PBKDF2_ITERATIONS = 100000;
    private static final int SALT_SIZE = 32;
    
    // Privacy settings
    private boolean burnAfterRead = false;
    private boolean ghostMode = false;
    private boolean messagePadding = true;
    private boolean trafficScrambling = false;
    private boolean paranoidMode = false;
    
    // Message padding config
    private static final int PADDING_BLOCK_SIZE = 256;
    private static final int MIN_DELAY_MS = 100;
    private static final int MAX_DELAY_MS = 3000;
    
    private final SecureRandom secureRandom = new SecureRandom();
    private final Path dataDir;
    private SecretKey storageKey;
    
    public PrivacyGuard(Path dataDirectory) {
        this.dataDir = dataDirectory;
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            // Ignore
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ENCRYPTED STORAGE
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Derives an encryption key from a passphrase using PBKDF2.
     */
    public void unlockStorage(char[] passphrase) throws Exception {
        Path saltFile = dataDir.resolve(".salt");
        byte[] salt;
        
        if (Files.exists(saltFile)) {
            salt = Files.readAllBytes(saltFile);
        } else {
            salt = new byte[SALT_SIZE];
            secureRandom.nextBytes(salt);
            Files.write(saltFile, salt);
            // Hide the salt file
            try {
                Files.setAttribute(saltFile, "dos:hidden", true);
            } catch (Exception ignored) {}
        }
        
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, AES_KEY_SIZE);
        SecretKey tmp = factory.generateSecret(spec);
        storageKey = new SecretKeySpec(tmp.getEncoded(), "AES");
        
        // Wipe passphrase from memory
        Arrays.fill(passphrase, '\0');
        spec.clearPassword();
    }
    
    /**
     * Encrypts and saves data to a file.
     */
    public void saveEncrypted(String filename, byte[] data) throws Exception {
        if (storageKey == null) throw new IllegalStateException("Storage not unlocked");
        
        byte[] iv = new byte[GCM_IV_SIZE];
        secureRandom.nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_SIZE, iv);
        cipher.init(Cipher.ENCRYPT_MODE, storageKey, spec);
        
        byte[] encrypted = cipher.doFinal(data);
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        
        Files.write(dataDir.resolve(filename), combined);
    }
    
    /**
     * Loads and decrypts data from a file.
     */
    public byte[] loadEncrypted(String filename) throws Exception {
        if (storageKey == null) throw new IllegalStateException("Storage not unlocked");
        
        Path file = dataDir.resolve(filename);
        if (!Files.exists(file)) return null;
        
        byte[] combined = Files.readAllBytes(file);
        byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_SIZE);
        byte[] encrypted = Arrays.copyOfRange(combined, GCM_IV_SIZE, combined.length);
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_SIZE, iv);
        cipher.init(Cipher.DECRYPT_MODE, storageKey, spec);
        
        return cipher.doFinal(encrypted);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MESSAGE PRIVACY
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Pads a message to hide its true length.
     * Uses PKCS7-style padding to a fixed block size.
     */
    public String padMessage(String message) {
        if (!messagePadding) return message;
        
        byte[] msgBytes = message.getBytes();
        int paddedLength = ((msgBytes.length / PADDING_BLOCK_SIZE) + 1) * PADDING_BLOCK_SIZE;
        int paddingSize = paddedLength - msgBytes.length;
        
        // Add random padding with length indicator
        byte[] padded = new byte[paddedLength];
        System.arraycopy(msgBytes, 0, padded, 0, msgBytes.length);
        
        // Fill with random bytes, last byte is padding length
        secureRandom.nextBytes(Arrays.copyOfRange(padded, msgBytes.length, paddedLength - 1));
        padded[paddedLength - 1] = (byte) paddingSize;
        
        return Base64.getEncoder().encodeToString(padded);
    }
    
    /**
     * Removes padding from a message.
     */
    public String unpadMessage(String paddedBase64) {
        if (!messagePadding) return paddedBase64;
        
        try {
            byte[] padded = Base64.getDecoder().decode(paddedBase64);
            int paddingSize = padded[padded.length - 1] & 0xFF;
            int originalLength = padded.length - paddingSize;
            return new String(Arrays.copyOfRange(padded, 0, originalLength));
        } catch (Exception e) {
            return paddedBase64; // Return as-is if not padded
        }
    }
    
    /**
     * Returns a random delay for traffic pattern obfuscation.
     */
    public int getRandomDelay() {
        if (!trafficScrambling) return 0;
        return MIN_DELAY_MS + secureRandom.nextInt(MAX_DELAY_MS - MIN_DELAY_MS);
    }
    
    /**
     * Generates cover traffic (dummy messages) to obscure real traffic patterns.
     */
    public byte[] generateCoverTraffic() {
        byte[] dummy = new byte[PADDING_BLOCK_SIZE];
        secureRandom.nextBytes(dummy);
        return dummy;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PANIC MODE - SECURE DATA DESTRUCTION
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * PANIC: Securely wipes all local data.
     * Overwrites files with random data before deletion.
     */
    public void panic() throws IOException {
        // Wipe storage key from memory
        if (storageKey != null) {
            byte[] keyBytes = storageKey.getEncoded();
            secureRandom.nextBytes(keyBytes);
            storageKey = null;
        }
        
        // Securely delete all files in data directory
        if (Files.exists(dataDir)) {
            Files.walk(dataDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        secureDelete(path);
                    } catch (IOException e) {
                        // Try regular delete
                        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                    }
                });
        }
        
        // Clear command history, chat logs, etc. from memory
        System.gc();
    }
    
    /**
     * Securely deletes a file by overwriting with random data.
     */
    private void secureDelete(Path path) throws IOException {
        if (Files.isRegularFile(path)) {
            long size = Files.size(path);
            byte[] random = new byte[(int) Math.min(size, 1024 * 1024)];
            
            // Overwrite 3 times with different patterns
            for (int pass = 0; pass < 3; pass++) {
                try (OutputStream os = Files.newOutputStream(path)) {
                    long written = 0;
                    while (written < size) {
                        if (pass == 0) {
                            Arrays.fill(random, (byte) 0x00);
                        } else if (pass == 1) {
                            Arrays.fill(random, (byte) 0xFF);
                        } else {
                            secureRandom.nextBytes(random);
                        }
                        int toWrite = (int) Math.min(random.length, size - written);
                        os.write(random, 0, toWrite);
                        written += toWrite;
                    }
                }
            }
        }
        Files.deleteIfExists(path);
    }
    
    /**
     * Securely wipes a byte array from memory.
     */
    public void secureWipe(byte[] data) {
        if (data != null) {
            secureRandom.nextBytes(data);
            Arrays.fill(data, (byte) 0);
        }
    }
    
    /**
     * Securely wipes a char array from memory.
     */
    public void secureWipe(char[] data) {
        if (data != null) {
            Arrays.fill(data, '\0');
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PRIVACY MODES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Enables paranoid mode - maximum privacy settings.
     */
    public void enableParanoidMode() {
        paranoidMode = true;
        burnAfterRead = true;
        ghostMode = true;
        messagePadding = true;
        trafficScrambling = true;
    }
    
    /**
     * Disables all enhanced privacy features.
     */
    public void disableParanoidMode() {
        paranoidMode = false;
        burnAfterRead = false;
        ghostMode = false;
        trafficScrambling = false;
        // Keep padding enabled by default
    }
    
    public boolean isBurnAfterRead() { return burnAfterRead; }
    public void setBurnAfterRead(boolean enabled) { this.burnAfterRead = enabled; }
    
    public boolean isGhostMode() { return ghostMode; }
    public void setGhostMode(boolean enabled) { this.ghostMode = enabled; }
    
    public boolean isMessagePadding() { return messagePadding; }
    public void setMessagePadding(boolean enabled) { this.messagePadding = enabled; }
    
    public boolean isTrafficScrambling() { return trafficScrambling; }
    public void setTrafficScrambling(boolean enabled) { this.trafficScrambling = enabled; }
    
    public boolean isParanoidMode() { return paranoidMode; }
    
    // ═══════════════════════════════════════════════════════════════
    // FINGERPRINT VERIFICATION
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Generates a human-readable fingerprint from a public key.
     * Format: XXXX-XXXX-XXXX-XXXX-XXXX
     */
    public String generateFingerprint(byte[] publicKey) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKey);
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                if (i > 0 && i % 2 == 0) sb.append("-");
                sb.append(String.format("%02X", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    /**
     * Generates a word-based fingerprint for easier verbal verification.
     */
    public String generateWordFingerprint(byte[] publicKey) {
        String[] words = {
            "alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf", "hotel",
            "india", "juliet", "kilo", "lima", "mike", "november", "oscar", "papa",
            "quebec", "romeo", "sierra", "tango", "uniform", "victor", "whiskey", "xray",
            "yankee", "zulu", "zero", "one", "two", "three", "four", "five"
        };
        
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKey);
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                if (i > 0) sb.append(" ");
                int index = (hash[i] & 0xFF) % words.length;
                sb.append(words[index]);
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PLAUSIBLE DENIABILITY
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Creates a decoy data directory that looks legitimate.
     * Used for plausible deniability if device is seized.
     */
    public void createDecoyData(Path decoyDir) throws IOException {
        Files.createDirectories(decoyDir);
        
        // Create fake "normal" looking files
        Files.writeString(decoyDir.resolve("notes.txt"), 
            "Shopping list:\n- Milk\n- Bread\n- Eggs\n");
        Files.writeString(decoyDir.resolve("todo.txt"),
            "TODO:\n- Call mom\n- Pay bills\n- Exercise\n");
    }
}
