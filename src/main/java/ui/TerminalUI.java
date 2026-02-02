package ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * TerminalUI - A hacker-style terminal interface for Cwtch Messenger.
 * Features ASCII art, command-line interface, and Matrix-style aesthetics.
 */
public class TerminalUI extends VBox {
    
    private static final String TERMINAL_GREEN = "#00ff41";
    private static final String TERMINAL_DIM = "#00aa2a";
    private static final String TERMINAL_BG = "#0a0a0a";
    private static final String TERMINAL_RED = "#ff3333";
    private static final String TERMINAL_YELLOW = "#ffff00";
    private static final String TERMINAL_CYAN = "#00ffff";
    
    private final TextFlow outputArea;
    private final ScrollPane scrollPane;
    private final TextField inputField;
    private final Label promptLabel;
    private final Label statusBar;
    
    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;
    
    private Consumer<String> commandHandler;
    private String currentPrompt = "cwtch> ";
    private boolean inputEnabled = true;
    
    // ASCII Art Logo
    private static final String ASCII_LOGO = """
     ██████╗██╗    ██╗████████╗ ██████╗██╗  ██╗
    ██╔════╝██║    ██║╚══██╔══╝██╔════╝██║  ██║
    ██║     ██║ █╗ ██║   ██║   ██║     ███████║
    ██║     ██║███╗██║   ██║   ██║     ██╔══██║
    ╚██████╗╚███╔███╔╝   ██║   ╚██████╗██║  ██║
     ╚═════╝ ╚══╝╚══╝    ╚═╝    ╚═════╝╚═╝  ╚═╝
    ══════════════════════════════════════════════
       M E S S E N G E R  //  v1.0  //  SECURE
    ══════════════════════════════════════════════
    """;
    
    private static final String HELP_TEXT = """
    ╔══════════════════════════════════════════════════════════════╗
    ║                    AVAILABLE COMMANDS                        ║
    ╠══════════════════════════════════════════════════════════════╣
    ║  GENERAL                                                     ║
    ║    help              - Show this help message                ║
    ║    clear / cls       - Clear terminal screen                 ║
    ║    status            - Show connection status                ║
    ║    whoami            - Show your onion address               ║
    ║    uptime            - Show session uptime                   ║
    ║                                                              ║
    ║  CONTACTS                                                    ║
    ║    list              - List all contacts                     ║
    ║    add <addr> [name] - Add contact by onion address          ║
    ║    remove <id>       - Remove contact                        ║
    ║    connect <id>      - Connect to contact                    ║
    ║    disconnect <id>   - Disconnect from contact               ║
    ║                                                              ║
    ║  MESSAGING                                                   ║
    ║    chat <id>         - Open chat with contact                ║
    ║    msg <text>        - Send message to current chat          ║
    ║    burn              - Enable burn-after-read mode           ║
    ║    history           - Show message history                  ║
    ║                                                              ║
    ║  PRIVACY                                                     ║
    ║    panic             - EMERGENCY: Wipe all data              ║
    ║    scramble          - Randomize traffic patterns            ║
    ║    padding <on|off>  - Toggle message padding                ║
    ║    ghost             - Enter ghost mode (no presence)        ║
    ║    paranoid          - Maximum privacy settings              ║
    ║                                                              ║
    ║  CRYPTO                                                      ║
    ║    fingerprint       - Show identity fingerprint             ║
    ║    verify <id>       - Verify contact's fingerprint          ║
    ║    rekey             - Force session key rotation            ║
    ║                                                              ║
    ║  Press ↑/↓ for command history | Tab for auto-complete       ║
    ╚══════════════════════════════════════════════════════════════╝
    """;
    
