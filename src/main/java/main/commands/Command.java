package main.commands;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

public interface Command {
    // Primește lista pentru a putea adăuga rezultate (JSON-uri de răspuns)
    void execute(List<ObjectNode> outputs);
}