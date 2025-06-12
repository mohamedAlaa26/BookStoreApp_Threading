package ServerSide;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import DB_connection.Connection;

public class Server {
    private static final int PORT = 12345;
    private static Connection dbConnection;
    private static Map<String, User> loggedInUsers = new HashMap<>();
//    private static Map<String, ClientHandler> loggedInUsers = new HashMap<>();

    public static void main(String[] args) {
        dbConnection = new Connection("jdbc:mysql://localhost:3306/mysql", "root", "12345678");
        dbConnection.connect();
        //jdbc:mysql://localhost:3306/mysql

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started. Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket);

                ClientHandler clientHandler = new ClientHandler(clientSocket);// the threads part
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                dbConnection.disconnect();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    //    public static Map<String, User> getLoggedInUsers() {
//
//        return loggedInUsers;
//    }
    public static List<String> getOnlineUsers()
    {
        List<String> onlineUsers = new ArrayList<>();
        for (Map.Entry<String, User> entry : loggedInUsers.entrySet()) {
            onlineUsers.add(entry.getKey());
        }
        return onlineUsers;
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private static Set<String> loggedInUsernames = new HashSet<>();

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
//                sendCommandList();// menue for the client commands
                String input;
                while ((input = in.readLine()) != null) {
                    String[] tokens = input.split(" ");
                    String command = tokens[0];
                    switch (command) {
                        case "LOGIN":
                            handleLogin(tokens);
                            break;
                        case "REGISTER":
                            handleRegistration(tokens);
                            break;
                        case "BROWSE":
                            handleBrowse();
                            break;
                        case "SEARCH":
                            //    searchBooks(tokens[1]);
                            break;
                        case "ADD":
                            addBook(tokens);
                            break;
                        case "REMOVE":
                            removeBook(tokens);
                            break;
                        case "GET_MY_BOOKS":
                            GetMyBooks(tokens[1]);
                        case "REQUEST":
                            submitRequest(tokens);
                            break;
                        case "ACCEPT":
                            acceptRequest(tokens[1],tokens[2]);
                            break;
                        case "REJECT":
                            rejectRequest(tokens[1],tokens[2]);
                            break;
                        case "REQUEST_HISTORY":
                            getRequestHistory(tokens[1]);
                            break;
                        case "LIBRARY_STATS":
                            //    sendLibraryStats();
                            break;
                        case "LOGOUT":
                            logout(tokens[1]);
                            break;
                        case "Get_OnlineUsers":
                            out.println(getOnlineUsers());
                            break;
                        default:
                            out.println("Invalid command");
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        private void sendCommandList() {
//            out.println("List of available commands:");
//            out.println("LOGIN <username> <password>");
//            out.println("REGISTER <username> <password>");
//            out.println("BROWSE");
//            out.println("SEARCH <keyword>");
//            out.println("ADD <title> <author> <genre> <price> <quantity>");
//            out.println("REMOVE <bookId>");
//            out.println("REQUEST <borrowerUsername> <lenderUsername> <bookId>");
//            out.println("ACCEPT <requestId>");
//            out.println("REJECT <requestId>");
//            out.println("REQUEST_HISTORY <username>");
//            out.println("LIBRARY_STATS");
//            out.println("LOGOUT <username>");
//            out.println("Get_OnlineUsers");
//            out.println("Enter a command:");
//            out.flush();
//        }

        private void handleLogin(String[] tokens) {
            String username = tokens[1];
            String password = tokens[2];

            try {
                if (loggedInUsers.containsKey(username)) {
                    out.println("USER_ALREADY_LOGGED_IN");
                    return;
                }
                PreparedStatement statement = dbConnection.getConnection().prepareStatement("SELECT * FROM mysql.users WHERE username = ?");
                statement.setString(1, username);
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    // Username exists, check password
                    String dbPassword = resultSet.getString("password");
                    if (password.equals(dbPassword)) {
                        // Correct password
                        User user = new User(username, password);
                        loggedInUsers.put(username, user);
                        out.println("LOGIN_SUCCESS");
                    } else {
                        // Wrong password
                        out.println("401 Wrong password");
                    }
                } else {
                    // Username not found
                    out.println("404 Username not found");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void handleRegistration(String[] tokens) {
            String username = tokens[1];
            String password = tokens[2];

            try {

                // Check if username already exists
                PreparedStatement checkStatement = dbConnection.getConnection().prepareStatement("SELECT * FROM mysql.users WHERE username = ?");
                checkStatement.setString(1, username);
                ResultSet resultSet =checkStatement.executeQuery();

                if (resultSet.next()) {
                    out.println("409 Username already exists");
                    return;
                }

                PreparedStatement insertStatement = dbConnection.getConnection().prepareStatement("INSERT INTO mysql.users (username, password) VALUES (?, ?)");
                insertStatement.setString(1, username);
                insertStatement.setString(2, password);
                int rowsInserted = insertStatement.executeUpdate();

                if (rowsInserted > 0) {
                    out.println("REGISTER_SUCCESS");
                } else {
                    out.println("500 Internal Server Error");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                out.println("500 Internal Server Error");
            }
        }


        private void addBook(String[] tokens)
        {
            String username = tokens[1];
            if (!loggedInUsers.containsKey(username)) {// tokens[1] is the username of the client
                out.println("403 Forbidden: You must be logged in to add a book");
                return;
            }

            String author = tokens[2];
            String genre = tokens[3];
            double price = Double.parseDouble(tokens[4]) ;
            int quantity = Integer.parseInt(tokens[5]);
            String title = tokens[6];

            try {
                PreparedStatement statementUserId = dbConnection.getConnection().prepareStatement("SELECT id FROM mysql.users WHERE username = ?");
                statementUserId.setString(1, username);
                ResultSet resultSetUserID = statementUserId.executeQuery();
                resultSetUserID.next();
                int userId = resultSetUserID.getInt("id");

                PreparedStatement statement = dbConnection.getConnection().prepareStatement("INSERT INTO mysql.books (author, genre, price , quantity , title , user_id) VALUES (?, ?, ?, ?, ?, ?)");
                statement.setString(1, author);
                statement.setString(2, genre);
                statement.setDouble(3,price);
                statement.setInt(4,quantity);
                statement.setString(5,title );
                statement.setInt(6, userId);

                int rowsInserted = statement.executeUpdate();

                if (rowsInserted > 0) {
                    out.println("BOOK_ADDED");
                } else {
                    out.println("BOOK_ADD_FAILED");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }


        private void removeBook(String[] tokens) {
            if (!loggedInUsers.containsKey(tokens[1])) {// tokens[1] is the username of the client
                out.println("403 Forbidden: You must be logged in to remove a book");
                return;
            }
            try {
                PreparedStatement statement = dbConnection.getConnection().prepareStatement("DELETE FROM books WHERE id = ?");
                statement.setInt(1, Integer.parseInt(tokens[2]));

                int rowsDeleted = statement.executeUpdate();

                if (rowsDeleted > 0) {
                    out.println("BOOK_REMOVED");
                } else {
                    out.println("BOOK_REMOVE_FAILED");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void GetMyBooks(String username) {
            if (!loggedInUsers.containsKey(username)) {
                out.println("403 Forbidden: You must be logged in to get your books");
                return;
            }

            try {
                // Get the user ID of the logged-in user
                PreparedStatement getUserIdStatement = dbConnection.getConnection().prepareStatement("SELECT id FROM mysql.users WHERE username = ?");
                getUserIdStatement.setString(1, username);
                ResultSet userIdResultSet = getUserIdStatement.executeQuery();

                if (userIdResultSet.next()) {
                    int userId = userIdResultSet.getInt("id");

                    // Query the books table for books added by the user
                    PreparedStatement getBooksStatement = dbConnection.getConnection().prepareStatement("SELECT * FROM mysql.books WHERE user_id = ?");
                    getBooksStatement.setInt(1, userId);
                    ResultSet resultSet = getBooksStatement.executeQuery();

                    List<String> userBooks = new ArrayList<>();
                    while (resultSet.next()) {
                        String author = resultSet.getString("author");
                        String genre = resultSet.getString("genre");
                        double price = resultSet.getDouble("price");
                        int quantity = resultSet.getInt("quantity");
                        String title = resultSet.getString("title");

                        String bookInfo = String.format("Author: %s, Genre: %s, Price: %.2f, Quantity: %d, Title: %s",
                                author, genre, price, quantity, title);
                        userBooks.add(bookInfo);
                    }

                    if (!userBooks.isEmpty()) {
                        out.println("MY_BOOKS");
                        for (String bookInfo : userBooks) {
                            out.println(bookInfo);
                        }
                    } else {
                        out.println("You have not added any books yet.");
                    }
                } else {
                    out.println("404 User not found");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }


        private void handleBrowse() {
            StringBuilder response = new StringBuilder();
            try {
                PreparedStatement statement = dbConnection.getConnection().prepareStatement("SELECT * FROM mysql.books");
                ResultSet resultSet = statement.executeQuery();

                List<String> books = new ArrayList<>();
                while (resultSet.next()) {
                    int bookId = resultSet.getInt("id");
                    String title = resultSet.getString("title");
                    String author = resultSet.getString("author");
                    String genre = resultSet.getString("genre");
                    double price = resultSet.getDouble("price");
                    int quantity = resultSet.getInt("quantity");
                    String userid = resultSet.getString("user_id");
                    String bookInfo = String.format("BookID: %s,Title: %s, Author: %s, Genre: %s, Price: %.2f, Quantity: %d, User_id: %s",
                            bookId, title, author, genre, price, quantity, userid);
                    books.add(bookInfo);
                }

                if (!books.isEmpty()) {
                    response.append("BROWSE_SUCCESS\n");
                    for (String book : books) {
                        response.append(book).append("\n");
                    }
                    out.println(response);

                } else {
                    out.println("No books available");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }


        private void submitRequest(String[] tokens) {
            String borrowerUsername = tokens[1];
            String lenderUsername = tokens[2];
            int bookId = Integer.parseInt(tokens[3]);
            String message = tokens[4];  // Optional message from borrower

            try {
                // Get borrower and lender IDs
                int borrowerId = getUserIdByUsername(borrowerUsername);
                int lenderId = getUserIdByUsername(lenderUsername);

                PreparedStatement insertStatement = dbConnection.getConnection().prepareStatement("INSERT INTO requests (borrower_id, lender_id, book_id, status ,message) VALUES (?, ?, ? , ?, ?)");
                insertStatement.setInt(1, borrowerId);
                insertStatement.setInt(2, lenderId);
                insertStatement.setInt(3, bookId);
                insertStatement.setString(4, "Pending");
                insertStatement.setString(5, message);

                int rowsInserted = insertStatement.executeUpdate();

                if (rowsInserted > 0) {
                    out.println("REQUEST_SUBMITTED");
                } else {
                    out.println("REQUEST_SUBMIT_FAILED");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                out.println("REQUEST_SUBMIT_FAILED");
            }
        }
        private void getRequestHistory(String username) {
            try {
                int userId = getUserIdByUsername(username);

                PreparedStatement getHistoryStatement = dbConnection.getConnection().prepareStatement("SELECT * FROM requests WHERE borrower_id = ? OR lender_id = ?");
                getHistoryStatement.setInt(1, userId);
                getHistoryStatement.setInt(2, userId);

                ResultSet resultSet = getHistoryStatement.executeQuery();
                List<String> requestHistory = new ArrayList<>();

                while (resultSet.next()) {
                    int requestId = resultSet.getInt("id");
                    String borrower = getUsernameById(resultSet.getInt("borrower_id"));
                    String lender = getUsernameById(resultSet.getInt("lender_id"));
                    String bookTitle = getBookTitleById(resultSet.getInt("book_id"));
                    String status = resultSet.getString("status");
                    String message = resultSet.getString("message");

                    String requestInfo = String.format("Request ID: %d, Borrower: %s, Lender: %s, Book Title: %s, Status: %s , Message: %s",
                            requestId, borrower, lender, bookTitle, status,message);
                    requestHistory.add(requestInfo);
                }

                if (!requestHistory.isEmpty()) {
                    out.println("REQUEST_HISTORY");
                    for (String info : requestHistory) {
                        out.println(info);
                    }
                } else {
                    out.println("No request history found.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                out.println("REQUEST_HISTORY_FAILED");
            }
        }

        private void acceptRequest(String lenderUsername, String requestId) {
            try {
                if (!loggedInUsers.containsKey(lenderUsername)) {
                    out.println("403 Forbidden: You must be logged in as the lender to accept the request.");
                    return;
                }

                User lender = loggedInUsers.get(lenderUsername);
                if (!lender.getRole().equalsIgnoreCase("lender")) {
                    out.println("403 Forbidden: Only lenders can accept requests.");
                    return;
                }

                PreparedStatement updateStatement = dbConnection.getConnection().prepareStatement("UPDATE requests SET status = ? WHERE id = ?");
                updateStatement.setString(1, "Accepted");
                updateStatement.setInt(2, Integer.parseInt(requestId));

                int rowsUpdated = updateStatement.executeUpdate();

                if (rowsUpdated > 0) {
                    out.println("REQUEST_ACCEPTED");
                } else {
                    out.println("REQUEST_ACCEPT_FAILED");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                out.println("REQUEST_ACCEPT_FAILED");
            }
        }

        private void rejectRequest(String lenderUsername, String requestId) {
            try {
                if (!loggedInUsers.containsKey(lenderUsername)) {
                    out.println("403 Forbidden: You must be logged in as the lender to reject the request.");
                    return;
                }

                User lender = loggedInUsers.get(lenderUsername);
                if (!lender.getRole().equalsIgnoreCase("lender")) {
                    out.println("403 Forbidden: Only lenders can reject requests.");
                    return;
                }

                PreparedStatement updateStatement = dbConnection.getConnection().prepareStatement("UPDATE requests SET status = ? WHERE id = ?");
                updateStatement.setString(1, "Rejected");
                updateStatement.setInt(2, Integer.parseInt(requestId));

                int rowsUpdated = updateStatement.executeUpdate();

                if (rowsUpdated > 0) {
                    out.println("REQUEST_REJECTED");
                } else {
                    out.println("REQUEST_REJECT_FAILED");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                out.println("REQUEST_REJECT_FAILED");
            }
        }


        private int getUserIdByUsername(String username) throws SQLException {
            PreparedStatement getUserIdStatement = dbConnection.getConnection().prepareStatement("SELECT id FROM users WHERE username = ?");
            getUserIdStatement.setString(1, username);
            ResultSet resultSet = getUserIdStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("id");
            }
            return -1;  // User not found
        }

        private String getUsernameById(int userId) throws SQLException {
            PreparedStatement getUsernameStatement = dbConnection.getConnection().prepareStatement("SELECT username FROM users WHERE id = ?");
            getUsernameStatement.setInt(1, userId);
            ResultSet resultSet = getUsernameStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("username");
            }
            return null;  // Username not found
        }

        private String getBookTitleById(int bookId) throws SQLException {
            PreparedStatement getBookTitleStatement = dbConnection.getConnection().prepareStatement("SELECT title FROM books WHERE id = ?");
            getBookTitleStatement.setInt(1, bookId);
            ResultSet resultSet = getBookTitleStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("title");
            }
            return null;  // Book title not found
        }

        private void logout(String username) {
            loggedInUsers.remove(username);
            out.println("LOGOUT_SUCCESS");
        }
    }
}
