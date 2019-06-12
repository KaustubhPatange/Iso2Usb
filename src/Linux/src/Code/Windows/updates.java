package Code.Windows;

import Code.CommonClass;
import Code.Controller;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class updates implements Initializable {

    @FXML private ComboBox comboBox;
    @FXML private Button close;
    @FXML private Button checkNow;

    Stage stage; FXMLLoader fxmlLoader;

    public void setStage(Stage stage, FXMLLoader loader) {
        this.stage = stage;
        this.fxmlLoader = loader;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Adding items to combo box and making a default selection
        comboBox.getItems().addAll("Disabled","Enabled");
        if (CommonClass.readSetting("autoupdate").contains("yes")) {
            comboBox.getSelectionModel().select(1);
        }else comboBox.getSelectionModel().select(0);

        // Adding events to controls
        close.setOnAction(e->stage.close());
        checkNow.setOnAction(e-> ((Controller) fxmlLoader.getController()).CheckUpdates());
        // This will change update settings
        comboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            switch (comboBox.getSelectionModel().getSelectedIndex()) {
                case 0:
                    CommonClass.writeSetting("autoupdate","no");
                    break;
                case 1:
                    CommonClass.writeSetting("autoupdate","yes");
                    break;
            }
        });
    }
}
