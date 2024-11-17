package com.example.quizgame.client;

import com.exemple.quizgame.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.util.Duration;

public class QuizGameApp extends Application {
    private QuizGameGrpc.QuizGameBlockingStub stub;
    private String currentPlayer;
    private Quiz currentQuiz;
    private int currentQuestionIndex = 0;
    private GetQuizResponse quizResponse;

    @Override
    public void start(Stage primaryStage) {
        // Initialize gRPC channel and stub
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();
        stub = QuizGameGrpc.newBlockingStub(channel);

        // Create the main layout
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.CENTER);

        // Player registration section
        showPlayerRegistration(mainLayout, primaryStage);

        // Set up the primary stage
        Scene scene = new Scene(mainLayout, 600, 400);
        primaryStage.setTitle("Quiz Game");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Handle cleanup when the window is closed
        primaryStage.setOnCloseRequest(e -> {
            channel.shutdown();
            Platform.exit();
        });
    }

    private void showPlayerRegistration(VBox mainLayout, Stage primaryStage) {
        Label titleLabel = new Label("Welcome to Quiz Game!");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField playerNameField = new TextField();
        playerNameField.setPromptText("Enter your name");
        playerNameField.setMaxWidth(200);

        Button startButton = new Button("Start Game");
        startButton.setStyle("-fx-font-size: 16px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        startButton.setOnAction(e -> {
            String playerName = playerNameField.getText().trim();
            if (!playerName.isEmpty()) {
                try {
                    registerPlayer(playerName);
                    currentPlayer = playerName;
                    loadQuizzes(mainLayout, primaryStage);
                } catch (Exception ex) {
                    showError("Registration Error", ex.getMessage());
                }
            } else {
                showError("Invalid Input", "Please enter your name");
            }
        });

        // Handle Enter key press for player name field
        playerNameField.setOnAction(e -> startButton.fire());

        mainLayout.getChildren().addAll(titleLabel, playerNameField, startButton);
    }

    private void loadQuizzes(VBox mainLayout, Stage primaryStage) {
        try {
            // Fetch all quizzes
            GetQuizRequest quizRequest = GetQuizRequest.newBuilder().build();
            quizResponse = stub.getQuiz(quizRequest);
            
            if (!quizResponse.getQuizList().isEmpty()) {
                currentQuestionIndex = 0;
                showQuestion(mainLayout, primaryStage);
            }
        } catch (Exception e) {
            showError("Error", "Failed to load quizzes: " + e.getMessage());
        }
    }

    private void showQuestion(VBox mainLayout, Stage primaryStage) {
        // Clear previous content
        mainLayout.getChildren().clear();

        currentQuiz = quizResponse.getQuiz(currentQuestionIndex);

        // Question display
        Label questionLabel = new Label("Question " + (currentQuestionIndex + 1) + "/" + quizResponse.getQuizCount());
        questionLabel.setStyle("-fx-font-size: 18px;");
        Label questionText = new Label(currentQuiz.getQuestion());
        questionText.setWrapText(true);
        questionText.setStyle("-fx-font-size: 16px;");

        // Answer options
        ToggleGroup answerGroup = new ToggleGroup();
        VBox answersBox = new VBox(10);
        RadioButton[] answerButtons = new RadioButton[4];
        
        answerButtons[0] = new RadioButton(currentQuiz.getAnswer1());
        answerButtons[1] = new RadioButton(currentQuiz.getAnswer2());
        answerButtons[2] = new RadioButton(currentQuiz.getAnswer3());
        answerButtons[3] = new RadioButton(currentQuiz.getAnswer4());

        for (RadioButton rb : answerButtons) {
            rb.setToggleGroup(answerGroup);
            answersBox.getChildren().add(rb);
        }

        // Submit button
        Button submitButton = new Button("Submit Answer");
        submitButton.setStyle("-fx-font-size: 16px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        submitButton.setOnAction(e -> {
            RadioButton selectedButton = (RadioButton) answerGroup.getSelectedToggle();
            if (selectedButton != null) {
                int selectedAnswer = answersBox.getChildren().indexOf(selectedButton) + 1;
                submitAnswer(selectedAnswer, mainLayout, primaryStage);
            } else {
                showError("Invalid Selection", "Please select an answer");
            }
        });

        // Handle Enter key press for answer selection
        for (RadioButton rb : answerButtons) {
            rb.setOnAction(e -> submitButton.fire());
        }

        mainLayout.getChildren().addAll(questionLabel, questionText, answersBox, submitButton);
    }

    private void submitAnswer(int answer, VBox mainLayout, Stage primaryStage) {
        try {
            PlayRequest playRequest = PlayRequest.newBuilder()
                    .setPlayerName(currentPlayer)
                    .setQuizId(currentQuiz.getId())
                    .setAnswer(answer)
                    .build();

            PlayResponse playResponse = stub.play(playRequest);

            // Show result
            showAnswerResult(playResponse.getCorrectAnswer() == answer, playResponse.getCorrectAnswer(), mainLayout, primaryStage);
        } catch (Exception e) {
            showError("Error", "Failed to submit answer: " + e.getMessage());
        }
    }

    private void showAnswerResult(boolean correct, int correctAnswer, VBox mainLayout, Stage primaryStage) {
        mainLayout.getChildren().clear();

        Label resultLabel = new Label(correct ? "Correct!" : "Incorrect!");
        resultLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + (correct ? "green;" : "red;"));

        // Add fade transition for result label
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), resultLabel);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();

        Button nextButton = new Button("Next Question");
        nextButton.setStyle("-fx-font-size: 16px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        nextButton.setOnAction(e -> {
            currentQuestionIndex++;
            if (currentQuestionIndex < quizResponse.getQuizCount()) {
                showQuestion(mainLayout, primaryStage);
            } else {
                showFinalScore(mainLayout);
            }
        });

        // Add progress bar
        ProgressBar progressBar = new ProgressBar((double) currentQuestionIndex / quizResponse.getQuizCount());
        progressBar.setStyle("-fx-accent: #4CAF50;");

        mainLayout.getChildren().addAll(resultLabel, progressBar, nextButton);
    }

    private void showFinalScore(VBox mainLayout) {
        mainLayout.getChildren().clear();

        try {
            GetPlayerScoresRequest scoresRequest = GetPlayerScoresRequest.newBuilder().build();
            GetPlayerScoresResponse scoresResponse = stub.getPlayerScores(scoresRequest);

            Label finalLabel = new Label("Quiz Completed!");
            finalLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

            VBox scoresBox = new VBox(10);
            scoresBox.setAlignment(Pos.CENTER);

            for (Player player : scoresResponse.getPlayersList()) {
                Label scoreLabel = new Label(player.getPlayerName() + ": " + player.getScore() + " points");
                scoreLabel.setStyle("-fx-font-size: 16px;");
                scoresBox.getChildren().add(scoreLabel);
            }

            Button exitButton = new Button("Exit Game");
            exitButton.setStyle("-fx-font-size: 16px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
            exitButton.setOnAction(e -> Platform.exit());

            mainLayout.getChildren().addAll(finalLabel, scoresBox, exitButton);
        } catch (Exception e) {
            showError("Error", "Failed to load final scores: " + e.getMessage());
        }
    }

    private void registerPlayer(String playerName) {
        RegisterPlayerRequest request = RegisterPlayerRequest.newBuilder()
                .setPlayerName(playerName)
                .build();
        stub.registerPlayer(request);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}