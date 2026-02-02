package ui;

import java.util.Optional;

import javafx.scene.control.TextInputDialog;

/**
 * UsernameDialog shows a dialog for the user to enter their username at startup.
 */
public class UsernameDialog {
    public static String promptForUsername() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Choose Username");
        dialog.setHeaderText("Enter your username:");
        dialog.setContentText("Username:");
        Optional<String> result = dialog.showAndWait();
        return result.filter(name -> !name.trim().isEmpty()).orElse("Anonymous");
    }
}
