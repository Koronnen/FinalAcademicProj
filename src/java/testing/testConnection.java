package testing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class testConnection {

    // MySQL Configuration
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3310/mysql?zeroDateTimeBehavior=CONVERT_TO_NULL";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = "VinzJairus8461`";

    // PostgreSQL Configuration
    private static final String POSTGRES_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String POSTGRES_USER = "postgres";
    private static final String POSTGRES_PASSWORD = "VinzJairus8461`";

    public static void main(String[] args) {
        System.out.println("=== Starting Database Connection Tests ===\n");

        testMySQLConnection();
        System.out.println("\n-----------------------------------------\n");
        testPostgreSQLConnection();

        System.out.println("\n=== Connection Tests Finished ===");
    }

    private static void testMySQLConnection() {
        System.out.println("Attempting to connect to MySQL...");
        try (Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD)) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ SUCCESS: Successfully connected to MySQL!");
                System.out.println("Database Product Name: " + conn.getMetaData().getDatabaseProductName());
                System.out.println("Database Product Version: " + conn.getMetaData().getDatabaseProductVersion());
            }
        } catch (SQLException e) {
            System.err.println("❌ FAILURE: Could not connect to MySQL.");
            System.err.println("Error Message: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
        }
    }

    private static void testPostgreSQLConnection() {
        System.out.println("Attempting to connect to PostgreSQL...");
        try (Connection conn = DriverManager.getConnection(POSTGRES_URL, POSTGRES_USER, POSTGRES_PASSWORD)) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ SUCCESS: Successfully connected to PostgreSQL!");
                System.out.println("Database Product Name: " + conn.getMetaData().getDatabaseProductName());
                System.out.println("Database Product Version: " + conn.getMetaData().getDatabaseProductVersion());
            }
        } catch (SQLException e) {
            System.err.println("❌ FAILURE: Could not connect to PostgreSQL.");
            System.err.println("Error Message: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
        }
    }
}