    public TerminalUI() {
        setStyle("-fx-background-color: " + TERMINAL_BG + ";");
        setPadding(new Insets(10));
        setSpacing(5);
        
        // Output area with TextFlow for colored text
        outputArea = new TextFlow();
        outputArea.setStyle("-fx-background-color: " + TERMINAL_BG + ";");
        outputArea.setPadding(new Insets(10));
        outputArea.setLineSpacing(2);
        
        scrollPane = new ScrollPane(outputArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: " + TERMINAL_BG + "; -fx-background: " + TERMINAL_BG + ";");
        scrollPane.getStyleClass().add("terminal-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Auto-scroll
        outputArea.heightProperty().addListener((obs, old, val) -> scrollPane.setVvalue(1.0));
        
        // Input line with prompt
        HBox inputLine = new HBox(5);
        inputLine.setAlignment(Pos.CENTER_LEFT);
        inputLine.setStyle("-fx-background-color: " + TERMINAL_BG + ";");
        
        promptLabel = new Label(currentPrompt);
        promptLabel.setStyle("-fx-text-fill: " + TERMINAL_GREEN + "; -fx-font-family: 'Courier New', monospace; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        inputField = new TextField();
        inputField.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: " + TERMINAL_GREEN + "; " +
            "-fx-font-family: 'Courier New', monospace; " +
            "-fx-font-size: 14px; " +
            "-fx-border-width: 0; " +
            "-fx-focus-color: transparent; " +
            "-fx-faint-focus-color: transparent;"
        );
        HBox.setHgrow(inputField, Priority.ALWAYS);
        
        // Blinking cursor effect
        inputField.setPromptText("_");
        
        inputLine.getChildren().addAll(promptLabel, inputField);
        
        // Status bar
        statusBar = new Label("[ TOR: CONNECTING... ] [ ENCRYPT: AES-256-GCM ] [ MODE: NORMAL ]");
        statusBar.setStyle(
            "-fx-text-fill: " + TERMINAL_DIM + "; " +
            "-fx-font-family: 'Courier New', monospace; " +
            "-fx-font-size: 11px; " +
            "-fx-padding: 5 0 0 0;"
        );
        
        getChildren().addAll(scrollPane, inputLine, statusBar);
        
        // Key handlers
        setupKeyHandlers();
        
        // Display boot sequence
        Platform.runLater(this::displayBootSequence);
    }
    
    private void setupKeyHandlers() {
        inputField.setOnAction(e -> {
            if (inputEnabled) {
                String command = inputField.getText().trim();
                if (!command.isEmpty()) {
                    commandHistory.add(0, command);
                    historyIndex = -1;
                    echoCommand(command);
                    inputField.clear();
                    if (commandHandler != null) {
                        commandHandler.accept(command);
                    }
                }
            }
        });
        
        inputField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.UP) {
                if (historyIndex < commandHistory.size() - 1) {
                    historyIndex++;
                    inputField.setText(commandHistory.get(historyIndex));
                    inputField.positionCaret(inputField.getText().length());
                }
                e.consume();
            } else if (e.getCode() == KeyCode.DOWN) {
                if (historyIndex > 0) {
                    historyIndex--;
                    inputField.setText(commandHistory.get(historyIndex));
                } else if (historyIndex == 0) {
                    historyIndex = -1;
                    inputField.clear();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.TAB) {
                // TODO: Auto-complete
                e.consume();
            } else if (e.getCode() == KeyCode.C && e.isControlDown()) {
                inputField.clear();
                printLine("^C", TERMINAL_RED);
                e.consume();
            } else if (e.getCode() == KeyCode.L && e.isControlDown()) {
                clearScreen();
                e.consume();
            }
        });
    }
    
    public void displayBootSequence() {
        inputEnabled = false;
        
        Timeline bootSequence = new Timeline();
        int delay = 0;
        
        // ASCII logo with typing effect
        String[] logoLines = ASCII_LOGO.split("\n");
        for (String line : logoLines) {
            final String l = line;
            bootSequence.getKeyFrames().add(new KeyFrame(Duration.millis(delay), e -> printLine(l, TERMINAL_GREEN)));
            delay += 50;
        }
        
        // Boot messages
        String[] bootMessages = {
            "",
            "[" + getTimestamp() + "] Initializing secure environment...",
            "[" + getTimestamp() + "] Loading cryptographic modules... OK",
            "[" + getTimestamp() + "] Generating ephemeral keys... OK",
            "[" + getTimestamp() + "] Connecting to Tor network...",
            "[" + getTimestamp() + "] Establishing hidden service...",
            "",
            "Type 'help' for available commands.",
            ""
        };
        
        for (String msg : bootMessages) {
            final String m = msg;
            String color = msg.contains("OK") ? TERMINAL_GREEN : 
                          msg.contains("...") ? TERMINAL_YELLOW : TERMINAL_DIM;
            bootSequence.getKeyFrames().add(new KeyFrame(Duration.millis(delay), e -> printLine(m, color)));
            delay += 100;
        }
        
        bootSequence.getKeyFrames().add(new KeyFrame(Duration.millis(delay), e -> {
            inputEnabled = true;
            inputField.requestFocus();
        }));
        
        bootSequence.play();
    }
    
    public void printLine(String text, String color) {
        Platform.runLater(() -> {
            Text textNode = new Text(text + "\n");
            textNode.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 13px;");
            textNode.setFill(Color.web(color));
            outputArea.getChildren().add(textNode);
        });
    }
    
    public void printLine(String text) {
        printLine(text, TERMINAL_GREEN);
    }
    
    public void printSystem(String text) {
        printLine("[SYS] " + text, TERMINAL_CYAN);
    }
    
    public void printError(String text) {
        printLine("[ERR] " + text, TERMINAL_RED);
    }
    
    public void printWarning(String text) {
        printLine("[!] " + text, TERMINAL_YELLOW);
    }
    
    public void printSuccess(String text) {
        printLine("[OK] " + text, TERMINAL_GREEN);
    }
    
    /**
     * Print a message in the chat style (convenience method without isOwn param)
     */
    public void printMessage(String from, String message) {
        printMessage(from, message, false);
    }
    
