import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class JavaFXTest extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label("JavaFX Working!");

        Scene scene = new Scene(new StackPane(label), 400, 200);

        stage.setTitle("Knowledge Black Hole Detector");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
