# 🧅 Cwtch Messenger

An anonymous peer-to-peer messaging application using Tor hidden services and the [Cwtch protocol](https://cwtch.im) for end-to-end encryption.

> **"Privacy is not a crime"** — For the privacy-conscious and paranoid alike.

## Features

### Core
- **Anonymous Identity** — Your identity is a Tor hidden service (.onion address)
- **End-to-End Encryption** — X25519 ECDH key exchange + AES-256-GCM message encryption
- **Metadata Resistant** — No central servers, no message logs, no contact discovery
- **Decentralized** — Direct peer-to-peer communication over Tor
- **QR Code Sharing** — Easily share your onion address via QR codes

### Privacy Features 🔒
- **Panic Button** — Emergency data wipe with secure overwrite (Ctrl+P or `/panic`)
- **Ghost Mode** — No online presence, no read receipts, no typing indicators
- **Paranoid Mode** — Maximum privacy with all features enabled
- **Burn After Read** — Messages self-destruct after viewing
- **Message Padding** — All messages padded to 256-byte blocks to hide length
- **Traffic Scrambling** — Random delays between messages to prevent timing analysis
- **Encrypted Storage** — PBKDF2 + AES-256-GCM for local data
- **Fingerprint Verification** — Out-of-band identity verification

### Dual Interface
- **🖥️ Graphical Mode** — Modern dark UI with panels and windows (JavaFX)
- **⌨️ Terminal Mode** — Pure CLI that works on ANY platform with Java

## Quick Start

### Terminal Mode (Recommended - Cross-Platform)

The pure terminal version works on **any device** with Java 17+:

```bash
# Run directly with Maven
mvn exec:java -Dexec.mainClass=app.TerminalMain

# Or with offline mode (no Tor needed for testing)
mvn exec:java -Dexec.mainClass=app.TerminalMain -Dexec.args="--offline"

# Or use the standalone JAR
java -jar target/cwtch-terminal.jar --offline
```

**Terminal Commands:**
```
/help          - Show all commands
/id            - Show your onion address
/add <addr>    - Add a contact
/contacts      - List contacts
/chat <#>      - Start chat with contact #
/connect <addr>- Connect to peer
/ghost         - Toggle ghost mode
/paranoid      - Toggle paranoid mode
/panic         - 🚨 EMERGENCY: Wipe all data
/quit          - Exit
```

### Graphical Mode (Linux/Windows/macOS with JavaFX)

```bash
mvn javafx:run
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Cwtch Messenger                          │
├─────────────────────────────────────────────────────────────┤
│  UI Layer (JavaFX)                                          │
│  ├── ConnectionPanel   - Tor status, address, connect       │
│  ├── ContactList       - Peer contacts with status          │
│  ├── ChatArea          - Message display                    │
│  ├── MessageInput      - Compose and send                   │
│  └── TerminalUI        - Hacker-style CLI interface         │
├─────────────────────────────────────────────────────────────┤
│  Service Layer                                              │
│  ├── TorService        - Manages Tor hidden service         │
│  ├── PeerManager       - Handles peer connections           │
│  └── PrivacyGuard      - Advanced privacy features          │
├─────────────────────────────────────────────────────────────┤
│  Protocol Layer (cwtch-java-protocol)                       │
│  ├── Identity          - Ed25519 cryptographic identity     │
│  ├── Handshake         - X25519 ECDH key exchange           │
│  ├── SessionCrypto     - HKDF + AES-GCM encryption          │
│  ├── PeerChannel       - Encrypted message transport        │
│  └── TorManager        - Tor process & hidden service       │
└─────────────────────────────────────────────────────────────┘
```

## Terminal Mode Commands

```
╔══════════════════════════════════════════════════════════════╗
║                    AVAILABLE COMMANDS                        ║
╠══════════════════════════════════════════════════════════════╣
║  GENERAL                                                     ║
║    help              - Show help message                     ║
║    clear             - Clear terminal screen                 ║
║    status            - Show connection status                ║
║    whoami            - Show your onion address               ║
║                                                              ║
║  CONTACTS                                                    ║
║    list              - List all contacts                     ║
║    add <addr> [name] - Add contact by onion address          ║
║    chat <id>         - Open chat with contact                ║
║    connect           - Connect to selected contact           ║
║    disconnect        - Disconnect from contact               ║
║                                                              ║
║  MESSAGING                                                   ║
║    msg <text>        - Send message to current chat          ║
║    (or just type)    - Messages sent to selected contact     ║
║                                                              ║
║  PRIVACY                                                     ║
║    panic             - EMERGENCY: Wipe all data              ║
║    ghost             - Enter ghost mode (no presence)        ║
║    paranoid          - Enable all privacy features           ║
║    burn              - Toggle burn-after-read mode           ║
║    padding           - Toggle message padding                ║
║    scramble          - Toggle traffic scrambling             ║
║    rekey             - Force session key rotation            ║
║                                                              ║
║  CRYPTO                                                      ║
║    fingerprint       - Show identity fingerprint             ║
║    verify <id>       - Verify contact's fingerprint          ║
╚══════════════════════════════════════════════════════════════╝
```

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+P` | Panic mode - wipe all data |
| `Ctrl+G` | Toggle ghost mode |
| `Ctrl+L` | Clear terminal screen |
| `Ctrl+C` | Cancel current input |
| `↑/↓` | Command history navigation |
| `Tab` | Auto-complete commands |

## Prerequisites

- **Java 17+**
- **Maven**
- **Tor** installed and running with `ControlPort 9051` enabled

### Tor Configuration

Add to `/etc/tor/torrc`:
```
ControlPort 9051
CookieAuthentication 1
```

Restart Tor:
```bash
sudo systemctl restart tor
```

## Building

1. First, install the cwtch-java-protocol to your local Maven repo:
```bash
cd ../cwtch-java-protocol
mvn install
```

2. Build Cwtch Messenger:
```bash
cd ../CwtchMessenger
mvn clean package
```

## Running

### Default (Mode Selector)
```bash
mvn javafx:run
```

### Direct Terminal Mode
```bash
mvn javafx:run -Djavafx.args="--mode=terminal"
```

### Direct GUI Mode
```bash
mvn javafx:run -Djavafx.args="--mode=gui"
```

Or run the JAR directly:
```bash
java -jar target/cwtch-messenger-1.0-SNAPSHOT.jar
```

## Usage

### GUI Mode
1. **Start the app** — Wait for Tor to connect (green indicator)
2. **Share your address** — Click QR or copy your .onion address
3. **Add contacts** — Enter their .onion address
4. **Connect** — Right-click a contact and select "Connect"
5. **Chat** — Double-click a contact to open chat, send encrypted messages

### Terminal Mode
1. **Boot sequence** — Watch the Matrix-style boot animation
2. **View identity** — Type `whoami` to see your .onion address
3. **Add contact** — `add abc123...xyz.onion Alice`
4. **Start chat** — `chat 1` or `chat Alice`
5. **Send message** — Just type and press Enter
6. **Enable privacy** — `paranoid` for maximum protection

## Security Model

| Threat | Mitigation |
|--------|------------|
| Eavesdropping | AES-256-GCM encryption with per-session keys |
| Man-in-the-middle | X25519 ECDH with identity verification |
| Traffic analysis | Tor hidden services + optional padding/scrambling |
| Metadata exposure | No central servers, P2P only, ghost mode |
| Replay attacks | Message counters and sequence validation |
| Data seizure | Panic button, encrypted storage |
| Message length analysis | 256-byte block padding |
| Timing analysis | Random message delays |

## Dependencies

- [cwtch-java-protocol](../cwtch-java-protocol) — Core protocol implementation
- [JavaFX 17](https://openjfx.io/) — UI framework
- [ZXing](https://github.com/zxing/zxing) — QR code generation/scanning
- [BouncyCastle](https://www.bouncycastle.org/) — Cryptography (via protocol lib)

## License

Apache License 2.0

## Credits

- Inspired by [Ricochet](https://ricochet.im/) and [Cwtch](https://cwtch.im/)
- Uses the [Tor Project](https://www.torproject.org/) for anonymity
