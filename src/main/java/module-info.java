module psp.taskmanager {
    requires javafx.controls;
    requires javafx.fxml;

    opens psp.taskmanager to javafx.fxml;
    exports psp.taskmanager;
}
