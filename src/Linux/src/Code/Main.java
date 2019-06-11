package Code;

import Code.Windows.askSudo;
import javafx.application.Application;
import javafx.event.EventType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/* Installation Instructions..
*
* unzip jdk_1.8
* sudo apt-get install p7zip-full (need 7z)
* */

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("iso2Usb.fxml"));
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("Iso2Usb - 1.0.5.0 (Portable)");
        ((Controller) fxmlLoader.getController()).setStage(primaryStage);
        primaryStage.getIcons().add(new Image(("file:icon.png")));
        Scene scene = new Scene(root, 617, 531);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
