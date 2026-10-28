package psp.taskmanager;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * A process launched by the application, with the data shown in the table.
 * The status and exit code are JavaFX properties, so the table updates itself when they change.
 */
public class ManagedProcess {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Process process;
    private final String command;
    private final String startTime;
    private final Path logFile;

    private final StringProperty status = new SimpleStringProperty("RUNNING");
    private final StringProperty exitCode = new SimpleStringProperty("-");

    public ManagedProcess(Process process, String command, Path logFile) {
        this.process = process;
        this.command = command;
        this.logFile = logFile;
        this.startTime = LocalTime.now().format(TIME_FORMAT);
    }

    public Process getProcess() {
        return process;
    }

    public long getPid() {
        return process.pid();
    }

    public String getCommand() {
        return command;
    }

    public String getStartTime() {
        return startTime;
    }

    public Path getLogFile() {
        return logFile;
    }

    /** Changes the status to FINISHED and stores the exit code. */
    public void markFinished(int code) {
        status.set("FINISHED");
        exitCode.set(String.valueOf(code));
    }

    public boolean isFinished() {
        return "FINISHED".equals(status.get());
    }

    public ReadOnlyStringProperty statusProperty() {
        return status;
    }

    public ReadOnlyStringProperty exitCodeProperty() {
        return exitCode;
    }
}
