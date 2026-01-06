package main.commands;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.utils.InputData;
import main.commands.*;

import java.util.List;

public class CommandFactory {
    public static Command createCommand(InputData data) {
        String type = data.getCommand();
        return switch (type) {
            case "reportTicket" -> new ReportTicketCommand(data);
            case "viewTickets" -> new ViewTicketsCommand(data);
            default -> null;
        };
    }

    private static Command UnknownCommand(InputData data) {
        return new Command() {
            @Override
            public void execute(List<ObjectNode> outputs) {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode errorNode = mapper.createObjectNode();
                errorNode.put("status", "error");
                errorNode.put("message", "Unknown command: " + data.getCommand());
                outputs.add(errorNode);
            }
        };
    }
}