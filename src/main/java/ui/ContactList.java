package ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import service.PeerManager;

/**
 * ContactList displays Ricochet-style contacts with onion addresses and connection status.
 */
public class ContactList extends VBox {
    
    /**
     * Contact represents a peer with onion address and connection status.
     */
    public static class Contact {
        public String name;
        public String onionAddress;
        public boolean connected;
        
        public Contact(String name) {
            this.name = name;
            this.onionAddress = "";
            this.connected = false;
        }
        
        public Contact(String name, String onionAddress) {
            this.name = name;
            this.onionAddress = onionAddress;
            this.connected = false;
        }
        
        public String getDisplayName() {
            if (name != null && !name.isEmpty()) return name;
            return shortenOnion(onionAddress);
        }
        
        private String shortenOnion(String onion) {
            if (onion == null || onion.length() < 16) return onion;
            return onion.substring(0, 8) + "...";
        }
        
        @Override
        public String toString() { return getDisplayName(); }
    }

    public static class Group {
        public String name;
        public List<Contact> members;
        public Group(String name, List<Contact> members) {
            this.name = name;
            this.members = members;
        }
        @Override
        public String toString() { return "[Group] " + name; }
    }

    private ListView<Contact> listView;
    private List<Contact> contacts = new ArrayList<>();
    private List<Group> groups = new ArrayList<>();
    private ContactClickListener contactClickListener;
    private GroupClickListener groupClickListener;
    private ContactActionListener contactActionListener;

    public interface ContactClickListener { void onContactClick(Contact contact); }
    public interface GroupClickListener { void onGroupClick(Group group); }
    public interface ContactActionListener { 
        void onConnect(Contact contact);
        void onDisconnect(Contact contact);
        void onRemove(Contact contact);
    }

    public ContactList() {
        getStyleClass().add("contact-list");
        setPrefWidth(220);
        setSpacing(5);
        setPadding(new Insets(10));
        
        Label header = new Label("Contacts");
        header.getStyleClass().add("contact-list-header");
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        listView = new ListView<>();
        listView.getStyleClass().add("contact-listview");
        listView.setCellFactory(lv -> new ContactCell());
        VBox.setVgrow(listView, Priority.ALWAYS);
        
        getChildren().addAll(header, listView);

        // Handle double-click to open chat
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Contact selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null && contactClickListener != null) {
                    contactClickListener.onContactClick(selected);
                }
            }
        });
    }
    
    /**
     * Custom cell factory for contacts with status indicator and context menu.
     */
    private class ContactCell extends ListCell<Contact> {
        private final HBox content;
        private final Circle statusDot;
        private final Label nameLabel;
        private final Label addressLabel;
        
        public ContactCell() {
            content = new HBox(8);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(5));
            
            statusDot = new Circle(5);
            statusDot.getStyleClass().add("contact-status");
            
            VBox textBox = new VBox(2);
            nameLabel = new Label();
            nameLabel.getStyleClass().add("contact-name");
            addressLabel = new Label();
            addressLabel.getStyleClass().add("contact-address");
            addressLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
            textBox.getChildren().addAll(nameLabel, addressLabel);
            HBox.setHgrow(textBox, Priority.ALWAYS);
            
            content.getChildren().addAll(statusDot, textBox);
            
            // Context menu
            ContextMenu contextMenu = new ContextMenu();
            MenuItem connectItem = new MenuItem("Connect");
            MenuItem disconnectItem = new MenuItem("Disconnect");
            MenuItem copyAddressItem = new MenuItem("Copy Address");
            MenuItem removeItem = new MenuItem("Remove");
            
            connectItem.setOnAction(e -> {
                if (contactActionListener != null && getItem() != null) {
                    contactActionListener.onConnect(getItem());
                }
            });
            disconnectItem.setOnAction(e -> {
                if (contactActionListener != null && getItem() != null) {
                    contactActionListener.onDisconnect(getItem());
                }
            });
            copyAddressItem.setOnAction(e -> {
                if (getItem() != null && getItem().onionAddress != null) {
                    javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                    javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                    cc.putString(getItem().onionAddress);
                    clipboard.setContent(cc);
                }
            });
            removeItem.setOnAction(e -> {
                if (contactActionListener != null && getItem() != null) {
                    contactActionListener.onRemove(getItem());
                }
            });
            
            contextMenu.getItems().addAll(connectItem, disconnectItem, new SeparatorMenuItem(), copyAddressItem, removeItem);
            setContextMenu(contextMenu);
        }
        
        @Override
        protected void updateItem(Contact contact, boolean empty) {
            super.updateItem(contact, empty);
            if (empty || contact == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(contact.getDisplayName());
                addressLabel.setText(shortenAddress(contact.onionAddress));
                statusDot.setFill(contact.connected ? Color.LIMEGREEN : Color.GRAY);
                setGraphic(content);
            }
        }
        
        private String shortenAddress(String addr) {
            if (addr == null || addr.length() < 20) return addr;
            return addr.substring(0, 12) + "..." + addr.substring(addr.length() - 6);
        }
    }
    
    public void addContact(Contact contact) {
        this.contacts.add(contact);
        updateList();
    }
    
    public void removeContact(Contact contact) {
        this.contacts.remove(contact);
        updateList();
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = new ArrayList<>(contacts);
        updateList();
    }
    
    public void setGroups(List<Group> groups) {
        this.groups = groups;
        // Groups not shown in this version
    }
    
    private void updateList() {
        Platform.runLater(() -> {
            listView.getItems().setAll(contacts);
        });
    }
    
    /**
     * Updates the connection status of a contact.
     */
    public void updateContactStatus(String onionAddress, boolean connected) {
        for (Contact c : contacts) {
            if (onionAddress.equals(c.onionAddress)) {
                c.connected = connected;
                break;
            }
        }
        updateList();
    }
    
    /**
     * Syncs contact list from PeerManager.
     */
    public void syncFromPeerManager(Map<String, PeerManager.Peer> peers) {
        for (PeerManager.Peer peer : peers.values()) {
            boolean found = false;
            for (Contact c : contacts) {
                if (peer.onionAddress.equals(c.onionAddress)) {
                    c.connected = peer.connected;
                    c.name = peer.name != null ? peer.name : c.name;
                    found = true;
                    break;
                }
            }
            if (!found) {
                Contact newContact = new Contact(peer.name, peer.onionAddress);
                newContact.connected = peer.connected;
                contacts.add(newContact);
            }
        }
        updateList();
    }
    
    public void setContactClickListener(ContactClickListener listener) {
        this.contactClickListener = listener;
    }
    
    public void setGroupClickListener(GroupClickListener listener) {
        this.groupClickListener = listener;
    }
    
    public void setContactActionListener(ContactActionListener listener) {
        this.contactActionListener = listener;
    }
    
    public List<Contact> getContacts() {
        return new ArrayList<>(contacts);
    }
    
    public Contact getSelectedContact() {
        return listView.getSelectionModel().getSelectedItem();
    }
}
