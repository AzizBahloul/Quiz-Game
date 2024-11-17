package com.example.quizgame.client;

import com.exemple.quizgame.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.util.Duration;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class QuizGameApp extends Application {
    private QuizGameGrpc.QuizGameBlockingStub stub;
    private String currentPlayer;
    private Quiz currentQuiz;
    private int currentQuestionIndex = 0;
    private GetQuizResponse quizResponse;
    
    // Define theme colors
    private static final String DARK_BACKGROUND = "#1E1E1E";
    private static final String SECONDARY_DARK = "#252526";
    private static final String ACCENT_BLUE = "#007ACC";
    private static final String TEXT_COLOR = "#FFFFFF";
    private static final String HOVER_BLUE = "#0098FF";

    private void applyDarkTheme(Scene scene) {
        String css = getClass().getResource("/styles.css").toExternalForm();
        scene.getStylesheets().add(css);
        scene.setFill(javafx.scene.paint.Color.valueOf(DARK_BACKGROUND));
    }

    private void styleControl(Control control) {
        if (control instanceof Button) {
            control.setStyle(
                "-fx-background-color: " + ACCENT_BLUE + ";" +
                "-fx-text-fill: " + TEXT_COLOR + ";" +
                "-fx-font-size: 14px;"
            );
        } else if (control instanceof TextField) {
            control.setStyle(
                "-fx-background-color: " + SECONDARY_DARK + ";" +
                "-fx-text-fill: " + TEXT_COLOR + ";" +
                "-fx-border-color: " + ACCENT_BLUE + ";" +
                "-fx-border-radius: 3px;"
            );
        } else if (control instanceof Label) {
            control.setStyle(
                "-fx-text-fill: " + TEXT_COLOR + ";"
            );
        } else if (control instanceof RadioButton) {
            control.setStyle(
                "-fx-text-fill: " + TEXT_COLOR + ";"
            );
        }
    }

    private void applyEntranceAnimation(Node control) {
        // Slide and fade in with updated colors
        TranslateTransition translate = new TranslateTransition(Duration.millis(1000), control);
        translate.setFromX(-50);
        translate.setToX(0);

        FadeTransition fade = new FadeTransition(Duration.millis(1000), control);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(1000), control);
        scale.setFromX(0.8);
        scale.setFromY(0.8);
        scale.setToX(1);
        scale.setToY(1);

        ParallelTransition parallel = new ParallelTransition(translate, fade, scale);
        parallel.play();
    }

    @Override
    public void start(Stage primaryStage) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();
        stub = QuizGameGrpc.newBlockingStub(channel);

        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setStyle("-fx-background-color: " + DARK_BACKGROUND + ";");

        Scene scene = new Scene(mainLayout, 600, 400);
        applyDarkTheme(scene);
        
        primaryStage.setTitle("Quiz Game");
        primaryStage.setScene(scene);
        primaryStage.setFullScreen(true); // Set the window to full screen
        primaryStage.setFullScreenExitHint(""); // Remove the full screen exit hint
        primaryStage.show();

        showPlayerRegistration(mainLayout, primaryStage);

        primaryStage.setOnCloseRequest(e -> {
            channel.shutdown();
            Platform.exit();
        });
    }

    private void showPlayerRegistration(VBox mainLayout, Stage primaryStage) {
        // Read and display ASCII art
        String asciiArt = "";
        try {
            asciiArt = new String(Files.readAllBytes(Paths.get("ascii_art_fixed.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Label asciiLabel = new Label(asciiArt);
        asciiLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 8px; -fx-text-fill: " + TEXT_COLOR + ";");
        
        // Apply entrance animation to ASCII art
        applyEntranceAnimation(asciiLabel);

        Label titleLabel = new Label("Welcome to Quiz Game!");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_COLOR + ";");

        TextField playerNameField = new TextField();
        playerNameField.setPromptText("Enter your name");
        playerNameField.setMaxWidth(200);
        styleControl(playerNameField);

        Button startButton = new Button("Start Game");
        styleControl(startButton);
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

        // Add components to layout
        mainLayout.getChildren().addAll(asciiLabel, titleLabel, playerNameField, startButton);

        // Apply entrance animations to all components
        applyEntranceAnimation(titleLabel);
        applyEntranceAnimation(playerNameField);
        applyEntranceAnimation(startButton);
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
        questionLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: " + TEXT_COLOR + ";");
        Label questionText = new Label(currentQuiz.getQuestion());
        questionText.setWrapText(true);
        questionText.setStyle("-fx-font-size: 16px; -fx-text-fill: " + TEXT_COLOR + ";");

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
            styleControl(rb);
            answersBox.getChildren().add(rb);
        }

        // Submit button
        Button submitButton = new Button("Submit Answer");
        styleControl(submitButton);
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

        // Apply entrance animations to all components
        applyEntranceAnimation(questionLabel);
        applyEntranceAnimation(questionText);
        applyEntranceAnimation(answersBox);
        applyEntranceAnimation(submitButton);
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
        styleControl(nextButton);
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
        progressBar.setStyle("-fx-accent: " + ACCENT_BLUE + ";");

        mainLayout.getChildren().addAll(resultLabel, progressBar, nextButton);

        // Add fade transition for the entire layout
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), mainLayout);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void showFinalScore(VBox mainLayout) {
        mainLayout.getChildren().clear();

        try {
            GetPlayerScoresRequest scoresRequest = GetPlayerScoresRequest.newBuilder().build();
            GetPlayerScoresResponse scoresResponse = stub.getPlayerScores(scoresRequest);

            Label finalLabel = new Label("Quiz Completed!");
            finalLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_COLOR + ";");

            VBox scoresBox = new VBox(10);
            scoresBox.setAlignment(Pos.CENTER);

            for (Player player : scoresResponse.getPlayersList()) {
                Label scoreLabel = new Label(player.getPlayerName() + ": " + player.getScore() + " points");
                scoreLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: " + TEXT_COLOR + ";");
                scoresBox.getChildren().add(scoreLabel);
            }

            Button exitButton = new Button("Exit Game");
            styleControl(exitButton);
            exitButton.setOnAction(e -> Platform.exit());

            mainLayout.getChildren().addAll(finalLabel, scoresBox, exitButton);

            // Add fade transition for the entire layout
            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), mainLayout);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
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