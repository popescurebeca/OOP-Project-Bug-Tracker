package main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import main.commands.Command;
import main.commands.CommandFactory;
import main.database.Database;
import main.utils.InputData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * main.App represents the main application logic that processes input commands,
 * generates outputs, and writes them to a file
 */
public class App {
    private App() {
    }

    private static final String INPUT_USERS_FIELD = "input/database/users.json";

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final ObjectWriter WRITER =
            new ObjectMapper().writer().withDefaultPrettyPrinter();

    /**
     * Runs the application: reads commands from an input file,
     * processes them, generates results, and writes them to an output file
     *
     * @param inputPath path to the input file containing commands
     * @param outputPath path to the file where results should be written
     */
    public static void run(final String inputPath, final String outputPath) throws IOException {
        // feel free to change this if needed
        // however keep 'outputs' variable name to be used for writing
        List<ObjectNode> outputs = new ArrayList<>();

        /*
            TODO 1 :
            Load initial user data and commands. we strongly recommend using jackson library.
            you can use the reading from hw1 as a reference.
            however you can use some of the more advanced features of
            jackson library, available here: https://www.baeldung.com/jackson-annotations
        */
        InputData[] usersInput = MAPPER.readValue(new File(INPUT_USERS_FIELD), InputData[].class);
        Database.getInstance().reset();
        Database.getInstance().loadUsers(Arrays.asList(usersInput));

        // TODO 2: process commands.

        InputData[] commandsInput = MAPPER.readValue(new File(inputPath), InputData[].class);

        for (InputData input : commandsInput) {
            // Folosim Factory pentru a crea comanda corectă
            Command command = CommandFactory.createCommand(input);

            if (command != null) {
                // Executăm comanda și îi dăm lista de outputs ca să poată scrie în ea
                command.execute(outputs);
            }
        }

        // TODO 3: create objectnodes for output, add them to outputs list.

        // DO NOT CHANGE THIS SECTION IN ANY WAY
        try {
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();
            WRITER.withDefaultPrettyPrinter().writeValue(outputFile, outputs);
        } catch (IOException e) {
            System.out.println("error writing to output file: " + e.getMessage());
        }
    }
}
