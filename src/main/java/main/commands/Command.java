package main.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

/**
 * Interface for all commands in the system.
 */
public interface Command {
    /**
     * Executes the command logic.
     * Receives the list to append results (response JSONs).
     *
     * @param outputs The list of JSON output nodes.
     */
    void execute(final List<ObjectNode> outputs);
}