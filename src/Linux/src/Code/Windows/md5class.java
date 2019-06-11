package Code.Windows;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class md5class implements Initializable {

    @FXML
    private TextField md5box;
    @FXML
    private TextField sha1box;
    @FXML
    private Button okbutton;

    Stage stage;

    public void setStage(Stage stage, String md5, String sha1) {
        this.stage = stage;
        md5box.setText(md5);
        sha1box.setText(sha1);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        okbutton.setOnAction(e-> stage.close());
    }
}
