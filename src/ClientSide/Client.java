package ClientSide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

class Client {
    Client() {
    }

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 12345);
            System.out.println("List of available commands:");
            System.out.println("----------------------------------------------------------");
            System.out.println("|  1. LOGIN <username> <password>");
            System.out.println("|  2. REGISTER <username> <password>");
            System.out.println("|  3. BROWSE");
            System.out.println("|  4. SEARCH <keyword>");
            System.out.println("|  5. ADD <username> <title> <author> <genre> <price> <quantity>");
            System.out.println("|  6. REMOVE <username> <bookId>");
            System.out.println("|  7. GET_MY_BOOKS <username>");
            System.out.println("|  8. REQUEST <borrowerUsername> <lenderUsername> <bookId> <message>");
            System.out.println("|  9. ACCEPT <username> <requestId>");
            System.out.println("|  10. REJECT <requestId>");
            System.out.println("| 11. REQUEST_HISTORY <username>");
            System.out.println("| 12. LIBRARY_STATS");
            System.out.println("| 13. LOGOUT <username>");
            System.out.println("| 14. Get_OnlineUsers");
            System.out.println("----------------------------------------------------------");
            System.out.println("Enter a command:");
            Scanner sc = new Scanner(System.in);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start a separate thread to listen for server messages
            Thread receiveThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println("Server replied: " + message);
                        System.out.println("Enter a command:");
                    }

                } catch (IOException e) {
                    System.out.println("Error receiving message from server: " + e.getMessage());
                }
            });
            receiveThread.start();

            String line = "";
            while (!"EXIT".equalsIgnoreCase(line)) {
                line = sc.nextLine();
                out.println(line);
                out.flush();
            }

            // Close resources
            sc.close();
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
