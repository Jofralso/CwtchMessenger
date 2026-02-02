package ui;

import java.util.Optional;

import javafx.scene.control.TextInputDialog;

/**
 * AddContactDialog shows a dialog to enter a username for adding a contact.
 */
public class AddContactDialog {
    public static String promptForUsername() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Contact");
        dialog.setHeaderText("Enter the username of the contact to add:");
        dialog.setContentText("Username:");
        Optional<String> result = dialog.showAndWait();
        return result.filter(name -> !name.trim().isEmpty()).orElse(null);
    }
}
