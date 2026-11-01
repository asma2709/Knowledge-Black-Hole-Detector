import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.sql.*;

public class KnowledgeBlackHoleDetectorConsole {

    // ------------------------------------------------
    // SIMILARITY THRESHOLD
    // ------------------------------------------------

    static final double SIMILARITY_THRESHOLD = 20.0;

    // ------------------------------------------------
    // MYSQL DATABASE DETAILS
    // ------------------------------------------------

    static final String DB_URL =
            "jdbc:mysql://localhost:3306/knowledge_blackhole";

    static final String DB_USER = "root";

    static final String DB_PASSWORD = "asma_27092007";

    // ------------------------------------------------
    // STORE DOCUMENTS
    // ------------------------------------------------

    static HashMap<String, String> documents =
            new HashMap<>();

    // ------------------------------------------------
    // STORE DOCUMENT CONNECTIONS
    // ------------------------------------------------

    static HashMap<String, HashSet<String>> graph =
            new HashMap<>();


    // =================================================
    // MAIN METHOD
    // =================================================

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.println("==============================================");
        System.out.println("       KNOWLEDGE BLACK HOLE DETECTOR");
        System.out.println("==============================================");

        System.out.print(
                "\nEnter folder path containing .txt files: "
        );

        String folderPath = sc.nextLine();

        // ------------------------------------------------
        // STEP 1: READ DOCUMENTS
        // ------------------------------------------------

        readDocuments(folderPath);

        if (documents.size() < 2) {

            System.out.println(
                    "\nAt least 2 text files are required."
            );

            sc.close();
            return;
        }

        // ------------------------------------------------
        // DISPLAY DOCUMENTS
        // ------------------------------------------------

        System.out.println(
                "\n----------------------------------------------"
        );

        System.out.println("DOCUMENT SCANNING");

        System.out.println(
                "----------------------------------------------"
        );

        System.out.println(
                "Total documents found: "
                        + documents.size()
        );

        for (String fileName : documents.keySet()) {

            System.out.println("✓ " + fileName);
        }

        // ------------------------------------------------
        // STEP 2: SAVE DOCUMENTS TO MYSQL
        // ------------------------------------------------

        saveDocumentsToDatabase();

        // ------------------------------------------------
        // STEP 3: INITIALIZE GRAPH
        // ------------------------------------------------

        for (String fileName : documents.keySet()) {

            graph.put(
                    fileName,
                    new HashSet<>()
            );
        }

        // ------------------------------------------------
        // STEP 4: CALCULATE SIMILARITY
        // ------------------------------------------------

        calculateSimilarity();

        // ------------------------------------------------
        // STEP 5: DISPLAY CONNECTIONS
        // ------------------------------------------------

        displayConnections();

        // ------------------------------------------------
        // STEP 6: DETECT BLACK HOLES
        // ------------------------------------------------

        ArrayList<String> blackHoles =
                detectBlackHoles();

        // ------------------------------------------------
        // STEP 7: DISPLAY BLACK HOLES
        // ------------------------------------------------

        displayBlackHoles(blackHoles);

        // ------------------------------------------------
        // STEP 8: DISPLAY RECOMMENDATIONS
        // ------------------------------------------------

        displayRecommendations(blackHoles);

        // ------------------------------------------------
        // COMPLETED
        // ------------------------------------------------

        System.out.println(
                "\n=============================================="
        );

        System.out.println(
                "        ANALYSIS COMPLETED"
        );

        System.out.println(
                "=============================================="
        );

