package psp.taskmanager;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TaskManagerController {
    /** Folder where the output of every launched process is saved. */
    private static final Path LOG_DIR = Path.of("logs");

    // ---------- Tab 1: my processes ----------
    @FXML private TextField commandField;
    @FXML private TableView<ManagedProcess> processTable;
    @FXML private TableColumn<ManagedProcess, Long> pidColumn;
    @FXML private TableColumn<ManagedProcess, String> commandColumn;
    @FXML private TableColumn<ManagedProcess, String> startColumn;
    @FXML private TableColumn<ManagedProcess, String> statusColumn;
    @FXML private TableColumn<ManagedProcess, String> exitCodeColumn;
    @FXML private Label logLabel;
    @FXML private TextArea logArea;

    // ---------- Tab 2: system processes (optional) ----------
    @FXML private TextField filterField;
    @FXML private TableView<ProcessHandle> systemTable;
    @FXML private TableColumn<ProcessHandle, Long> sysPidColumn;
    @FXML private TableColumn<ProcessHandle, String> sysParentColumn;
    @FXML private TableColumn<ProcessHandle, String> sysUserColumn;
    @FXML private TableColumn<ProcessHandle, String> sysCommandColumn;
    @FXML private Label systemInfoLabel;

    /** Rows of the "My processes" table. Add or remove ManagedProcess objects here. */
    private final ObservableList<ManagedProcess> processes = FXCollections.observableArrayList();

    /** Rows of the "System processes" table. */
    private final ObservableList<ProcessHandle> systemProcesses = FXCollections.observableArrayList();

    /** Connects the columns with the data. It is complete: you do not need to change it. */
    @FXML
    private void initialize() {
        pidColumn.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getPid()));
        commandColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getCommand()));
        startColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getStartTime()));
        statusColumn.setCellValueFactory(d -> d.getValue().statusProperty());
        exitCodeColumn.setCellValueFactory(d -> d.getValue().exitCodeProperty());
        processTable.setItems(processes);

        sysPidColumn.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().pid()));
        sysParentColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().parent().map(p -> String.valueOf(p.pid())).orElse("-")));
        sysUserColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().info().user().orElse("?")));
        sysCommandColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(commandOf(d.getValue())));
        systemTable.setItems(systemProcesses);
    }

    // =====================================================================
    // Level 1: launch processes
    // =====================================================================

    /** "Launch" button, or Enter in the command field. */
    @FXML
    private void onLaunch() {
        // TODO 1.1 Read the text of commandField. If it is empty, show a warning and return.
        String commandFieldText = commandField.getText();

        if (commandFieldText.isBlank()){
            showWarning("The command field cant be blank");
            return;
        }

        // TODO 1.2 Create the LOG_DIR folder if it does not exist (Files.createDirectories)
        //          and a new log file inside it (Files.createTempFile(LOG_DIR, "process-", ".log")).
        Path logFile;
        try {
            Files.createDirectories(LOG_DIR);
            logFile = Files.createTempFile(LOG_DIR, "process-", ".log");
        } catch (IOException e){
            showError(e.getMessage());
            return;
        }

        // TODO 1.3 Create a ProcessBuilder. Use CommandLine.split(text) to separate program and arguments.
        //          Redirect stdout to the log file and merge stderr into stdout.
        List<String> splitCommandFieldText = CommandLine.split(commandFieldText);
        ProcessBuilder processBuilder = new ProcessBuilder(splitCommandFieldText);
        processBuilder.redirectOutput(logFile.toFile());
        processBuilder.redirectErrorStream(true);

        // TODO 1.4 Start the process, create a ManagedProcess and add it to the "processes" list.
        //          Select the new row in processTable and clear the command field.

        try {
            Process process = processBuilder.start();
            ManagedProcess managedProcess = new ManagedProcess(process, commandFieldText, logFile);

            // TODO 3.1. (optional): Automatic status Pressing Refresh status is not very comfortable.
            //  Process.onExit() lets you run an action when the process ends. Add this to onLaunch(),
            //  after creating item:

            process.onExit().thenAccept(p->
                    Platform.runLater(()-> managedProcess.markFinished(p.exitValue())));

            // The action of onExit() runs in a different thread, and only the JavaFX thread can modify the window.
            // Platform.runLater() sends the change to the JavaFX thread.
            // For now, use it as a recipe.
            // You will understand it completely when you study threads.

            processes.add(managedProcess);
            processTable.getSelectionModel().selectLast();
            commandField.clear();

        // TODO 1.5 If the process cannot be started (IOException), show an error with the message.
        }catch (IOException e){
            showError(e.getMessage());
        }

    }

    // =====================================================================
    // Level 2: control processes
    // =====================================================================

    private void stopSelectedProcess(boolean force){
        ManagedProcess managedProcess = processTable.getSelectionModel().getSelectedItem();

        if(managedProcess == null || !managedProcess.getProcess().isAlive()){
            showWarning("The process is not selected or is not alive");
            return;
        }

        if (force){
            managedProcess.getProcess().destroyForcibly();
        } else {
            managedProcess.getProcess().destroy();
        }

    }

    /** "Stop" button: asks the selected process to finish. */
    @FXML
    private void onStop() {
        // TODO 2.1 Get the selected row. If there is no selection, or the process is not alive,
        //          show a warning. Otherwise, ask the process to finish (destroy).
        stopSelectedProcess(false);

    }

    /** "Force kill" button: kills the selected process immediately. */
    @FXML
    private void onForceKill() {
        // TODO 2.2 Same as onStop(), but killing the process (destroyForcibly).
        //          Tip: move the common code of 2.1 and 2.2 to a private method.
        stopSelectedProcess(true);

    }

    /** "Refresh status" button: updates the Status and Exit code columns. */
    @FXML
    private void onRefresh() {
        // TODO 2.3 For every process of the list that is not marked as finished:
        //          if it is not alive anymore, call markFinished() with its exit code.
        processes.forEach(p-> {
            if (!p.isFinished() && !p.getProcess().isAlive()) {
                p.markFinished(p.getProcess().exitValue());
            }
        });
    }

    /** "Show log" button: shows the content of the log file of the selected process. */
    @FXML
    private void onShowLog() {
        // TODO 2.4 Get the selected row (warning if there is none).
        //          Read its log file and show the content in logArea.
        //          Update logLabel with the PID and the path of the file.
        //          Tip: new String(Files.readAllBytes(path), Charset.defaultCharset())
        ManagedProcess selectedRow = processTable.getSelectionModel().getSelectedItem();

        if (selectedRow == null){
            showWarning("No row selected");
            return;
        }

        try {
            Path path = selectedRow.getLogFile();
            String logContent = new String(Files.readAllBytes(path), Charset.defaultCharset());

            logArea.setText(logContent);
            logLabel.setText("PID: " + selectedRow.getPid() + '-' + path.toString());

        }catch (IOException e){
            showError(e.getMessage());
        }

    }

    /** "Remove finished" button: removes the finished processes from the table. */
    @FXML
    private void onRemoveFinished() {
        // TODO 2.5 Refresh the status first, then remove the finished processes from the list.
        onRefresh();

        processes.removeIf(ManagedProcess::isFinished);
    }

    /** Called when the window is closed (see TaskManagerApp). */
    public void shutdown() {
        // TODO 2.6 If there are processes still alive, ask the user (Alert of type CONFIRMATION)
        //          whether to stop them before closing. If the answer is YES, stop all of them.
        boolean hasAliveProcess = processes.stream().anyMatch(p->p.getProcess().isAlive());

        if (hasAliveProcess){
            Alert alert = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "One or more processes are still active. " +
                            "Would you like to terminate them before closing the application?",
                    ButtonType.YES,
                    ButtonType.NO);
            Optional<ButtonType> response = alert.showAndWait();

            if ( response.isPresent() && response.get() == ButtonType.YES){
                processes.stream().filter(p->p.getProcess()
                        .isAlive()).forEach(p->p.getProcess().destroy());
            }
        }
    }

    // =====================================================================
    // Level 4 (optional): system processes with ProcessHandle
    // =====================================================================

    /** "Refresh" button of the second tab. */
    @FXML
    private void onRefreshSystem() {
        // TODO 4.1 Use ProcessHandle.allProcesses() to get the processes whose command
        //          contains the text of filterField (ignoring case), sorted by PID.
        //          Put them in systemProcesses and show how many were found in systemInfoLabel.

        String filter = filterField.getText().toLowerCase();

        List<ProcessHandle> matchingProcesses =  ProcessHandle.allProcesses()
                .filter(p-> commandOf(p).toLowerCase().contains(filter))
                .sorted(Comparator.comparingLong(ProcessHandle::pid)).toList();

        systemProcesses.setAll(matchingProcesses);
        systemInfoLabel.setText(matchingProcesses.size() + " processes found");

    }

    /** "Kill selected" button of the second tab. */
    @FXML
    private void onKillSystem() {
        // TODO 4.2 Get the selected process. Do not allow killing the task manager itself.
        //          Ask for confirmation and call destroy(). If it returns false, show an error.
        //          Finally, refresh the list.
        ProcessHandle selected = systemTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showWarning("No process selected");
            return;
        }

        if (selected.pid() == ProcessHandle.current().pid()) {
            showWarning("You cannot kill the task manager itself");
            return;
        }

        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Kill process " + selected.pid() + "?",
                ButtonType.YES,
                ButtonType.NO);
        Optional<ButtonType> response = alert.showAndWait();

        if (response.isPresent() && response.get() == ButtonType.YES) {
            if (!selected.destroy()) {
                showError("Could not kill process " + selected.pid());
            }
            onRefreshSystem();
        }
    }

    // =====================================================================
    // Helpers (complete)
    // =====================================================================

    /** Command of a process, or an empty string if the OS does not give it. */
    private static String commandOf(ProcessHandle handle) {
        return handle.info().command().orElse("");
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

}
