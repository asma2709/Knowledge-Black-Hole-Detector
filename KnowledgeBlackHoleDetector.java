import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class KnowledgeBlackHoleDetector extends Application {

    // =========================
    // DATABASE SETTINGS
    // =========================
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/knowledge_blackhole";

    private static final String DB_USER = "root";

    // IMPORTANT:
    // இங்கே உங்க OLD working code-ல் இருந்த அதே password-ஐ மட்டும் போடுங்க.
    private static final String DB_PASSWORD = "asma_27092007";

    private static final String DOCUMENT_FOLDER = "KnowledgeDocuments";

    private static final double SIMILARITY_THRESHOLD = 20.0;

    // =========================
    // GUI COMPONENTS
    // =========================
    private Label documentCountLabel;
    private Label blackHoleLabel;
    private Label recommendationLabel;
    private TextArea resultArea;

    // =========================
    // START GUI
    // =========================
    @Override
    public void start(Stage stage) {

        Label title = new Label("Knowledge Black Hole Detector");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitle = new Label(
                "JDBC + JavaFX Knowledge Analysis System"
        );

        Button analyzeButton = new Button("Run Analysis");
        analyzeButton.setStyle(
                "-fx-font-size: 16px; -fx-padding: 10 20 10 20;"
        );

        documentCountLabel = new Label("Documents analyzed: -");
        blackHoleLabel = new Label("Knowledge Black Hole: -");
        recommendationLabel = new Label("Recommendation: -");

        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setPrefHeight(400);

        analyzeButton.setOnAction(event -> runAnalysis());

        VBox root = new VBox(
                12,
                title,
                subtitle,
                analyzeButton,
                documentCountLabel,
                blackHoleLabel,
                recommendationLabel,
                new Label("Similarity Results:"),
                resultArea
        );

        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 750, 650);

        stage.setTitle("Knowledge Black Hole Detector");
        stage.setScene(scene);
        stage.show();
    }

    // =========================
    // RUN ANALYSIS
    // =========================
    private void runAnalysis() {

        try {

            List<Document> documents = loadDocuments();

            if (documents.isEmpty()) {
                showError("No .txt documents found!");
                return;
            }

            saveDocumentsToDatabase(documents);

            List<SimilarityResult> similarities =
                    calculateSimilarities(documents);

            saveSimilarityResultsToDatabase(similarities);

            Map<String, Set<String>> graph =
                    buildGraph(documents, similarities);

            List<String> blackHoles =
                    findBlackHoles(graph);

            String recommendation = "";

            if (!blackHoles.isEmpty()) {

                String blackHole = blackHoles.get(0);

                SimilarityResult best =
                        findBestRecommendation(
                                blackHole,
                                similarities
                        );

                if (best != null) {
                    recommendation =
                            best.otherDocument(blackHole)
                            + " (" +
                            String.format("%.2f", best.similarity)
                            + "%)";
                }

                blackHoleLabel.setText(
                        "Knowledge Black Hole: " + blackHole
                );

            } else {

                blackHoleLabel.setText(
                        "Knowledge Black Hole: None"
                );
            }

            recommendationLabel.setText(
                    "Recommendation: " +
                    (recommendation.isEmpty()
                            ? "None"
                            : recommendation)
            );

            documentCountLabel.setText(
                    "Documents analyzed: " + documents.size()
            );

            displaySimilarityResults(similarities);

        } catch (Exception e) {

            showError(
                    "Error: " + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    // =========================
    // LOAD DOCUMENTS
    // =========================
    private List<Document> loadDocuments()
            throws IOException {

        List<Document> documents = new ArrayList<>();

        Path folder =
                Paths.get(DOCUMENT_FOLDER);

        if (!Files.exists(folder)) {
            return documents;
        }

        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(folder, "*.txt")) {

            for (Path file : stream) {

                String content =
                        Files.readString(file);

                documents.add(
                        new Document(
                                file.getFileName().toString(),
                                content
                        )
                );
            }
        }

        return documents;
    }

    // =========================
    // SAVE DOCUMENTS TO MYSQL
    // =========================
    private void saveDocumentsToDatabase(
            List<Document> documents)
            throws SQLException {

        String deleteSQL =
                "DELETE FROM documents";

        String insertSQL =
                "INSERT INTO documents " +
                "(document_name, content) VALUES (?, ?)";

        try (Connection connection =
                     DriverManager.getConnection(
                             DB_URL,
                             DB_USER,
                             DB_PASSWORD);

             Statement statement =
                     connection.createStatement()) {

            statement.executeUpdate(deleteSQL);

            try (PreparedStatement ps =
                         connection.prepareStatement(insertSQL)) {

                for (Document doc : documents) {

                    ps.setString(
                            1,
                            doc.name
                    );

                    ps.setString(
                            2,
                            doc.content
                    );

                    ps.executeUpdate();
                }
            }
        }
    }

    // =========================
    // CALCULATE SIMILARITY
    // =========================
    private List<SimilarityResult>
    calculateSimilarities(
            List<Document> documents) {

        List<SimilarityResult> results =
                new ArrayList<>();

        for (int i = 0;
             i < documents.size();
             i++) {

            for (int j = i + 1;
                 j < documents.size();
                 j++) {

                Document d1 =
                        documents.get(i);

                Document d2 =
                        documents.get(j);

                double similarity =
                        calculateSimilarity(
                                d1.content,
                                d2.content
                        );

                results.add(
                        new SimilarityResult(
                                d1.name,
                                d2.name,
                                similarity
                        )
                );
            }
        }

        return results;
    }

    // =========================
    // WORD SET SIMILARITY
    // =========================
    private double calculateSimilarity(
            String text1,
            String text2) {

        Set<String> words1 =
                extractWords(text1);

        Set<String> words2 =
                extractWords(text2);

        Set<String> intersection =
                new HashSet<>(words1);

        intersection.retainAll(words2);

        Set<String> union =
                new HashSet<>(words1);

        union.addAll(words2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return
                ((double) intersection.size()
                        / union.size()) * 100.0;
    }

    // =========================
    // EXTRACT WORDS
    // =========================
    private Set<String> extractWords(
            String text) {

        Set<String> words =
                new HashSet<>();

        String[] tokens =
                text.toLowerCase()
                        .split("[^a-zA-Z]+");

        for (String word : tokens) {

            if (word.length() > 2) {
                words.add(word);
            }
        }

        return words;
    }

    // =========================
    // SAVE SIMILARITIES
    // =========================
    private void saveSimilarityResultsToDatabase(
            List<SimilarityResult> results)
            throws SQLException {

        String deleteSQL =
                "DELETE FROM similarity_results";

        String insertSQL =
                "INSERT INTO similarity_results " +
                "(document1, document2, similarity) " +
                "VALUES (?, ?, ?)";

        try (Connection connection =
                     DriverManager.getConnection(
                             DB_URL,
                             DB_USER,
                             DB_PASSWORD);

             Statement statement =
                     connection.createStatement()) {

            statement.executeUpdate(deleteSQL);

            try (PreparedStatement ps =
                         connection.prepareStatement(insertSQL)) {

                for (SimilarityResult result : results) {

                    ps.setString(
                            1,
                            result.document1
                    );

                    ps.setString(
                            2,
                            result.document2
                    );

                    ps.setDouble(
                            3,
                            result.similarity
                    );

                    ps.executeUpdate();
                }
            }
        }
    }

    // =========================
    // BUILD GRAPH
    // =========================
    private Map<String, Set<String>>
    buildGraph(
            List<Document> documents,
            List<SimilarityResult> results) {

        Map<String, Set<String>> graph =
                new HashMap<>();

        for (Document doc : documents) {
            graph.put(
                    doc.name,
                    new HashSet<>()
            );
        }

        for (SimilarityResult result : results) {

            if (result.similarity >=
                    SIMILARITY_THRESHOLD) {

                graph.get(result.document1)
                        .add(result.document2);

                graph.get(result.document2)
                        .add(result.document1);
            }
        }

        return graph;
    }

    // =========================
    // FIND BLACK HOLES
    // =========================
    private List<String> findBlackHoles(
            Map<String, Set<String>> graph) {

        List<String> blackHoles =
                new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry
                : graph.entrySet()) {

            if (entry.getValue().isEmpty()) {
                blackHoles.add(entry.getKey());
            }
        }

        return blackHoles;
    }

    // =========================
    // BEST RECOMMENDATION
    // =========================
    private SimilarityResult findBestRecommendation(
            String blackHole,
            List<SimilarityResult> results) {

        SimilarityResult best = null;

        for (SimilarityResult result : results) {

            if (result.document1.equals(blackHole)
                    || result.document2.equals(blackHole)) {

                if (best == null ||
                        result.similarity >
                                best.similarity) {

                    best = result;
                }
            }
        }

        return best;
    }

    // =========================
    // DISPLAY RESULTS
    // =========================
    private void displaySimilarityResults(
            List<SimilarityResult> results) {

        StringBuilder output =
                new StringBuilder();

        for (SimilarityResult result : results) {

            output.append(
                    result.document1
            );

            output.append(" <-> ");

            output.append(
                    result.document2
            );

            output.append(" : ");

            output.append(
                    String.format(
                            "%.2f",
                            result.similarity
                    )
            );

            output.append("%");

            if (result.similarity >=
                    SIMILARITY_THRESHOLD) {

                output.append(
                        "  [CONNECTED]"
                );
            }

            output.append("\n");
        }

        resultArea.setText(
                output.toString()
        );
    }

    // =========================
    // ERROR MESSAGE
    // =========================
    private void showError(String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // =========================
    // MAIN
    // =========================
    public static void main(String[] args) {
        launch(args);
    }

    // =========================
    // DOCUMENT CLASS
    // =========================
    static class Document {

        String name;
        String content;

        Document(
                String name,
                String content) {

            this.name = name;
            this.content = content;
        }
    }

    // =========================
    // SIMILARITY CLASS
    // =========================
    static class SimilarityResult {

        String document1;
        String document2;
        double similarity;

        SimilarityResult(
                String document1,
                String document2,
                double similarity) {

            this.document1 = document1;
            this.document2 = document2;
            this.similarity = similarity;
        }

        String otherDocument(
                String document) {

            if (document1.equals(document)) {
                return document2;
            }

            return document1;
        }
    }
}