        sc.close();
    }


    // =================================================
    // READ ALL TEXT DOCUMENTS
    // =================================================

    static void readDocuments(String folderPath) {

        Path path = Paths.get(folderPath);

        if (!Files.exists(path)
                || !Files.isDirectory(path)) {

            System.out.println(
                    "\nInvalid folder path!"
            );

            return;
        }

        try {

            DirectoryStream<Path> files =
                    Files.newDirectoryStream(
                            path,
                            "*.txt"
                    );

            for (Path file : files) {

                try {

                    String content =
                            Files.readString(file);

                    if (!content.trim().isEmpty()) {

                        documents.put(
                                file.getFileName().toString(),
                                content.toLowerCase()
                        );
                    }

                } catch (IOException e) {

                    System.out.println(
                            "Unable to read: "
                                    + file.getFileName()
                    );
                }
            }

            files.close();

        } catch (IOException e) {

            System.out.println(
                    "Error scanning folder."
            );
        }
    }


    // =================================================
    // SAVE DOCUMENTS TO MYSQL
    // =================================================

    static void saveDocumentsToDatabase() {

        String deleteSQL =
                "DELETE FROM documents";

        String insertSQL =
                "INSERT INTO documents "
                + "(document_name, content) "
                + "VALUES (?, ?)";

        try (
                Connection connection =
                        DriverManager.getConnection(
                                DB_URL,
                                DB_USER,
                                DB_PASSWORD
                        );

                PreparedStatement deleteStatement =
                        connection.prepareStatement(
                                deleteSQL
                        );

                PreparedStatement insertStatement =
                        connection.prepareStatement(
                                insertSQL
                        )
        ) {

            // Remove old records
            deleteStatement.executeUpdate();

            // Insert current documents
            for (String fileName :
                    documents.keySet()) {

                insertStatement.setString(
                        1,
                        fileName
                );

                insertStatement.setString(
                        2,
                        documents.get(fileName)
                );

                insertStatement.executeUpdate();
            }

            System.out.println(
                    "\nDocuments saved to MySQL successfully!"
            );

        } catch (SQLException e) {

            System.out.println(
                    "\nError saving documents to MySQL."
            );

            e.printStackTrace();
        }
    }


    // =================================================
    // CALCULATE DOCUMENT SIMILARITY
    // =================================================

    static void calculateSimilarity() {

        System.out.println(
                "\n----------------------------------------------"
        );

        System.out.println(
                "CALCULATING DOCUMENT SIMILARITY"
        );

        System.out.println(
                "----------------------------------------------"
        );

        ArrayList<String> fileNames =
                new ArrayList<>(
                        documents.keySet()
                );

        boolean connectionFound = false;

        // Clear old similarity results
        clearSimilarityResults();

        for (int i = 0;
             i < fileNames.size();
             i++) {

            for (int j = i + 1;
                 j < fileNames.size();
                 j++) {

                String file1 =
                        fileNames.get(i);

                String file2 =
                        fileNames.get(j);

                double similarity =
                        calculateWordSimilarity(
                                documents.get(file1),
                                documents.get(file2)
                        );

                // Display similarity
                System.out.printf(
                        "%s <-> %s : %.2f%%%n",
                        file1,
                        file2,
                        similarity
                );

                // Save similarity to MySQL
                saveSimilarity(
                        file1,
                        file2,
                        similarity
                );

                // Create graph connection
                if (similarity >=
                        SIMILARITY_THRESHOLD) {

                    graph.get(file1)
                            .add(file2);

                    graph.get(file2)
                            .add(file1);

                    connectionFound = true;
                }
            }
        }

        if (!connectionFound) {

            System.out.println(
                    "\nNo strong document relationships found."
            );
        }
    }


    // =================================================
    // CLEAR OLD SIMILARITY RESULTS
    // =================================================

    static void clearSimilarityResults() {

        String sql =
                "DELETE FROM similarity_results";

        try (
                Connection connection =
                        DriverManager.getConnection(
                                DB_URL,
                                DB_USER,
                                DB_PASSWORD
                        );

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.executeUpdate();

        } catch (SQLException e) {

            System.out.println(
                    "Unable to clear old similarity results."
            );

            e.printStackTrace();
        }
    }


    // =================================================
    // SAVE SIMILARITY TO MYSQL
    // =================================================

    static void saveSimilarity(
            String document1,
            String document2,
            double similarity) {

        String sql =
                "INSERT INTO similarity_results "
                + "(document1, document2, similarity) "
                + "VALUES (?, ?, ?)";

        try (
                Connection connection =
                        DriverManager.getConnection(
                                DB_URL,
                                DB_USER,
                                DB_PASSWORD
                        );

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    document1
            );

            statement.setString(
                    2,
                    document2
            );

            statement.setDouble(
                    3,
                    similarity
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            System.out.println(
                    "Could not save similarity to database."
            );

            e.printStackTrace();
        }
    }


    // =================================================
    // WORD BASED SIMILARITY
    // =================================================

    static double calculateWordSimilarity(
            String text1,
            String text2) {

        Set<String> words1 =
                extractWords(text1);

        Set<String> words2 =
                extractWords(text2);

        if (words1.isEmpty()
                || words2.isEmpty()) {

            return 0.0;
        }

        Set<String> commonWords =
                new HashSet<>(words1);

        commonWords.retainAll(words2);

        Set<String> allWords =
                new HashSet<>(words1);

        allWords.addAll(words2);

        return (
                commonWords.size() * 100.0
        ) / allWords.size();
    }


    // =================================================
    // EXTRACT WORDS
    // =================================================

    static Set<String> extractWords(
            String text) {

        Set<String> words =
                new HashSet<>();

        String[] tokens =
                text.toLowerCase()
                        .replaceAll(
                                "[^a-zA-Z0-9 ]",
                                " "
                        )
                        .split("\\s+");

        for (String word : tokens) {

            if (word.length() > 2) {

                words.add(word);
            }
        }

        return words;
    }


    // =================================================
    // DISPLAY DOCUMENT CONNECTIONS
    // =================================================

    static void displayConnections() {

        System.out.println(
                "\n----------------------------------------------"
        );

        System.out.println(
                "KNOWLEDGE GRAPH CONNECTIONS"
        );

        System.out.println(
                "----------------------------------------------"
        );

        boolean found = false;

        for (String document :
                graph.keySet()) {

            for (String connected :
                    graph.get(document)) {

                // Avoid duplicate connections
                if (document.compareTo(
                        connected
                ) < 0) {

                    System.out.println(
                            document
                                    + " <----> "
                                    + connected
                    );

                    found = true;
                }
            }
        }

        if (!found) {

            System.out.println(
                    "No connected documents."
            );
        }
    }


    // =================================================
    // DETECT KNOWLEDGE BLACK HOLES
    // =================================================

    static ArrayList<String>
    detectBlackHoles() {

        ArrayList<String> blackHoles =
                new ArrayList<>();

        for (String document :
                graph.keySet()) {

            // No connections
            // = Knowledge Black Hole

            if (graph.get(document)
                    .isEmpty()) {

                blackHoles.add(document);
            }
        }

        return blackHoles;
    }


    // =================================================
    // DISPLAY BLACK HOLES
    // =================================================

    static void displayBlackHoles(
            ArrayList<String> blackHoles) {

        System.out.println(
                "\n----------------------------------------------"
        );

        System.out.println(
                "KNOWLEDGE BLACK HOLE DETECTION"
        );

        System.out.println(
                "----------------------------------------------"
        );

        if (blackHoles.isEmpty()) {

            System.out.println(
                    "✓ No isolated documents found."
            );

            System.out.println(
                    "All documents have at least one connection."
            );

        } else {

            System.out.println(
                    "⚠ Knowledge Black Holes Detected!"
            );

            for (String document :
                    blackHoles) {

                System.out.println(
                        "⚠ " + document
                );
            }
        }
    }


    // =================================================
    // RECOMMEND RELATED DOCUMENTS
    // =================================================

    static void displayRecommendations(
            ArrayList<String> blackHoles) {

        System.out.println(
                "\n----------------------------------------------"
        );

        System.out.println(
                "RECOMMENDED RELATED DOCUMENTS"
        );

        System.out.println(
                "----------------------------------------------"
        );

        if (blackHoles.isEmpty()) {

            System.out.println(
                    "No recommendations required."
            );

            return;
        }

        for (String blackHole :
                blackHoles) {

            System.out.println(
                    "\nFor: " + blackHole
            );

            String bestDocument = null;

            double bestSimilarity = 0;

            for (String other :
                    documents.keySet()) {

                if (other.equals(
                        blackHole
                )) {

                    continue;
                }

                double similarity =
                        calculateWordSimilarity(
                                documents.get(
                                        blackHole
                                ),
                                documents.get(
                                        other
                                )
                        );

                if (similarity >
                        bestSimilarity) {

                    bestSimilarity =
                            similarity;

                    bestDocument =
                            other;
                }
            }

            if (bestDocument != null) {

                System.out.printf(
                        "→ Recommended: %s "
                                + "(Similarity: %.2f%%)%n",
                        bestDocument,
                        bestSimilarity
                );

            } else {

                System.out.println(
                        "→ No related document available."
                );
            }
        }
    }
}