package ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * ConnectionPanel provides Ricochet-style UI controls for Tor status,
 * onion address display, and peer connection management.
 */
public class ConnectionPanel extends VBox {
    
    private final Circle statusIndicator;
    private final Label statusLabel;
    private final Label onionAddressLabel;
    private final Button copyAddressButton;
    private final Button showQRButton;
    public final Button addContactButton;
    private final TextField connectField;
    private final Button connectButton;
    
    private Runnable onCopyAddress;
    private Runnable onShowQR;
    private Runnable onAddContact;
    private java.util.function.Consumer<String> onConnect;
    
    public ConnectionPanel() {
        setSpacing(10);
        setPadding(new Insets(15));
        getStyleClass().add("connection-panel");
        
        // --- Status row ---
        HBox statusRow = new HBox(10);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        
        statusIndicator = new Circle(6);
        statusIndicator.setFill(Color.GRAY);
        statusIndicator.getStyleClass().add("status-indicator");
        
        statusLabel = new Label("Initializing...");
        statusLabel.getStyleClass().add("status-label");
        
        statusRow.getChildren().addAll(statusIndicator, statusLabel);
        
        // --- Onion address row ---
        HBox addressRow = new HBox(10);
        addressRow.setAlignment(Pos.CENTER_LEFT);
        
        Label addressTitle = new Label("Your Address:");
        addressTitle.getStyleClass().add("address-title");
        
        onionAddressLabel = new Label("Not connected");
        onionAddressLabel.getStyleClass().add("onion-address");
        onionAddressLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
        HBox.setHgrow(onionAddressLabel, Priority.ALWAYS);
        
        copyAddressButton = new Button("📋");
        copyAddressButton.setTooltip(new Tooltip("Copy onion address"));
        copyAddressButton.getStyleClass().add("icon-button");
        copyAddressButton.setOnAction(e -> { if (onCopyAddress != null) onCopyAddress.run(); });
        
        showQRButton = new Button("QR");
        showQRButton.setTooltip(new Tooltip("Show QR code"));
        showQRButton.getStyleClass().add("icon-button");
        showQRButton.setOnAction(e -> { if (onShowQR != null) onShowQR.run(); });
        
        addressRow.getChildren().addAll(addressTitle, onionAddressLabel, copyAddressButton, showQRButton);
        
        // --- Separator ---
        Separator sep = new Separator();
        
        // --- Connect to peer row ---
        HBox connectRow = new HBox(10);
        connectRow.setAlignment(Pos.CENTER_LEFT);
        
        connectField = new TextField();
        connectField.setPromptText("Enter onion address to connect...");
        connectField.getStyleClass().add("connect-field");
        HBox.setHgrow(connectField, Priority.ALWAYS);
        
        connectButton = new Button("Connect");
        connectButton.getStyleClass().add("connect-button");
        connectButton.setOnAction(e -> {
            String address = connectField.getText().trim();
            if (!address.isEmpty() && onConnect != null) {
                onConnect.accept(address);
                connectField.clear();
            }
        });
        
        connectField.setOnAction(e -> connectButton.fire());
        
        addContactButton = new Button("+ Add Contact");
        addContactButton.getStyleClass().add("add-contact-button");
        addContactButton.setOnAction(e -> { if (onAddContact != null) onAddContact.run(); });
        
        connectRow.getChildren().addAll(connectField, connectButton, addContactButton);
        
        getChildren().addAll(statusRow, addressRow, sep, connectRow);
    }
    
    /**
     * Binds the panel to TorService properties for automatic updates.
     */
    public void bindToTorService(StringProperty onionAddress, StringProperty status, BooleanProperty connected) {
        onionAddressLabel.textProperty().bind(onionAddress);
        statusLabel.textProperty().bind(status);
        
        connected.addListener((obs, wasConnected, isConnected) -> {
            if (isConnected) {
                statusIndicator.setFill(Color.LIMEGREEN);
            } else {
                statusIndicator.setFill(Color.GRAY);
            }
        });
    }
    
    public void setOnCopyAddress(Runnable handler) { this.onCopyAddress = handler; }
    public void setOnShowQR(Runnable handler) { this.onShowQR = handler; }
    public void setOnAddContact(Runnable handler) { this.onAddContact = handler; }
    public void setOnConnect(java.util.function.Consumer<String> handler) { this.onConnect = handler; }
    
    public void setStatus(String status, boolean connected) {
        statusLabel.setText(status);
        statusIndicator.setFill(connected ? Color.LIMEGREEN : Color.GRAY);
    }
    
    public void setOnionAddress(String address) {
        onionAddressLabel.setText(address);
    }
}
