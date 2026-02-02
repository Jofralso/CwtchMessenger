package ui;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class MessageInput extends HBox {
    public TextField messageField;
    public Button sendButton;

    public MessageInput() {
        super(10);
        setStyle("-fx-padding: 10px;");
        messageField = new TextField();
        messageField.setPromptText("Type your message...");
        sendButton = new Button("Send");
        getChildren().addAll(messageField, sendButton);
    }
}
