package Code.Windows;

import Code.CommonClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import sun.rmi.runtime.Log;

import java.io.FileWriter;
import java.net.URL;
import java.util.ResourceBundle;

public class askSudo implements Initializable {

    @FXML
    private TextField pswd;
    @FXML
    private Button okbutton;
    @FXML
    private CheckBox remeberpswd;
    @FXML
    private AnchorPane ap;

    Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        okbutton.setOnAction(actionEvent -> {
            if (remeberpswd.isSelected()) {
                CommonClass.writeSetting("pass",pswd.getText());
            }
            stage.close();
        });
    }
}