    public void printMessage(String from, String message, boolean isOwn) {
        Platform.runLater(() -> {
            String timestamp = getTimestamp();
            String prefix = isOwn ? ">> " : "<< ";
            String color = isOwn ? TERMINAL_CYAN : TERMINAL_GREEN;
            
            Text timeText = new Text("[" + timestamp + "] ");
            timeText.setFill(Color.web(TERMINAL_DIM));
            timeText.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 13px;");
            
            Text prefixText = new Text(prefix + from + ": ");
            prefixText.setFill(Color.web(color));
            prefixText.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 13px; -fx-font-weight: bold;");
            
            Text msgText = new Text(message + "\n");
            msgText.setFill(Color.web(TERMINAL_GREEN));
            msgText.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 13px;");
            
            outputArea.getChildren().addAll(timeText, prefixText, msgText);
        });
    }
    
    private void echoCommand(String command) {
        Platform.runLater(() -> {
            Text prompt = new Text(currentPrompt);
            prompt.setFill(Color.web(TERMINAL_GREEN));
            prompt.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 13px; -fx-font-weight: bold;");
            
            Text cmd = new Text(command + "\n");
            cmd.setFill(Color.web(TERMINAL_GREEN));
            cmd.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 13px;");
            
            outputArea.getChildren().addAll(prompt, cmd);
        });
    }
    
    public void clearScreen() {
        Platform.runLater(() -> outputArea.getChildren().clear());
    }
    
    public void showHelp() {
        for (String line : HELP_TEXT.split("\n")) {
            printLine(line, TERMINAL_GREEN);
        }
    }
    
    public void setPrompt(String prompt) {
        this.currentPrompt = prompt;
        Platform.runLater(() -> promptLabel.setText(prompt));
    }
    
    /**
     * Updates the prompt to show the onion address prefix
     */
    public void setOnionAddress(String address) {
        if (address != null && address.length() > 8) {
            setPrompt("cwtch@" + address.substring(0, 8) + "...> ");
        }
    }
    
    /**
     * Sets the currently selected contact in the prompt
     */
    public void setSelectedContact(String contact) {
        if (contact != null) {
            setPrompt(contact + "@cwtch> ");
        } else {
            setPrompt("cwtch> ");
        }
    }
    
    public void setCommandHandler(Consumer<String> handler) {
        this.commandHandler = handler;
    }
    
    public void updateStatusBar(String torStatus, String mode) {
        Platform.runLater(() -> {
            String torColor = torStatus.contains("CONNECTED") ? "CONNECTED" : "CONNECTING...";
            statusBar.setText("[ TOR: " + torColor + " ] [ ENCRYPT: AES-256-GCM ] [ MODE: " + mode.toUpperCase() + " ]");
        });
    }
    
    public void focusInput() {
        Platform.runLater(() -> inputField.requestFocus());
    }
    
    private String getTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    /**
     * Matrix-style rain effect for panic mode
     */
    public void matrixRain(Runnable onComplete) {
        inputEnabled = false;
        clearScreen();
        
        Random rand = new Random();
        Timeline rain = new Timeline();
        
        for (int i = 0; i < 50; i++) {
            final int line = i;
            rain.getKeyFrames().add(new KeyFrame(Duration.millis(i * 30), e -> {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < 60; j++) {
                    sb.append((char)(rand.nextInt(94) + 33));
                }
                printLine(sb.toString(), line % 2 == 0 ? TERMINAL_GREEN : TERMINAL_DIM);
            }));
        }
        
        rain.getKeyFrames().add(new KeyFrame(Duration.millis(1600), e -> {
            clearScreen();
            printLine("", TERMINAL_GREEN);
            printLine("  ██████╗  █████╗ ███╗   ██╗██╗ ██████╗", TERMINAL_RED);
            printLine("  ██╔══██╗██╔══██╗████╗  ██║██║██╔════╝", TERMINAL_RED);
            printLine("  ██████╔╝███████║██╔██╗ ██║██║██║     ", TERMINAL_RED);
            printLine("  ██╔═══╝ ██╔══██║██║╚██╗██║██║██║     ", TERMINAL_RED);
            printLine("  ██║     ██║  ██║██║ ╚████║██║╚██████╗", TERMINAL_RED);
            printLine("  ╚═╝     ╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝ ╚═════╝", TERMINAL_RED);
            printLine("", TERMINAL_RED);
            printLine("  ALL DATA HAS BEEN SECURELY WIPED", TERMINAL_RED);
            printLine("  Memory overwritten with random data", TERMINAL_YELLOW);
            printLine("  Session terminated", TERMINAL_YELLOW);
            printLine("", TERMINAL_RED);
        }));
        
        rain.getKeyFrames().add(new KeyFrame(Duration.millis(3000), e -> {
            if (onComplete != null) onComplete.run();
        }));
        
        rain.play();
    }
}
