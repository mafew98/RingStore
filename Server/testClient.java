package Server;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class testClient {
    /**
     * TEST CLIENT FOR SERVER SIDE TESTING. NOT USED!
     * @param args
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Interactive Socket Client. Type 'exit' anytime to quit.");

        while (true) {
            try {
                System.out.print("\nEnter destination IP (or 'exit'): ");
                String ip = scanner.nextLine().trim();
                if (ip.equalsIgnoreCase("exit")) break;

                System.out.print("Enter destination port: ");
                String portStr = scanner.nextLine().trim();
                if (portStr.equalsIgnoreCase("exit")) break;

                int port = Integer.parseInt(portStr);

                System.out.print("Enter message to send (or 'exit'): ");
                String message = scanner.nextLine();
                if (message.equalsIgnoreCase("exit")) break;

                try (Socket socket = new Socket(ip, port);
                     PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    writer.println(message);
                    System.out.println("Message sent to " + ip + ":" + port);

                    // If message starts with "R,", read and print the server's response
                    if (message.startsWith("R,")) {
                        String response = reader.readLine(); // Blocks until server responds or closes
                        if (response != null) {
                            System.out.println("Response: " + response);
                        } else {
                            System.out.println("No response (connection closed).");
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Failed to send/read: " + e.getMessage());
                }

            } catch (Exception e) {
                System.out.println("Invalid input. Try again.");
            }
        }

        System.out.println("Exiting client.");
        scanner.close();
    }
}