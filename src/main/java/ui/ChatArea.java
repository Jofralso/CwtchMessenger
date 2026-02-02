package ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/**
 * ChatArea displays messages in a scrollable view with automatic scrolling.
 */
public class ChatArea extends ScrollPane {
    private final VBox messageContainer;
    
    public ChatArea() {
        messageContainer = new VBox(5);
        messageContainer.setPadding(new Insets(10));
        messageContainer.getStyleClass().add("chat-messages");
        
        setContent(messageContainer);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        getStyleClass().add("chat-area");
        
        // Auto-scroll to bottom when new messages are added
        messageContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            setVvalue(1.0);
        });
    }

    /**
     * Adds a message to the chat area.
     */
    public void addMessage(String message) {
        Platform.runLater(() -> {
            Label msgLabel = new Label(message);
            msgLabel.setWrapText(true);
            msgLabel.getStyleClass().add("chat-message");
            
            // Style system messages differently
            if (message.startsWith("[System]") || message.startsWith("[Chat with")) {
                msgLabel.getStyleClass().add("system-message");
            } else if (message.startsWith("You:")) {
                msgLabel.getStyleClass().add("own-message");
            } else {
                msgLabel.getStyleClass().add("peer-message");
            }
            
            messageContainer.getChildren().add(msgLabel);
        });
    }
    
    /**
     * Clears all messages from the chat area.
     */
    public void clear() {
        Platform.runLater(() -> {
            messageContainer.getChildren().clear();
        });
    }
}
