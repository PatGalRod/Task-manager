package psp.taskmanager;

import java.util.ArrayList;
import java.util.List;

/** Utility to split a command line typed by the user into program + arguments. */
public final class CommandLine {

    private CommandLine() {
    }

    /**
     * Splits a command line by spaces. Text between double quotes is kept as one argument.
     * <p>Example: {@code ping -c 3 "my host"} returns {@code [ping, -c, 3, my host]}.
     */
    public static List<String> split(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.trim().toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (!current.isEmpty()) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }
        return parts;
    }
}
