# OOP Project - Bug Tracker System

## Project Overview
This project simulates the backend of a Bug Tracker System. It reads a linear stream of commands from a JSON file handling users, tickets, and milestones and produces an output log reflecting the system's state.

## Application Architecture
I structured the project to strictly separate data, logic, and command processing:

* **`main.model`**: Holds the entities (`User`, `Ticket`, `Milestone`). I used inheritance here (e.g., `Bug` extends `Ticket`) to share common fields while allowing specific behaviors.
* **`main.commands`**: Contains the business logic. Each action is an isolated class implementing the `Command` interface.
* **`main.database`**: The single source of truth for the application state.
* **`main.visitor`**: Dedicated to generating analytics and reports without cluttering the model classes.

## Design Patterns & Implementation Details
I relied on several established patterns to keep the code modular, testable, and easy to extend. Here is a breakdown of why I chose them and how they interact:

### 1. Singleton Pattern (`Database`)
* **The Context:** The application processes commands sequentially. If command A adds a ticket, command B needs to see it immediately.
* **The Solution:** I implemented the `Database` as a Singleton. This ensures there is exactly one instance of the storage layer during the program's lifecycle, acting as a persistent shared memory across all commands.
* **Implementation Note:** Instead of calling `getInstance()` randomly throughout the code, I access it once in the `CommandFactory` and inject it into commands, keeping dependencies clear.

### 2. Command Pattern (`main.commands`)
* **The Context:** The input is a dynamic list of strings ("addComment", "changeStatus", etc.).
* **The Solution:** I encapsulated each request as an object implementing the `Command` interface. This allows each command to be its own "island" of logic.
* **Why:** It decouples the input parsing from the execution. It makes the code easier to debug (if "addComment" fails, I know exactly which file to check) and allows the system to be easily extensible.

### 3. Factory Pattern (`CommandFactory`)
* **The Context:** We need to convert a raw string (like "viewTickets") into a specific Java Object (`ViewTicketsCommand`).
* **The Solution:** The `CommandFactory` acts as a dispatcher. It centralizes the `switch` logic and handles the **Dependency Injection**, passing the `Database` instance and `InputData` to the new commands.
* **Why:** This keeps the main execution loop clean. The main class doesn't need to know how to construct a command or what dependencies it requires; the factory handles that complexity.

### 4. Visitor Pattern (`main.visitor`)
* **The Context:** The system requires complex reports (Performance, Customer Impact, Risk) that calculate scores based on specific ticket types.
* **The Solution:** I used the Visitor pattern to extract the calculation algorithms from the object structure. The `PerformanceStatsVisitor` "visits" a ticket, identifies its type (Bug/Feature/UI), and performs the specific math required for the report.
* **Why:** This adheres to the Single Responsibility Principle. Model classes should store data, not calculate standard deviations or weighted scores. It also makes adding new reports trivial—I just create a new Visitor without touching the existing Ticket classes.


### Java Streams Usage
Throughout the application, I replaced verbose `for` loops with **Java Streams** to handle data collections. Here is one example of how I utilized Streams effectively:

* **Efficient Filtering:**
  Instead of nesting multiple `if` statements inside a loop, I chained `.filter()` operations.
    * *Example:* In the Performance Report, extracting the relevant tickets for a developer became a clean pipeline:
        ```java
        List<Ticket> devTickets = allTickets.stream()
            .filter(t -> "CLOSED".equals(t.getStatus()))      
            .filter(t -> dev.equals(t.getAssignee()))        
            .collect(Collectors.toList());
        ```

* **Data Transformation:**
  I used `.map()` to extract specific fields or cast objects safely.
    * *Example:* Converting a list of `User` objects directly into a list of specific `Developer` instances for the team reports.

* **Sorting:**
  Streams made sorting complex lists trivial. Instead of writing custom comparators, I used concise method references like `.sorted(Comparator.comparing(Developer::getUsername))`.

### Safe Type Casting (`Optional`)
In a system with different user roles (`Manager`, `Developer`, `Employee`), I often needed to filter specific types from a mixed list. Using `instanceof` checks and manual casting (`(Developer) user`) is error-prone.

* **The Solution:**
  I implemented a **polymorphic** approach using `Optional`. The base `User` class has helper methods like `isDeveloper()` that return `Optional.empty()` by default.
    The `Developer` subclass overrides this method to return `Optional.of(this)`.
* **The Result:**
  This allowed me to write clean, safe streams without explicit casts.
    ```java
    // Instead of checking types manually, I ask the object:
    List<Developer> devs = users.stream()
        .map(User::isDeveloper)       // Try to view as Developer
        .flatMap(Optional::stream)    // Keep only if present
        .collect(Collectors.toList());
    ```