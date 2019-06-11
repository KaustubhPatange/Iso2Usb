package Code.Windows;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import com.sun.javafx.application.HostServicesDelegate;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class about implements Initializable {

    @FXML private ImageView github_button;
    @FXML private ImageView visitweb_button;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        github_button.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            try {
                new ProcessBuilder("x-www-browser", "https://google.com").start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
