package totalbroadcast;

import java.io.*;
import java.net.*;

public class ServerListener {
    public static void main(String[] args) throws IOException {
        int port = 24942;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server (Node 6) listening on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Connection established from: " + clientSocket.getInetAddress());

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received (sequenced): " + message);
            }
        }
    }
}

