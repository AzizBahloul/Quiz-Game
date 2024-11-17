package com.example.quizgame.server;

import com.exemple.quizgame.proto.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ServiceQuizGame extends QuizGameGrpc.QuizGameImplBase {
    private static final Logger logger = LoggerFactory.getLogger(ServiceQuizGame.class);
    private final List<Player> players = new ArrayList<>();
    private final List<Quiz> quizzes = new ArrayList<>();

    public ServiceQuizGame() {
        initializeQuizQuestions();
    }

    private void initializeQuizQuestions() {
        quizzes.add(Quiz.newBuilder()
                .setId(1)
                .setQuestion("What is the capital of France?")
                .setAnswer1("Berlin")
                .setAnswer2("Madrid")
                .setAnswer3("Paris")
                .setAnswer4("Rome")
                .setCorrectAnswer(3)
                .build());

        quizzes.add(Quiz.newBuilder()
                .setId(2)
                .setQuestion("Which programming language is this project using?")
                .setAnswer1("Python")
                .setAnswer2("Java")
                .setAnswer3("C++")
                .setAnswer4("JavaScript")
                .setCorrectAnswer(2)
                .build());

        quizzes.add(Quiz.newBuilder()
                .setId(3)
                .setQuestion("What is 2 + 2?")
                .setAnswer1("3")
                .setAnswer2("4")
                .setAnswer3("5")
                .setAnswer4("6")
                .setCorrectAnswer(2)
                .build());

        quizzes.add(Quiz.newBuilder()
                .setQuestion("What is the capital of France?")
                .setAnswer1("Berlin")
                .setAnswer2("Madrid")
                .setAnswer3("Paris")
                .setAnswer4("Rome")
                .setCorrectAnswer(3)
                .build());

        quizzes.add(Quiz.newBuilder()
                .setQuestion("What is the largest planet in our solar system?")
                .setAnswer1("Earth")
                .setAnswer2("Jupiter")
                .setAnswer3("Mars")
                .setAnswer4("Saturn")
                .setCorrectAnswer(2)
                .build());

        quizzes.add(Quiz.newBuilder()
                .setQuestion("What is the chemical symbol for water?")
                .setAnswer1("H2O")
                .setAnswer2("O2")
                .setAnswer3("CO2")
                .setAnswer4("HO")
                .setCorrectAnswer(1)
                .build());

        logger.info("Initialized {} quiz questions", quizzes.size());
    }

    @Override
    public void registerPlayer(RegisterPlayerRequest request, StreamObserver<RegisterPlayerResponse> responseObserver) {
        try {
            String playerName = request.getPlayerName();
            logger.info("Registering player: {}", playerName);

            // Validate player name
            if (playerName == null || playerName.trim().isEmpty()) {
                responseObserver.onError(
                        Status.INVALID_ARGUMENT
                                .withDescription("Player name cannot be empty")
                                .asException()
                );
                return;
            }

            // Check for duplicate player
            boolean playerExists = players.stream()
                    .anyMatch(p -> p.getPlayerName().equals(playerName));

            if (playerExists) {
                responseObserver.onError(
                        Status.ALREADY_EXISTS
                                .withDescription("Player already registered: " + playerName)
                                .asException()
                );
                return;
            }

            // Create and add new player
            Player player = Player.newBuilder()
                    .setPlayerName(playerName)
                    .setScore(0)
                    .build();
            players.add(player);

            // Send response
            RegisterPlayerResponse response = RegisterPlayerResponse.newBuilder()
                    .setPlayer(player)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("New player registered: {}", playerName);

        } catch (Exception e) {
            logger.error("Error registering player: ", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .withCause(e)
                            .asException()
            );
        }
    }

    @Override
    public void getQuiz(GetQuizRequest request, StreamObserver<GetQuizResponse> responseObserver) {
        try {
            GetQuizResponse response = GetQuizResponse.newBuilder()
                    .addAllQuiz(quizzes)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error retrieving quizzes: ", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .withCause(e)
                            .asException()
            );
        }
    }

    @Override
    public void getQuestion(GetQuestionRequest request, StreamObserver<GetQuestionResponse> responseObserver) {
        Quiz quiz = quizzes.stream()
                .filter(q -> q.getId() == request.getQuizId())
                .findFirst()
                .orElse(null);

        if (quiz != null) {
            GetQuestionResponse response = GetQuestionResponse.newBuilder()
                    .setQuiz(quiz)
                    .build();
            responseObserver.onNext(response);
        } else {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Quiz not found with ID: " + request.getQuizId())
                            .asException()
            );
        }
        responseObserver.onCompleted();
    }

    @Override
    public void play(PlayRequest request, StreamObserver<PlayResponse> responseObserver) {
        Player player = players.stream()
                .filter(p -> p.getPlayerName().equals(request.getPlayerName()))
                .findFirst()
                .orElse(null);

        Quiz quiz = quizzes.stream()
                .filter(q -> q.getId() == request.getQuizId())
                .findFirst()
                .orElse(null);

        if (player != null && quiz != null) {
            int score = player.getScore();
            if (quiz.getCorrectAnswer() == request.getAnswer()) {
                score++;
            }

            // Create a final variable for the updated player
            final Player updatedPlayer = player.toBuilder().setScore(score).build();

            // Update player score
            players.removeIf(p -> p.getPlayerName().equals(updatedPlayer.getPlayerName()));
            players.add(updatedPlayer);

            PlayResponse response = PlayResponse.newBuilder()
                    .setPlayer(updatedPlayer)
                    .setCorrectAnswer(quiz.getCorrectAnswer())
                    .build();
            responseObserver.onNext(response);
        } else {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Player or Quiz not found")
                            .asException()
            );
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getPlayerScores(GetPlayerScoresRequest request, StreamObserver<GetPlayerScoresResponse> responseObserver) {
        GetPlayerScoresResponse response = GetPlayerScoresResponse.newBuilder()
                .addAllPlayers(players)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
