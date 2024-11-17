package com.example.quizgame.client;

import com.exemple.quizgame.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;

public class QuizGameClient {

    public static void main(String[] args) {
        // Create a channel to connect to the server
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        // Create a stub to use the service
        QuizGameGrpc.QuizGameBlockingStub stub = QuizGameGrpc.newBlockingStub(channel);

        Scanner scanner = new Scanner(System.in);

        // Register two players
        System.out.print("Enter name for Player 1: ");
        String player1Name = scanner.nextLine();
        registerPlayer(stub, player1Name);

        System.out.print("Enter name for Player 2: ");
        String player2Name = scanner.nextLine();
        registerPlayer(stub, player2Name);

        // Retrieve the list of quizzes
        GetQuizRequest quizRequest = GetQuizRequest.newBuilder().build();
        GetQuizResponse quizResponse = stub.getQuiz(quizRequest);

        // Play the quiz for both players
        playQuiz(stub, scanner, player1Name, quizResponse);
        playQuiz(stub, scanner, player2Name, quizResponse);

        // Get and display player scores
        GetPlayerScoresRequest scoresRequest = GetPlayerScoresRequest.newBuilder().build();
        GetPlayerScoresResponse scoresResponse = stub.getPlayerScores(scoresRequest);

        System.out.println("\nScores:");
        for (Player player : scoresResponse.getPlayersList()) {
            System.out.println(player.getPlayerName() + ": " + player.getScore());
        }

        // Shutdown channel and scanner
        channel.shutdown();
        scanner.close();
    }

    private static void registerPlayer(QuizGameGrpc.QuizGameBlockingStub stub, String playerName) {
        RegisterPlayerRequest registerRequest = RegisterPlayerRequest.newBuilder()
                .setPlayerName(playerName)
                .build();
        stub.registerPlayer(registerRequest);
    }

    private static void playQuiz(QuizGameGrpc.QuizGameBlockingStub stub, Scanner scanner, String playerName, GetQuizResponse quizResponse) {
        for (int i = 0; i < quizResponse.getQuizCount(); i++) {
            Quiz quiz = quizResponse.getQuiz(i);
            System.out.println("\nQuestion " + quiz.getId() + ": " + quiz.getQuestion());
            System.out.println("1. " + quiz.getAnswer1());
            System.out.println("2. " + quiz.getAnswer2());
            System.out.println("3. " + quiz.getAnswer3());
            System.out.println("4. " + quiz.getAnswer4());

            int answer = 0;
            while (answer < 1 || answer > 4) {
                System.out.print("Your answer (1-4): ");
                if (scanner.hasNextInt()) {
                    answer = scanner.nextInt();
                    if (answer < 1 || answer > 4) {
                        System.out.println("Invalid input. Please enter a number between 1 and 4.");
                    }
                } else {
                    System.out.println("Invalid input. Please enter a number between 1 and 4.");
                    scanner.next(); // Clear invalid input
                }
            }

            PlayRequest playRequest = PlayRequest.newBuilder()
                    .setPlayerName(playerName)
                    .setQuizId(quiz.getId())
                    .setAnswer(answer)
                    .build();
            PlayResponse playResponse = stub.play(playRequest);

            if (playResponse.getCorrectAnswer() == answer) {
                System.out.println("Correct!");
            } else {
                System.out.println("Incorrect. The correct answer was: " + playResponse.getCorrectAnswer());
            }
        }
    }
}