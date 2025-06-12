package DB_connection;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlowe

import java.sql.DriverManager;
import java.sql.SQLException;


public class Connection {
    private java.sql.Connection connection;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public Connection(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(this.jdbcUrl, this.username, this.password);
            System.out.println("Database Connected!");
        } catch (SQLException var2) {
            System.err.println("Database Connection Error: " + var2.getMessage());
        } catch (ClassNotFoundException var3) {
            throw new RuntimeException(var3);
        }

    }

    public void disconnect() throws SQLException {
        if (this.connection != null) {
            this.connection.close();
            System.out.println("Database Disconnected!");
        }

    }

    public java.sql.Connection getConnection() {
        return this.connection;
    }
}
