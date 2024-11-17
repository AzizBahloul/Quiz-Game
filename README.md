# Quiz Game using gRPC and JavaFX

A multiplayer quiz game implementation using gRPC for client-server communication and JavaFX for the graphical user interface. Players can register, answer questions, and compete for the highest score.

## Project Structure

```
src/main/
├── java/
│   └── com/
│       └── example/
│           └── quizgame/
│               ├── client/
│               │   ├── QuizGameApp.java    # JavaFX GUI client
│               │   └── QuizGameClient.java  # Command-line client
│               └── server/
│                   ├── QuizGameServer.java  # gRPC server
│                   └── ServiceQuizGame.java # Service implementation
└── proto/
    └── quizgame/
        └── Quiz.proto                      # Protocol buffer definitions
```

## Prerequisites

- Java JDK 11 or higher
- Maven 3.6 or higher
- JavaFX (included in JDK 8, separate download for JDK 11+)
- gRPC dependencies (handled by Maven)

## Dependencies

Add these to your `pom.xml`:

```xml
<dependencies>
    <!-- gRPC dependencies -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-netty-shaded</artifactId>
        <version>1.42.0</version>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-protobuf</artifactId>
        <version>1.42.0</version>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-stub</artifactId>
        <version>1.42.0</version>
    </dependency>
    
    <!-- JavaFX dependencies -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>16</version>
    </dependency>
    
    <!-- Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>1.7.32</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.2.6</version>
    </dependency>
</dependencies>
```

## File Descriptions

### Proto File (Quiz.proto)
Defines the service contract and message types for the quiz game:
- Service definitions for player registration, quiz retrieval, and gameplay
- Message definitions for Quiz, Player, and various request/response types
- Located in `src/main/proto/quizgame/Quiz.proto`

### Server-side Files

#### QuizGameServer.java
- Main server class that starts the gRPC server
- Initializes the server on port 50051
- Located in `src/main/java/com/example/quizgame/server/QuizGameServer.java`

#### ServiceQuizGame.java
- Implements the quiz game service logic
- Handles player registration
- Manages quiz questions and answers
- Tracks player scores
- Located in `src/main/java/com/example/quizgame/server/ServiceQuizGame.java`

### Client-side Files

#### QuizGameApp.java
- JavaFX-based graphical client
- Provides user interface for:
  - Player registration
  - Question display
  - Answer selection
  - Score tracking
- Located in `src/main/java/com/example/quizgame/client/QuizGameApp.java`

#### QuizGameClient.java
- Command-line client implementation
- Useful for testing and demonstration
- Located in `src/main/java/com/example/quizgame/client/QuizGameClient.java`

## How to Run

### 1. Generate gRPC Code
First, generate the gRPC code from the proto file:
```bash
mvn clean compile
```

### 2. Start the Server
```bash
mvn exec:java -Dexec.mainClass="com.example.quizgame.server.QuizGameServer"
```

### 3. Run the Client
You can run either the GUI client or the command-line client:

For GUI client:
```bash
mvn exec:java -Dexec.mainClass="com.example.quizgame.client.QuizGameApp"
```

For command-line client:
```bash
mvn exec:java -Dexec.mainClass="com.example.quizgame.client.QuizGameClient"
```

## Game Flow

1. Start the server
2. Launch the client (GUI or command-line)
3. Register players (up to 2 players)
4. Answer quiz questions
5. View final scores

## Features

- Multiplayer support
- Real-time score tracking
- Multiple-choice questions
- Immediate feedback on answers
- Final score display
- Error handling and validation

## Common Issues and Solutions

1. **Server Already Running**
   - Error: "Address already in use"
   - Solution: Ensure no other instance of the server is running on port 50051

2. **JavaFX Not Found**
   - Error: "Error: JavaFX runtime components are missing"
   - Solution: Ensure JavaFX dependencies are properly included in your pom.xml

3. **Connection Refused**
   - Error: "io.grpc.StatusRuntimeException: UNAVAILABLE: Network closed"
   - Solution: Make sure the server is running before starting the client

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.