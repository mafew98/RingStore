package Client;

import java.io.*;
import java.net.*;

public class ServerListener {
    public static void main(String[] args) throws IOException {
        int port = 24942;
        // Get current server IP address
        InetAddress localHost = InetAddress.getLocalHost();
        String serverIP = localHost.getHostAddress();

        // Hardcode server IDs for simplicity
        int serverId;
        if (serverIP.equals("10.176.69.38")) {
            serverId = 1;
        } else if (serverIP.equals("10.176.69.39")) {
            serverId = 2;
        } else {
            serverId = 0; // Unknown
        }

        System.out.println("Server " + serverId + " (" + serverIP + ") listening on port " + port);

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server (Node 6) listening on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Connection established from: " + clientSocket.getInetAddress());

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Server " + serverId + " received (sequenced): " + message);
            }
            clientSocket.close();
        }
    }
}

