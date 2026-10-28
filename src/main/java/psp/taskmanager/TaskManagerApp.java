package psp.taskmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class TaskManagerApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        URL fxml = TaskManagerApp.class.getResource("task-manager.fxml");
        if (fxml == null) {
            // The IDE compiled the classes but did not copy the resources to target/classes
            throw new IllegalStateException("task-manager.fxml not found in target/classes. "
                    + "Open the project in IntelliJ through its pom.xml file, "
                    + "or run it with Maven: javafx:run");
        }

        FXMLLoader loader = new FXMLLoader(fxml);
        Scene scene = new Scene(loader.load(), 900, 600);

        TaskManagerController controller = loader.getController();
        // When the window is closed, the controller decides what to do with the running processes
        stage.setOnCloseRequest(event -> controller.shutdown());

        stage.setTitle("PSP - Mini Task Manager");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
