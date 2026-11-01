import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;

public class DatabaseTest {

    public static void main(String[] args) {

        String url = "jdbc:mysql://localhost:3306/knowledge_blackhole";
        String username = "root";
        String password = "asma_27092007";

        String folderPath = "D:\\knowledge black hole detector\\KnowledgeDocuments";

        String sql = "INSERT INTO documents (document_name, content) VALUES (?, ?)";

        try {
            Connection connection =
                    DriverManager.getConnection(url, username, password);

            Path path = Path.of(folderPath);

            DirectoryStream<Path> files =
                    Files.newDirectoryStream(path, "*.txt");

            int count = 0;

            for (Path file : files) {

                String fileName = file.getFileName().toString();
                String content = Files.readString(file);

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                statement.setString(1, fileName);
                statement.setString(2, content);

                statement.executeUpdate();

                statement.close();

                System.out.println(fileName + " inserted successfully!");

                count++;
            }

            files.close();
            connection.close();

            System.out.println("--------------------------------");
            System.out.println("Total documents inserted: " + count);

        } catch (Exception e) {
            System.out.println("Database operation failed!");
            e.printStackTrace();
        }
    }
}